# trajectory-api (公共 Feign 接口定义)

[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-OpenFeign-blue.svg)](https://spring.io/projects/spring-cloud-openfeign)

> **轨迹 Cloud 的服务间通信契约：定义全系统的 RPC 接口与 Feign 客户端**

**API 模块** 统一管理各微服务对外暴露的远程调用接口。通过将 Feign 接口抽象到独立模块，实现了服务间的解耦与契约一致性，避免了重复定义接口带来的维护成本。

---

## 📂 模块划分

| 子模块 | 对应服务 | 描述 |
| :--- | :--- | :--- |
| `trajectory-api-user` | `user-service` | 用户基础信息获取、鉴权详情查询等 |
| `trajectory-api-ai` | `ai-service` | AI 任务提交、分析结果异步回调等 |
| `trajectory-api-notification` | `notification-service` | 发送实时通知、消息状态管理等 |

---

## 🛠️ 使用指南

### 1. 引入依赖
在需要调用远程服务的模块 `pom.xml` 中引入对应的 API 依赖：
```xml
<dependency>
    <groupId>com.stephen</groupId>
    <artifactId>trajectory-api-user</artifactId>
</dependency>
```

### 2. 启用 Feign 客户端
在启动类上配置扫描路径：
```java
@EnableFeignClients(basePackages = "com.stephen.trajectory.api")
```

---

## ✨ 设计规范

-   **Fallback 机制**：所有 Feign 接口均需配置对应的 `FallbackFactory`，确保在下游服务不可用时能优雅降级。
-   **路径一致性**：Feign 接口的 `@RequestMapping` 必须与微服务 Controller 中的路径完全保持一致。
-   **入参脱敏**：RPC 接口仅传递必要的 DTO 数据，严禁直接传递包含敏感信息的 Entity 对象。

---

**维护者**: [StephenQiu30](https://github.com/StephenQiu30)  
**版本**: 1.0.0
