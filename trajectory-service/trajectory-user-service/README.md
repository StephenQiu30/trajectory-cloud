# trajectory-user-service (用户服务)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Sa-Token](https://img.shields.io/badge/Auth-Sa--Token-blue.svg)](https://sa-token.cc/)
[![MySQL](https://img.shields.io/badge/DB-MySQL-orange.svg)](https://www.mysql.com/)

> **轨迹 Cloud 系统的身份认证与用户管理核心基石**

**用户服务** 是系统的安全底座，负责全周期的账号生命周期管理、多渠道身份认证、细粒度权限管控、以及分布式 Session 调度。

---

## 🌟 核心功能

-   **🔐 多维度身份认证**：支持邮箱验证码、社交账号（GitHub、微信扫码）等多种登录方式，集成 OAuth2 流程。
-   **🚀 高性能会话管理**：基于 **Sa-Token** 实现。支持多端在线监控、强制下线、同端互斥登录等高级特性。
-   **🛡️ RBAC 权限体系**：提供基于角色与权限的精细化访问控制，支持 `@AuthCheck` 高级注解实现声明式拦截。
-   **⚡ 安全响应机制**：内置分布式频率限制器（Rate Limiter），防御暴力破解；完善的数据脱敏过滤器保证隐私安全。

---

## 🛠️ 技术栈

-   **核心框架**：Spring Boot 3.5.x + MyBatis-Plus 3.5.x
-   **认证鉴权**：Sa-Token 1.44.x (Reactor 支持)
-   **存储方案**：MySQL 8.4 (持久化) + Redis (分布式 Session & 验证码缓存)
-   **消息驱动**：RabbitMQ (用于分析用户轨迹或发送通知)
-   **文档工具**：Knife4j (Swagger 3)

---

## 📡 核心 API 概览

| 模块 | 路径 | 方法 | 说明 |
| :--- | :--- | :--- | :--- |
| **基础认证** | `/api/user/login/email` | `POST` | 邮箱验证码快捷登录 |
| **三方集成** | `/api/user/login/github` | `GET` | 发起 GitHub 授权流程 |
| **状态查询** | `/api/user/get/login` | `GET` | 实时获取当前登录用户信息 |
| **权限管理** | `/api/user/list/page/vo` | `POST` | 用户列表深度检索 (Admin) |

---

## 📂 项目结构

```text
trajectory-user-service
├── controller/     # 接口层 (RESTful API)
├── service/        # 业务逻辑层 (登录、三方集成、管理)
├── mapper/         # 数据持久层
├── model/          # 领域模型 (Entity, DTO, VO)
└── manager/        # 第三方平台对接 (GitHub, Redis 等)
```

---

## 🚀 启动与运行

-   **服务端口**: `8081`
-   **Nacos 命名空间**: `trajectory-cloud`
-   **前置依赖**: Nacos, MySQL 8, Redis, RabbitMQ
-   **快速运行**:
    ```bash
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ```

---

**维护者**: [StephenQiu30](https://github.com/StephenQiu30)  
**版本**: 1.0.0
