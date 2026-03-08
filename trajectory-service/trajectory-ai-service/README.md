# trajectory-ai-service - 智能服务

智能服务是 `trajectory-cloud` 的 AI 枢纽，通过集成 **LangChain4j** 与 **Aliyun DashScope**，为全系统提供智能数据分析与动态图表可视化生成能力。

## 🌟 核心功能

- **智能数据分析 (BI)**：
    - 支持同步与异步两种分析模式。
    - 自动将原始数据集 (Excel/CSV) 转换为结构化分析结论。
- **动态图表可视化**：
    - AI 自动生成符合 Echarts 规范的配置项。
    - 支持多种图表类型 (Bar, Line, Pie 等) 的智能适配。
- **异步任务队列**：
    - 基于 **RabbitMQ** 处理耗时较长的分析任务。
    - 任务完成后通过系统通知告知用户。

## 🛠️ 技术栈

- **AI SDK**: LangChain4j 0.36.2
- **大模型**: Aliyun DashScope (通义千问)
- **核心框架**: Spring Boot 3.5.9
- **消息驱动**: RabbitMQ

## 📡 核心 API 概览

| 模块     | 路径                    | 方法   | 描述              |
|:-------|:----------------------|:-----|:----------------|
| **同步分析** | `/ai/analysis/gen`     | POST | 同步生成图表与分析      |
| **异步分析** | `/ai/analysis/gen/async`| POST | 异步提交分析任务        |
| **任务消费** | `bi_chart_queue`       | MQ   | 异步分析任务消费者       |

## 🚀 启动与运行

- **服务端口**: `8089`
- **依赖服务**: Nacos, 以及已配置的大模型 API Key。

---

**维护者**: StephenQiu30  
**版本**: 1.0.0
