# QA-Agent-Pumluda 项目开发指引

## 项目概述

复刻项目 [QA-Agent](D:\JavaProject\QA-Agent)，一个**技术面试训练工作台**。用户上传 Markdown 学习材料 → 系统自动分块、向量化 → 基于 LangChain4j 的 RAG Agent 进行智能检索与流式对话 → 支持多轮记忆、来源引用、Typing 效果。

**目标：** 按 V1 → V2 → V3 → V4 → V5 → V6 版本迭代，逐步复刻原项目核心能力。当前在 V5 阶段。

**当前分支：** `v5-Frontend`

## 技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | - |
| 框架 | Spring Boot | 3.4.3 | 父 POM |
| AI 框架 | LangChain4j BOM | 1.14.0 | 管理所有 langchain4j 模块版本 |
| AI 模块 | langchain4j-open-ai | 1.14.0 | DeepSeek 兼容 OpenAI 协议 |
| AI 模块 | langchain4j-agentic | 1.14.0-beta24 | @Tool 注解 / AiServices / DAG |
| 对话模型 | DeepSeek v4-pro | OpenAI 协议 | `OpenAiChatModel` + `OpenAiStreamingChatModel` |
| Embedding | 阿里云 DashScope text-embedding-v3 | 1024维 | `OpenAiEmbeddingModel`（OpenAI 兼容协议） |
| Rerank | 阿里云 DashScope gte-rerank-v2 | - | DashScope 原生 SDK |
| 向量存储 | PostgreSQL pgvector | pg17 | 自定义 `PgVectorStore` (JdbcTemplate) |
| ORM | MyBatis-Plus | 3.5.16 | MySQL 业务数据 |
| 数据库 | MySQL | 8.0+ | 远程 134.175.232.110:13306 |
| 数据库 | PostgreSQL + pgvector + zhparser | pg17 | 远程 134.175.232.110:15432 |
| 消息队列 | Kafka (apache/kafka:3.7.1) | 3.7.1 | 异步索引、消息追踪 |
| Markdown 解析 | flexmark | 0.64.8 | AST 按标题层级分块 |
| DashScope SDK | dashscope-sdk-java | 2.22.17 | Rerank 重排序 |
| JSON | fastjson2 | 2.0.61 | - |
| 工具 | Hutool | 5.8.36 | - |
| 前端 | React 18 + Vite + TypeScript + Tailwind CSS v4 | - | 暖色玻璃拟态风格 |
| 前端 Markdown | react-markdown + remark-gfm + remark-math + rehype-katex | - | 支持 GFM / LaTeX / Callout |

## 项目结构（DDD 六模块 + 前端）

```
QA-Agent-Pumluda/
├── QA-Agent-api/              API 契约 DTO
├── QA-Agent-types/            共享类型、常量、枚举、异常、Response 包装
├── QA-Agent-domain/           领域模型 + 领域服务接口 + 实现 + MCP 工具
│   ├── document/              文档域（上传、分块、检索、Embedding）
│   └── agent/                 Agent 域（对话、记忆、Prompt、查询改写、MCP）
├── QA-Agent-infrastructure/   DAO/PO/Repository 实现、PgVectorStore、Kafka
├── QA-Agent-trigger/          HTTP Controller、Kafka Consumer、定时Job、异常处理
├── QA-Agent-app/              Spring Boot 启动 + 配置类 + 数据源 + Prompts
├── frontend/                  React 前端（V5 新增）
├── docs/
│   ├── dev-docs/V2~V5/        开发日志
│   ├── dev-ops/               Docker Compose + Dockerfile
│   ├── prompts/               Prompt 模板（外置管理）
│   └── sql/                   建表/迁移 SQL
└── CLAUDE.md                  本文件
```

## 版本规划

### 总体路线

