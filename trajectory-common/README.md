# trajectory-common (系统公共核心组件)

> **轨迹 Cloud 的技术底座：全系统通用的基础设施、工具类与标准抽象层**

**Common 模块** 聚合了全系统共享的核心逻辑。通过高度抽象的组件化设计，确保了微服务集群在缓存处理、数据库访问、消息通信、及 Web 响应规范上的高度统一。

---

## 📂 核心子组件

| 组件名 | 描述 | 核心技术 |
| :--- | :--- | :--- |
| `trajectory-common-core` | 系统通用基类、常量、自定义异常、Result 包装 | Spring Context |
| `trajectory-common-web` | WebMvc 配置、鉴权拦截器、全局异常处理器 (Mvc 版) | Spring Web |
| `trajectory-common-cache` | 分布式缓存抽象、Redis 工具封装 | Spring Data Redis |
| `trajectory-common-mysql` | MyBatis-Plus 增强配置、单表分片辅助 | MyBatis Plus |
| `trajectory-common-rabbitmq` | 消息队列抽象、消息可靠投递机制 | Spring Rabbit |
| `trajectory-common-websocket` | 实时通信基础设施 (Netty / WebSocket) | Netty |

---

## 🛠️ 设计哲学

1.  **开箱即用**：各组件均提供自动装配配置类，子模块只需引入依赖即可获得增强能力。
2.  **契约优先**：统一定义 `Result<T>` 响应格式与错误码标准，确保前后端及服务间通信的语义一致性。
3.  **零污染**：尽量减少对业务逻辑的侵入，通过注解（如 `@AuthCheck`, `@Log`）或拦截器实现横切关注点。

---

## 🚀 最佳实践

### 统一响应示例
```java
// 使用 Common-Core 提供的 Result 工具
return ResultUtils.success(data);
```

### 接入 Redis 缓存
```xml
<dependency>
    <groupId>com.stephen</groupId>
    <artifactId>trajectory-common-cache</artifactId>
</dependency>
```

---

**维护者**: [StephenQiu30](https://github.com/StephenQiu30)  
**版本**: 1.0.0
