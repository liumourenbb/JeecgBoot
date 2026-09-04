package org.jeecg.flyway.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flyway 数据库版本管理多数据源配置
 * <p>
 * 支持业务主库 (master) 与 Nacos (nacos) 等多数据源并行迁移升级。
 * 执行迁移前会自动探测目标数据库是否存在，若不存在则自动执行 CREATE DATABASE。
 * </p>
 *
 * @author jeecg
 */
@Slf4j
@Lazy(false)
@Configuration
@ConfigurationProperties(prefix = "spring.flyway")
@Data
public class FlywayConfig {

    /** 全局是否开启 flyway */
    private Boolean enabled = true;

    /** 全局编码格式，默认 UTF-8 */
    private String encoding = "UTF-8";

    /** 全局迁移 sql 脚本前缀，默认 V */
    private String sqlMigrationPrefix = "V";

    /** 全局迁移 sql 脚本分隔符，默认双下划线 __ */
    private String sqlMigrationSeparator = "__";

    /** 占位符前缀 */
    private String placeholderPrefix = "#(";

    /** 占位符后缀 */
    private String placeholderSuffix = ")";

    /** 全局迁移 sql 脚本后缀 */
    private String sqlMigrationSuffixes = ".sql";

    /** 迁移时是否校验，默认 true */
    private Boolean validateOnMigrate = true;

    /** 数据库非空时，自动执行基准迁移，默认 true */
    private Boolean baselineOnMigrate = true;

    /** 是否禁用 clean 功能（生产环境必须为 true，否则会清空数据库！） */
    private Boolean cleanDisabled = true;

    /**
     * 多数据源配置映射（key 为数据源别名，如 master、nacos）
     */
    private Map<String, FlywayDataSourceProperties> datasources = new LinkedHashMap<>();

    @Data
    public static class FlywayDataSourceProperties {
        /** 是否启用该数据源迁移 */
        private Boolean enabled = true;
        /** 数据库连接 URL */
        private String url;
        /** 用户名 */
        private String username;
        /** 密码 */
        private String password;
        /** 驱动类名 */
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        /** 迁移 sql 脚本路径 */
        private String locations;
        /** 是否自动建库（若库不存在则自动执行 CREATE DATABASE） */
        private Boolean autoCreateDatabase = true;
    }

    @PostConstruct
    public void migrate() {
        if (!Boolean.TRUE.equals(enabled)) {
            log.info("【数据库升级】Flyway 全局未启用，跳过所有数据库迁移（spring.flyway.enabled=false）");
            return;
        }

        if (datasources == null || datasources.isEmpty()) {
            log.warn("【数据库升级】未配置任何数据源（spring.flyway.datasources 为空），跳过迁移。");
            return;
        }

        log.info("【数据库升级】开始执行多数据源 Flyway 数据库迁移，共检测到 {} 个数据源配置...", datasources.size());

        datasources.forEach((dsName, dsProps) -> {
            if (dsProps == null || !Boolean.TRUE.equals(dsProps.getEnabled())) {
                log.info("【数据库升级】[{}] 数据源已配置禁用或为空，跳过迁移。", dsName);
                return;
            }

            String jdbcUrl = dsProps.getUrl();
            if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
                log.warn("【数据库升级】[{}] 数据源 URL 为空，跳过迁移。", dsName);
                return;
            }

            if (!jdbcUrl.contains("mysql")) {
                log.warn("【数据库升级】[{}] 当前仅支持 MySQL 自动升级，其他数据库请手工执行脚本。url={}", dsName, jdbcUrl);
                return;
            }

            // 自动建库支持（如果目标库尚不存在，先连 root 库创建）
            if (Boolean.TRUE.equals(dsProps.getAutoCreateDatabase())) {
                createDatabaseIfNotExist(dsName, dsProps);
            }

            try {
                log.info("【数据库升级】[{}] 开始执行 Flyway 迁移 -> URL: {}, 脚本路径: {}",
                        dsName, jdbcUrl, dsProps.getLocations());

                Flyway flyway = Flyway.configure()
                        .dataSource(jdbcUrl, dsProps.getUsername(), dsProps.getPassword())
                        .locations(dsProps.getLocations())
                        .encoding(encoding)
                        .sqlMigrationPrefix(sqlMigrationPrefix)
                        .sqlMigrationSeparator(sqlMigrationSeparator)
                        .placeholderPrefix(placeholderPrefix)
                        .placeholderSuffix(placeholderSuffix)
                        .sqlMigrationSuffixes(sqlMigrationSuffixes)
                        .validateOnMigrate(validateOnMigrate)
                        .baselineOnMigrate(baselineOnMigrate)
                        .cleanDisabled(cleanDisabled)
                        .load();

                MigrateResult result = flyway.migrate();
                log.info("【数据库升级】[{}] Flyway 迁移执行成功！本次成功应用脚本数: {}, 当前目标版本: {}",
                        dsName, result.migrationsExecuted, result.targetSchemaVersion);
            } catch (FlywayException e) {
                log.error("【数据库升级】[{}] Flyway 迁移执行失败！", dsName, e);
                throw e;
            } catch (Exception e) {
                log.error("【数据库升级】[{}] 数据源连接或迁移处理出现异常！", dsName, e);
                throw new RuntimeException("Flyway 迁移失败: " + dsName, e);
            }
        });

        log.info("【数据库升级】所有已配置的数据源 Flyway 自动升级流程全部执行完毕！");
    }

    /**
     * 解析 JDBC URL 中的库名，若库不存在则自动创建
     */
    private void createDatabaseIfNotExist(String dsName, FlywayDataSourceProperties props) {
        String url = props.getUrl();
        try {
            // 典型匹配: jdbc:mysql://host:port/dbname?params 或 jdbc:mysql://host:port/dbname
            Pattern pattern = Pattern.compile("jdbc:mysql://([^/?]+)/([^/?]+)(\\?.*)?");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String hostPort = matcher.group(1);
                String dbName = matcher.group(2);
                String params = matcher.group(3) != null ? matcher.group(3) : "";

                // 不指定特定库名连接 MySQL 服务
                String rootUrl = "jdbc:mysql://" + hostPort + "/" + params;
                try (Connection conn = DriverManager.getConnection(rootUrl, props.getUsername(), props.getPassword());
                     Statement stmt = conn.createStatement()) {
                    String createDbSql = "CREATE DATABASE IF NOT EXISTS `" + dbName + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;";
                    stmt.execute(createDbSql);
                    log.info("【数据库升级】[{}] 自动校验/创建数据库 `{}` 成功。", dsName, dbName);
                } catch (Exception e) {
                    log.warn("【数据库升级】[{}] 尝试自动创建数据库 `{}` 未成功（若数据库已存在可忽略）: {}", dsName, dbName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("【数据库升级】[{}] 解析数据库名时出现异常，跳过自动建库: {}", dsName, e.getMessage());
        }
    }
}