| 大版本 | 核心能力 | 状态 |
|--------|----------|------|
| V1 | 框架搭建 + 依赖导入 + LangChain4j 配置 | ✅ |
| V2 | 文档上传 + Markdown 分块 + Embedding + RAG 检索 | ✅ |
| V3 | PostgreSQL pgvector 检索引擎 | ✅ |
| V4 | Agent 智能问答 + 流式对话 + MCP 工具 | ✅ |
| V5 | 前端设计 + SSE 打字机 + Kafka 异步 + 联调 | ✅ |
| V6 | 练习/反馈/评分 + DAG 生成 | ⏳ |

### V2 完成情况

| 子版本 | 核心功能 |
|--------|----------|
| V2.1 | 文档上传 + MySQL + MyBatis-Plus |
| V2.2 | flexmark AST 按标题分块 |
| V2.3 | DashScope Embedding + 语义检索 |
| V2.3.1 | 事务 + MD5 幂等 + 全局异常处理 |
| V2.4 | 关键词检索 + RRF 多路融合混合检索 |
| V2.5 | DashScope gte-rerank 重排序 |

### V3 完成情况

| 子版本 | 核心功能 |
|--------|----------|
| V3.1 | PostgreSQL + pgvector + zhparser 部署 + chunk_search 建表 |
| V3.2 | PgVectorStore 替换 InMemoryEmbeddingStore（JdbcTemplate 手写） |
| V3.3 | 中文分词全文检索（zhparser tsvector + GIN 替换 MySQL LIKE） |
| V3.4 | Kafka 异步索引 + message_job 消息追踪 + 兜底轮询 |

### V4 完成情况

| 子版本 | 核心功能 |
|--------|----------|
| V4.1 | RAG Agent 核心问答 + 多轮记忆 + Prompt 外置管理 |
| V4.2 | 查询改写（口语→关键词） |
| V4.3 | MCP 工具 + KnowledgeSearchTool @Tool 注解（预留） |

### V5 完成情况

| 子版本 | 核心功能 |
|--------|----------|
| V5.1 | 前端设计样板——暖色玻璃拟态 + Welcome/Login/Document/Agent/Search 页 |
| V5.2 | 文档管理对接 + 目录扫描上传 + Markdown/Callout 渲染 + 分块展开 |
| V5.3 | Agent SSE 真流式 + 打字机 + LaTeX/Markdown 渲染 + Emoji 编码 + 换行修复 |
| V5.4 | 检索页完善 + Kafka 异步 Embedding 联调 + Agent 状态持久化 |

## 开发约束

### 依赖管理
- 所有依赖版本号统一在**父 POM `<properties>`** 中声明
- 子模块引用使用 `${property.name}`，禁止硬编码版本号
- 版本号优先对齐 QA-Agent 原项目

### Git 提交
- **Commit message 使用中文**
- 格式：`✨ feat: <描述>`
- 当前分支：`v5-Frontend`（从 v4-Agent 创建）

### 代码风格
- 所有 DDD 层可用 Spring 注解（`@Service`、`@Repository` 等）
- 服务接口命名：`IXxxService` / `IXxxRetriever` / `IXxxer` 等
- 实体字段必须加 `/** */` 注释
- DTO 转换方法按实体类型命名：`toXxxResponse()` / `toXxxPO()` / `toXxxEntity()`
- 多用 `@Slf4j` + `log.info("[模块] ...")` 标注执行状态
- Optional + `orElseThrow(() -> new AppException(...))` 模式
- 自定义业务异常统一通过 `ResponseCode` 枚举 + `AppException` 抛出
- API 路径变量显式写参数名：`@PathVariable("id")`、`@RequestParam("file")`

### API 约定
- 全局异常处理：`GlobalExceptionHandler` 拦截 `AppException` + `Exception`
- 响应统一用 `Response<T>` 包装（code + info + data）
- 中文文件名需 URL 解码：`URLDecoder.decode(rawFileName, StandardCharsets.UTF_8)`
- 前端 SSE 直连后端 `localhost:8091`（不用 Vite 代理——会缓冲 SSE 流）

