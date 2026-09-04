# 数据库变更与管理规范 (Database Rules)

- **默认数据库类型**：当前项目默认数据库为 **MySQL**。
- **强制使用 Flyway 增量管理**：后续所有数据库表结构变动（DDL）及系统/初始化数据变更（DML），**必须统一通过编写 Flyway 脚本进行迁移与版本管理**。
- **严禁直接修改数据库**：**严禁直接在数据库中手动修改表结构或数据**，所有变更必须保证脚本化、可版本化追踪、可自动化执行。
- **Flyway 脚本目录与命名**：
  - 存放路径：`jeecg-boot/jeecg-module-system/jeecg-module-flyway/src/main/resources/flyway/sql/mysql/`
  - 命名格式：`V[版本号/日期]_[序号]__[模块名缩写]_[操作类型]_[业务描述].sql`（例如 `V3.9.5_1__sys_add_user_field.sql`）。
