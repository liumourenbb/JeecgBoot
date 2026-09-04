package org.jeecg.flyway.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;

/**
 * Flyway 数据库版本管理配置
 * <p>
 * 独立启动模式：通过 spring.datasource.url 判断数据库类型，
 * 当前仅支持 MySQL，其他数据库请手工执行升级脚本。
 * </p>
 *
 * @author wangshuai
 * @date 2024/3/12
 */
@Slf4j
@Lazy(false)
@Configuration
public class FlywayConfig {

    @Autowired
    private DataSource dataSource;

    /** 是否开启 flyway */
    @Value("${spring.flyway.enabled:false}")
    private Boolean enabled;

    /** 数据库连接 URL，用于判断数据库类型 */
    @Value("${spring.datasource.url:}")
    private String dataSourceUrl;

    /** 编码格式，默认 UTF-8 */
    @Value("${spring.flyway.encoding:UTF-8}")
    private String encoding;

    /** 迁移 sql 脚本文件存放路径，默认 classpath:flyway/sql/mysql */
    @Value("${spring.flyway.locations:classpath:flyway/sql/mysql}")
    private String locations;

    /** 迁移 sql 脚本文件名称前缀，默认 V */
    @Value("${spring.flyway.sql-migration-prefix:V}")
    private String sqlMigrationPrefix;

    /** 迁移 sql 脚本文件名称分隔符，默认双下划线 __ */
    @Value("${spring.flyway.sql-migration-separator:__}")
    private String sqlMigrationSeparator;

    /** 占位符前缀 */
    @Value("${spring.flyway.placeholder-prefix:#(}")
    private String placeholderPrefix;

    /** 占位符后缀 */
    @Value("${spring.flyway.placeholder-suffix:)}")
    private String placeholderSuffix;

    /** 迁移 sql 脚本文件后缀 */
    @Value("${spring.flyway.sql-migration-suffixes:.sql}")
    private String sqlMigrationSuffixes;

    /** 迁移时是否校验，默认 true */
    @Value("${spring.flyway.validate-on-migrate:true}")
    private Boolean validateOnMigrate;

    /**
     * 数据库非空且存在无元数据的表时，自动执行基准迁移并新建 schema_version 表
     */
    @Value("${spring.flyway.baseline-on-migrate:true}")
    private Boolean baselineOnMigrate;

    /**
     * 是否禁用 clean 功能（生产环境必须为 true，否则会清空数据库！）
     */
    @Value("${spring.flyway.clean-disabled:true}")
    private Boolean cleanDisabled;

    @PostConstruct
    public void migrate() {
        if (!enabled) {
            log.info("【数据库升级】Flyway 未启用，跳过迁移（spring.flyway.enabled=false）");
            return;
        }

        if (dataSourceUrl.contains("mysql")) {
            try {
                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .locations(locations)
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
                flyway.migrate();
                log.info("【数据库升级】Flyway 数据库版本自动升级完成！");
            } catch (FlywayException e) {
                log.error("【数据库升级】Flyway 执行 sql 脚本失败", e);
                throw e;
            }
        } else {
            log.warn("【数据库升级】当前仅支持 MySQL 的 Flyway 自动升级，其他数据库请手工执行升级脚本。datasource.url={}", dataSourceUrl);
        }
    }
}
