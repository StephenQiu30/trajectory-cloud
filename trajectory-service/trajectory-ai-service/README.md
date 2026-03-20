# trajectory-ai-service (智能服务)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/AI-LangChain4j-blue.svg)](https://docs.langchain4j.dev/)
[![RabbitMQ](https://img.shields.io/badge/MQ-RabbitMQ-orange.svg)](https://www.rabbitmq.com/)

> **轨迹 Cloud 的 AI 枢纽：基于大模型的自动化数据分析与图表生成引擎**

**智能服务** 深度集成了 **LangChain4j** 与 **Aliyun DashScope (通义千问)**，为全系统提供端到端的智能数据解析、结构化报告生成及动态图表可视化能力。

---

## 🌟 核心功能

-   **🤖 智能数据分析 (BI)**：
    -   **同步模式**：快速响应小型数据集的即时分析需求。
    -   **异步模式**：处理海量数据上传，支持长耗时任务的后台静默执行。
-   **📊 动态图表可视化**：
    -   AI 自动推理并生成符合 **ECharts / AntV** 规范的多维度配置项。
    -   智能适配多种图表类型 (Bar, Line, Pie, Radar 等)。
-   **⚡ 高并发任务调度**：
    -   基于 **RabbitMQ** 构建可靠的任务分发队列，实现削峰填谷。
    -   任务进度与结果通过消息通知或 WebSocket 实时反馈初端。

---

## 🧩 业务流程

![核心流程](../docs/image-20241215223010544.png)

## 🛠️ 技术栈

-   **AI 调度层**：LangChain4j 0.36.2 (Java 版本的 AI 精英框架)
-   **后端基座**：Spring Boot 3.5.x + MyBatis-Plus
-   **智能大脑**：Aliyun DashScope (Qwen-Max/Plus 级大模型)
-   **消息总线**：RabbitMQ (全异步事件驱动)
-   **工具套件**：EasyExcel (百万级数据流式解析)

---

## 📡 核心 API 概览

| 模块 | 路径 | 方法 | 描述 |
| :--- | :--- | :--- | :--- |
| **即时分析** | `/ai/analysis/gen` | `POST` | 同步生成图表配置与分析结论 |
| **异步任务** | `/ai/analysis/gen/async` | `POST` | 提交后台分析任务 (返回任务ID) |
| **任务消费** | `bi_chart_queue` | `MQ` | 监听并执行 AI 生成逻辑的消费者 |

---

## 📂 项目结构

```text
trajectory-ai-service
├── controller/     # AI 接口层
├── service/        # 核心逻辑 (BI 分析、图表推理)
├── manager/        # AI 模型调用抽象 (DashScope 封装)
├── mq/             # 消息生产者与消费者实现
└── model/          # AI 专用结构定义 (Query, DTO, VO)
```

---

## 🚀 启动与运行

-   **服务端口**: `8083`
-   **前置要求**:
    -   配置 Nacos 开发环境。
    -   在环境变量或配置文件中注入 `dashscope.api-key`。
-   **启动引导**:
    ```bash
    mvn spring-boot:run
    ```

---

**维护者**: [StephenQiu30](https://github.com/StephenQiu30)  
**版本**: 1.0.0
