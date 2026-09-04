# JeecgBoot 服务与模块架构全景指南

本文档全面梳理与介绍 JeecgBoot 平台的所有模块、服务定位、核心职能、默认端口及启动方式，支持**单体架构**与**微服务架构**两种运行模式。

---

## 目录
- [1. 整体架构概览](#1-整体架构概览)
- [2. 核心底座模块](#2-核心底座模块)
- [3. 业务与功能模块 (jeecg-module-system / jeecg-boot-module)](#3-业务与功能模块)
- [4. 微服务核心组件 (jeecg-server-cloud)](#4-微服务核心组件)
- [5. 可视化运维与中间件支持 (jeecg-visual)](#5-可视化运维与中间件支持)
- [6. 独立工具模块](#6-独立工具模块)
- [7. 前端与容器基础设施](#7-前端与容器基础设施)
- [8. 服务清单与默认端口速查表](#8-服务清单与默认端口速查表)

---

## 1. 整体架构概览

JeecgBoot 采用“一套代码，双模运行”的设计理念，既可以作为单体工程打包运行，也可以无缝切换为分布式微服务集群。

```mermaid
flowchart TD
    subgraph 前端与入口
        Vue3[jeecgboot-vue3 前端 / 端口 80]
        Gateway[jeecg-cloud-gateway 网关 / 端口 9999]
    end

    subgraph 单体运行模式
        Monolith[jeecg-system-start / 端口 8080]
    end

    subgraph 微服务集群
        SystemCloud[jeecg-system-cloud-start 系统微服务]
        DemoCloud[jeecg-demo-cloud-start Demo微服务]
    end

    subgraph 基础设施与可视化 (jeecg-visual)
        Nacos[Nacos 注册与配置中心 / 8848]
        Sentinel[Sentinel 熔断限流控制台 / 9000]
        Monitor[Spring Boot Admin 监控面板]
        XXLJob[XXL-Job 分布式调度中心 / 9080]
    end

    subgraph 独立数据运维
        Flyway[jeecg-module-flyway 独立数据库迁移升级工具]
    end

    Vue3 -->|单体请求| Monolith
    Vue3 -->|微服务请求| Gateway
    Gateway --> SystemCloud
    Gateway --> DemoCloud
    SystemCloud -.-> Nacos
    DemoCloud -.-> Nacos
    Gateway -.-> Nacos
```

---

## 2. 核心底座模块

### 2.1 `jeecg-boot-base-core`
- **模块路径**: `jeecg-boot/jeecg-boot-base-core`
- **定位**: 系统的通用底层与核心能力支撑包。
- **主要职能**:
  - 提供系统通用常量、枚举、工具类（`oConvertUtils`、`DateUtils` 等）。
  - 集成与封装 MyBatis-Plus 通用分页、动态查询生成器（`QueryGenerator`）、数据权限拦截器。
  - 封装认证鉴权底座（支持 Shiro 与 Sa-Token 双重方案）、安全过滤、防止 SQL 注入。
  - 统一 API 响应格式（`Result<T>`）、全局异常处理、Swagger / Knife4j 接口文档配置。
  - 集成 Dynamic-Datasource 动态多数据源，支持运行时多数据库切换与读写分离。

---

## 3. 业务与功能模块

### 3.1 `jeecg-system-api`
- **模块路径**: `jeecg-boot/jeecg-module-system/jeecg-system-api`
- **包含子模块**:
  - `jeecg-system-local-api`: 单体模式接口定义，供同一 JVM 内模块间调用系统接口。
  - `jeecg-system-cloud-api`: 微服务模式接口定义，基于 OpenFeign 声明 RPC 远程调用契约，实现微服务间解耦与服务交互。

### 3.2 `jeecg-system-biz`
- **模块路径**: `jeecg-boot/jeecg-module-system/jeecg-system-biz`
- **定位**: 平台核心业务逻辑实现层。
- **主要职能**:
  - **基础权限**: 用户管理、角色管理、部门/组织机构、菜单权限、数据字典、操作日志。
  - **低代码引擎**: Online 表单开发、Online 报表、Online 编码规则、系统分类字典。
  - **集成能力**: 企业微信/钉钉免密登录与通讯录同步、第三方 OAuth2 登录（JustAuth）、积木报表（JimuReport / JimuBI）、AI 模块联动。

### 3.3 `jeecg-boot-module-airag`
- **模块路径**: `jeecg-boot/jeecg-boot-module/jeecg-boot-module-airag`
- **定位**: AI 大模型与 RAG（检索增强生成）知识库扩展模块。
- **主要职能**:
  - 对接国内外主流大模型（DeepSeek、OpenAI、文心一言、通义千问等）。
  - 集成 LiteFlow 规则引擎编排 AI 业务流程。
  - 结合向量数据库（如 pgvector）实现本地知识库切片、向量存储与精准语义召回。

### 3.4 `jeecg-module-demo`
- **模块路径**: `jeecg-boot/jeecg-boot-module/jeecg-module-demo`
- **定位**: 开发示例与最佳实践模块。
- **主要职能**:
  - 提供单表、一对多、树形表单等常用业务场景的示例代码。
  - 演示各类前端组件与后端接口交互标准。

### 3.5 `jeecg-system-start`
- **模块路径**: `jeecg-boot/jeecg-module-system/jeecg-system-start`
- **主启动类**: `org.jeecg.JeecgSystemApplication`
- **默认端口**: `8080`
- **定位**: **单体架构启动入口**。
- **主要职能**:
  - 聚合 `jeecg-system-biz`、`jeecg-module-demo` 等全部业务模块，以单体应用形式运行。
  - 适合中小型项目、轻量部署及本地单机开发调试。

---

## 4. 微服务核心组件 (jeecg-server-cloud)

### 4.1 `jeecg-cloud-gateway`
- **模块路径**: `jeecg-boot/jeecg-server-cloud/jeecg-cloud-gateway`
- **主启动类**: `org.jeecg.JeecgGatewayApplication`
- **默认端口**: `9999`
- **定位**: **微服务统一 API 网关**（基于 Spring Cloud Gateway）。
- **主要职能**:
  - 客户端流量统一入口与服务路由转发。
  - 全局统一安全鉴权、Token 校验与黑白名单过滤。
  - 跨域配置（CORS）、请求链路追踪、限流熔断降级。
  - 动态路由加载与 Swagger/Knife4j 微服务聚合文档分发。

### 4.2 `jeecg-cloud-nacos`
- **模块路径**: `jeecg-boot/jeecg-server-cloud/jeecg-cloud-nacos`
- **默认端口**: `8848` (控制台) / `18080`
- **定位**: **服务注册与配置中心**（Nacos 服务端）。
- **主要职能**:
  - 提供微服务实例的注册、心跳保活与健康发现。
  - 集中管理并动态推送所有微服务的配置（支持热更新）。

### 4.3 `jeecg-system-cloud-start`
- **模块路径**: `jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start`
- **主启动类**: `org.jeecg.JeecgSystemCloudApplication`
- **服务名**: `jeecg-system`
- **定位**: **微服务模式下的系统核心服务**。
- **主要职能**:
  - 承载所有的系统权限、组织机构、Online 低代码及核心业务 API。
  - 接入 Nacos 注册中心并支持集群多实例部署与负载均衡。

### 4.4 `jeecg-demo-cloud-start`
- **模块路径**: `jeecg-boot/jeecg-server-cloud/jeecg-demo-cloud-start`
- **主启动类**: `org.jeecg.JeecgDemoCloudApplication`
- **服务名**: `jeecg-demo`
- **定位**: **微服务模式下的业务示范独立服务**。
- **主要职能**:
  - 作为独立业务微服务的示范工程，演示微服务间通过 Feign 互调系统服务、分布式锁、分布式事务等能力。

---

## 5. 可视化运维与中间件支持 (jeecg-visual)

`jeecg-visual` 聚合了微服务架构下常用的运维管理与基础设施服务：

### 5.1 `jeecg-cloud-monitor`
- **模块路径**: `jeecg-boot/jeecg-server-cloud/jeecg-visual/jeecg-cloud-monitor`
- **主启动类**: `org.jeecg.monitor.JeecgMonitorApplication`
- **定位**: **微服务监控面板（Spring Boot Admin Server）**。
- **主要职能**:
  - 自动从 Nacos 发现各个微服务实例。
  - 可视化查看各微服务的 JVM 运行状态、内存使用率、GC 频次、线程池状态、Actuator 端点指标。

### 5.2 `jeecg-cloud-sentinel`
- **模块路径**: `jeecg-boot/jeecg-server-cloud/jeecg-visual/jeecg-cloud-sentinel`
- **默认端口**: `9000`
- **定位**: **流量防卫与熔断限流控制台（Sentinel Dashboard 定制版）**。
- **主要职能**:
  - 实时监控服务调用 QPS、响应时间与异常比例。
  - 在线动态配置流控规则、降级熔断规则、热点参数限流规则，并将规则持久化同步至 Nacos。

### 5.3 `jeecg-cloud-xxljob`
- **模块路径**: `jeecg-boot/jeecg-server-cloud/jeecg-visual/jeecg-cloud-xxljob`
- **主启动类**: `com.xxl.job.admin.XxlJobAdminApplication`
- **默认端口**: `9080`
- **定位**: **分布式任务调度中心（XXL-Job Admin）**。
- **主要职能**:
  - 提供可视化调度任务配置、触发策略管理与执行器注册面板。
  - 统一调度分配各个微服务节点执行后台批处理任务，支持失败重试与告警。

### 5.4 `jeecg-cloud-test` (测试套件)
- **模块路径**: `jeecg-boot/jeecg-server-cloud/jeecg-visual/jeecg-cloud-test`
- **包含组件**:
  - `jeecg-cloud-test-more`: Feign 远程调用、熔断降级与 Redis 分布式锁综合测试。
  - `jeecg-cloud-test-rabbitmq`: RabbitMQ 异步消息收发与死信队列测试。
  - `jeecg-cloud-test-rocketmq`: RocketMQ 分布式消息队列测试。
  - `jeecg-cloud-test-seata`: Seata 分布式事务集成示例（Account、Order、Storage 联动回滚测试）。
  - `jeecg-cloud-test-shardingsphere`: ShardingSphere-JDBC 分库分表示例。

---

## 6. 独立工具模块

### 6.1 `jeecg-module-flyway`
- **模块路径**: `jeecg-boot/jeecg-module-system/jeecg-module-flyway`
- **主启动类**: `org.jeecg.flyway.FlywayApplication`
- **运行模式**: 独立命令行应用（`WebApplicationType.NONE`，无 Web 容器开销）
- **定位**: **Flyway 独立数据库版本升级与数据迁移工具**。
- **主要职能**:
  - 存放平台所有版本增量变更 SQL 脚本（`flyway/sql/mysql/`）。
  - 通过环境变量或命令行注入 MySQL 连接信息，执行全量/增量脚本迁移。
  - 具备版本基线校验、防误删保护（`clean-disabled: true`），执行完毕后进程自动安全退出。
- **使用命令**:
  ```bash
  java -jar jeecg-module-flyway-3.9.5.jar \
    --spring.datasource.url="jdbc:mysql://localhost:3306/jeecg-boot?characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
    --spring.datasource.username=root \
    --spring.datasource.password=root \
    --spring.flyway.enabled=true
  ```

---

## 7. 前端与容器基础设施

### 7.1 `jeecgboot-vue3`
- **定位**: 基于 Vue3 + TypeScript + Ant Design Vue + Vite 构建的现代化 Web 前端。
- **默认端口**: `80` (Docker 部署) / `3100` (本地开发热更新)
- **主要职能**: 提供全套中后台 UI、动态路由加载、Online 低代码表单构建器与拖拽设计器、AI 助手会话交互界面。

### 7.2 支撑容器
- **`jeecg-boot-mysql`**: 平台元数据与业务关系型数据库（端口: 13306/3306）。
- **`jeecg-boot-redis`**: 高性能缓存、分布式锁与会话共享中间件（端口: 6379）。
- **`jeecg-boot-pgvector`**: PostgreSQL + pgvector 插件，专门为 AI 知识库提供高维向量检索能力（端口: 5432）。

---

## 8. 服务清单与默认端口速查表

| 服务 / 模块名称 | 所在目录 | 默认端口 | 运行形态 | 核心说明 |
|---|---|---|---|---|
| **jeecgboot-vue3** | `jeecgboot-vue3` | `80` / `3100` | 前端服务 | 基于 Vue3 的前端交互客户端 |
| **jeecg-system-start** | `jeecg-module-system/jeecg-system-start` | `8080` | 单体后端 | **单体模式主应用**，集成所有系统功能与 Demo |
| **jeecg-cloud-gateway** | `jeecg-server-cloud/jeecg-cloud-gateway` | `9999` | 微服务网关 | 微服务总入口，统一鉴权与路由转发 |
| **jeecg-cloud-nacos** | `jeecg-server-cloud/jeecg-cloud-nacos` | `8848` / `18080` | 微服务注册中心 | Nacos 服务注册、发现与配置中心 |
| **jeecg-system-cloud-start** | `jeecg-server-cloud/jeecg-system-cloud-start` | 随机 / 动态 | 微服务业务主服务 | 微服务核心，提供系统管理与通用业务 API |
| **jeecg-demo-cloud-start** | `jeecg-server-cloud/jeecg-demo-cloud-start` | 随机 / 动态 | 微服务业务示例 | 演示微服务间调用与独立微服务开发 |
| **jeecg-cloud-sentinel** | `jeecg-server-cloud/jeecg-visual/jeecg-cloud-sentinel` | `9000` | 可视化运维 | Sentinel Dashboard 流量与限流控制台 |
| **jeecg-cloud-xxljob** | `jeecg-server-cloud/jeecg-visual/jeecg-cloud-xxljob` | `9080` | 可视化运维 | XXL-Job 分布式任务调度控制台 |
| **jeecg-cloud-monitor** | `jeecg-server-cloud/jeecg-visual/jeecg-cloud-monitor` | 8088 / 自定义 | 可视化运维 | Spring Boot Admin 微服务健康监控中心 |
| **jeecg-module-flyway** | `jeecg-module-system/jeecg-module-flyway` | 无端口 | 独立工具应用 | 数据库版本迁移命令行工具，跑完自动退出 |
