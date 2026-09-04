package org.jeecg.flyway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Flyway 独立数据库版本管理工具启动类
 *
 * <p>使用方式：
 * <pre>
 *   java -jar jeecg-module-flyway.jar \
 *     --spring.datasource.url=jdbc:mysql://localhost:3306/jeecg-boot?... \
 *     --spring.datasource.username=root \
 *     --spring.datasource.password=root \
 *     --spring.flyway.enabled=true
 * </pre>
 *
 * <p>启动后自动执行 flyway/sql/mysql/ 下的增量 SQL，完成后进程自动退出。
 *
 * @author jeecg
 */
@Slf4j
@SpringBootApplication
public class FlywayApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FlywayApplication.class);
        // 非 Web 模式：迁移完成后自动退出，无需手动关闭
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
        log.info("【数据库升级】Flyway 迁移工具执行完毕，进程退出。");
    }
}
