# trajectory-gateway (系统网关)

[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2022.x-blue.svg)](https://spring.io/projects/spring-cloud-gateway)
[![Sa-Token](https://img.shields.io/badge/Auth-Sa--Token%20Reactor-brightgreen.svg)](https://sa-token.cc/)
[![Nacos](https://img.shields.io/badge/Config-Nacos-blue.svg)](https://nacos.io/)

> **轨迹 Cloud 的统一流量入口：基于 Spring Cloud Gateway 的高性能 API 网关**

**系统网关** 作为微服务集群的唯一入口，承担着流量路由、安全认证、请求洗净、分布式限流及全链路追踪的核心职责。

---

## 🚀 核心能力

| 能力 | 说明 | 亮点 |
| :--- | :--- | :--- |
| **动态路由** | 基于 Nacos 服务发现，按路径前缀实现 lb 负载均衡转发 | 高可用、易扩展 |
| **统一认证** | 集成 Sa-Token (Reactor 版)，强制校验 Token 并下发会话信息 | 分布式 Session 共享 |
| **请求洗净** | 最高优先级过滤器，彻底剥离伪造的敏感请求头（如 `userId`, `userName`） | 生产级安全保障 |
| **分布式限流** | 基于 Redis 令牌桶算法，支持 IP、用户、API 三维精准限流 | 流量洪峰保护 |
| **全链路追踪** | 注入 `X-Trace-Id`，记录请求全生命周期耗时日志并异步上报 | 快速问题定位 |
| **全局异常** | 统一定义 JSON 容错响应，覆盖网关层所有异常场景 | 友好错误提示 |

---

## 🛠️ 过滤器执行链路

```mermaid
graph LR
    A[客户端请求] --> B[HeaderSanitize]
    B --> C[Log - Start]
    C --> D[Sa-Token Auth]
    D --> E[Redis RateLimit]
    E --> F[内网服务路由]
    F --> G[Log - End]
    G --> H[客户端响应]
```

### 过滤器细节

| 过滤器名称 | Order (优先级) | 关键职责 |
| :--- | :--- | :--- |
| `GlobalHeaderSanitizeFilter` | `HIGHEST` | 阻断外部伪造敏感字段，确保后端受保护 |
| `GlobalLogFilter` | `-200` | 初始化 `X-Trace-Id`，统计全程响应耗时 |
| `GlobalAuthFilter` | `-98` | 状态机校验，注入 `userId` 到请求上下文 |

---

## 📡 路由映射概览 (Routes)

| 物理 ID | 匹配路径 | 转发目标 (lb) |
| :--- | :--- | :--- |
| `user-service` | `/api/user/**` | `trajectory-user-service` |
| `ai-service` | `/api/ai/**` | `trajectory-ai-service` |
| `notif-service` | `/api/notification/**` | `trajectory-notification-service` |
| `search-service` | `/api/search/**` | `trajectory-search-service` |
| `file-service` | `/api/file/**` | `trajectory-file-service` |

---

## 📂 项目结构

```text
trajectory-gateway
├── config/         # 限流算法与 WebClient 配置
├── filter/         # 核心过滤器链实现
├── handler/        # 网关层全局异常处理 (JsonError)
├── util/           # 响应封装与上下文搜索工具
└── GatewayApplication.java # 网关入口
```

---

## 🏃 启动与运行

-   **服务端口**: `8080`
-   **Nacos 命名空间**: `trajectory-cloud`
-   **关键依赖**: Nacos (配置/服务中心), Redis (限流与鉴权)
-   **配置参考**: 见 `trajectory-cloud-gateway.yml` 于 Nacos

---

**维护者**: [StephenQiu30](https://github.com/StephenQiu30)  
**版本**: 1.0.0
