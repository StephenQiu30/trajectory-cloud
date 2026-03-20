# trajectory-notification-service (通知服务)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/MQ-RabbitMQ-orange.svg)](https://www.rabbitmq.com/)
[![WebSocket](https://img.shields.io/badge/Push-WebSocket-blue.svg)](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)

> **轨迹 Cloud 的消息中枢：实现全方位、实时的多渠道消息触达**

**通知服务** 提供统一的消息生命周期管理，确保系统公告、业务提醒及各种互动（如点赞、评论）能够通过异步事件驱动，毫秒级推送到位。

---

## 🌟 核心功能

-   **🔔 统一消息管理**：覆盖系统级别与用户级别的全量通知类型，支持一键已读、未读数动态统计。
-   **📩 智能化分发引擎**：
    -   集成 **RabbitMQ** 消费来自其他业务微服务的交互事件。
    -   支持基于用户 ID 的定向私信推送与全站广播通知。
-   **⚡ 高性能实时推送**：
    -   与 `trajectory-websocket-service` (基于 Netty) 紧密流转。
    -   通过 WebSocket 实现 Web 端的无感刷新消息通知。

---

## 🛠️ 技术栈

-   **核心框架**：Spring Boot 3.5.x + MyBatis-Plus
-   **消息总线**：RabbitMQ (事件溯源与服务解耦)
-   **持久化层**：MySQL 8.4 (存储历史通知记录)
-   **实时推流**：Netty / WebSocket (分布式消息下发)

---

## 📡 核心 API 概览

| 模块 | 路径 | 方法 | 描述 |
| :--- | :--- | :--- | :--- |
| **消息创建** | `/api/notification/add` | `POST` | 手动发起系统公告 (Admin) |
| **状态变更** | `/api/notification/read` | `POST` | 标记单条或批量消息为已读 |
| **即时统计** | `/api/notification/unread/count` | `GET` | 实时计算当前用户未读总数 |
| **历史追踪** | `/api/notification/list/page` | `POST` | 分页浏览历史通知流水 |

---

## 📂 项目结构

```text
trajectory-notification-service
├── controller/     # RESTful 服务接口
├── service/        # 核心业务逻辑 (已读未读状态机)
├── listener/       # MQ 事件监听器 (处理各模块广播)
├── mapper/         # 数据库映射
└── model/          # 消息领域对象 (Entity, VO)
```

---

## 🚀 启动与运行

-   **服务端口**: `8082`
-   **WebSocket 端口**: `9090`
-   **命名空间**: `trajectory-cloud`
-   **前置依赖**: Nacos, MySQL, RabbitMQ, Redis
-   **启动引导**:
    ```bash
    mvn spring-boot:run
    ```

---

**维护者**: [StephenQiu30](https://github.com/StephenQiu30)  
**版本**: 1.0.0
