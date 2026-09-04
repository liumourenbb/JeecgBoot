package org.jeecg.flyway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;

/**
 * Flyway 独立数据库版本管理工具启动类
 *
 * <p>支持业务主库 (master) 与 Nacos (nacos) 等多数据源自动检测与增量迁移升级。
 * <p>运行方式：
 * <pre>
 *   java -jar jeecg-module-flyway.jar
 * </pre>
 *
 * <p>启动后自动完成所有数据源的校验与升级，完成后进程自动安全退出（退出码 0）。
 *
 * @author jeecg
 */
@Slf4j
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
public class FlywayApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FlywayApplication.class);
        // 非 Web 模式：迁移完成后自动退出，无需手动关闭
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
        log.info("【数据库升级】Flyway 迁移工具执行完毕，进程安全退出。");
    }
}