### 开发日志
- 路径：`docs/dev-docs/<版本号>/`
- 风格：个人学习笔记，Obsidian 格式，多用 callout 和 mermaid 图表
- 调用链/流程图统一用 ````mermaid` 代码块

## DDD 架构红线（全版本踩坑汇总）

### V3 踩坑
1. **domain 禁止引 infra** — 接口放 domain、实现放 infra
2. **双数据源不暴露两个 Bean** — MySQL 自动配置，PostgreSQL 内部 `new HikariDataSource()` 不注册
3. **API 以 properties 声明版本为准** — `EmbeddingStore.findRelevant()` 不存在，1.14.0 是 `search(EmbeddingSearchRequest)`
4. **YAML 一个文件只能一个同名根节点** — 不能有两个 `spring:` 块

### V4 踩坑
5. **`StreamingChatModel` ≠ `ChatModel`** — LangChain4j 1.14.0 是独立接口。AiServices 不支持 `StreamingChatModel`，所以 TOOL 模式只能用伪流式
6. **`ChatModel.chat()` 返回 `ChatResponse` 不是 `String`** — `.aiMessage().text()` 取值
7. **`ChatMemory` API** — `messages()` 不是 `toMessages()`，`add(ChatMessage)` 不是 `append()`
8. **`@Tool` + AiServices 不是最终方案** — 1.14.0 不支持流式 Tool Calling，先预留接口

### V5 踩坑
9. **SSE `\n` 换行丢失** — `\n` 在 SSE `data:` 行内是帧分隔符，须替换为标记 `[BR]`，前端解码
10. **SSE 空格被吞** — 解析器 `.replace(/^\s/, "")` 去掉前导空白。token 事件不 trim
11. **Emoji 编码** — `produces` 须指定 `charset=UTF-8`，否则多字节字符显示 `??`
12. **React StrictMode 双重调用** — `setMsgs` 回调内对象突变（`last.content += t`）导致字符翻倍。必须不可变更新 `{ ...m, content: m.content + t }`
13. **Vite proxy 缓冲 SSE** — Vite dev server 代理会缓冲完整响应才转发。SSE 必须直连后端
14. **CSS `padding` 简写覆盖 Tailwind `pl-*`** — 用 `input-has-icon` 类单独设 `padding-left`

## 数据库表

### MySQL

| 表名 | 用途 | 版本 |
|------|------|------|
| `source_document` | 文档仓库（direction_path 支持目录层级） | V2.1 |
| `document_chunk` | 分块文本（业务真数据） | V2.2 |
| `message_job` | Kafka 异步索引消息追踪 | V3.4 |

### PostgreSQL

| 表名 | 用途 | 版本 |
|------|------|------|
| `chunk_search` | 向量 + 全文索引 + 标签（一表双引擎：HNSW + GIN） | V3.1 |

## 当前状态

### 远程服务
| 服务 | 地址 | 状态 |
|------|------|------|
| MySQL | 134.175.232.110:13306 | ✅ |
| PostgreSQL | 134.175.232.110:15432 | ✅ |
| Kafka | 134.175.232.110:19092 | ✅ |

### 功能模块
| 模块 | 状态 |
|------|------|
| 文档上传 + 分块 + MD5 查重 | ✅ |
| RAG 检索（语义/关键词/混合/重排序） | ✅ |
| PgVectorStore 向量持久化 + HNSW 索引 | ✅ |
| zhparser 中文分词全文检索 | ✅ |
| Kafka 异步 Embedding + 兜底轮询 | ✅ |
| Agent 流式对话 + 打字机 + 多轮记忆 | ✅ |
| 查询改写（口语→关键词） | ✅ |
| Prompt 外置管理 + 热更新 | ✅ |
| 前端设计（暖色玻璃拟态） | ✅ |
| LaTeX 公式渲染 | ✅ |
| MCP Tool Calling（预留） | ⏳ 等 LangChain4j 支持流式 |
