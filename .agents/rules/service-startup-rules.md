# 服务与模块启动规范 (Service Startup Rules)

- **Nacos 前置状态判断**：
  - **启动顺序硬性约束**：在启动除 Nacos（`jeecg-cloud-nacos`）服务以外的任何模块（如 `jeecg-cloud-gateway` 网关、`jeecg-system-cloud-start` 业务微服务、各类监控和扩展服务等）之前，**必须先判断并确保 Nacos 服务已启动并处于正常运行状态**（默认端口 `8848`）。
  - **原因与风险提示**：微服务架构各模块强依赖 Nacos 作为注册中心与配置中心，若 Nacos 未就绪，其他服务在初始化阶段将因无法拉取配置或注册服务而直接启动失败。
