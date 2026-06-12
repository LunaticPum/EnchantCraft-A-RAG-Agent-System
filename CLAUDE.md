# QA-Agent-Pumluda 项目开发指引

## 项目概述

复刻项目 [QA-Agent](D:\JavaProject\QA-Agent)，一个**技术面试训练工作台**。用户上传 Markdown 学习材料 → 系统自动分块、向量化 → 基于 LangChain4j Agent DAG 自动生成结构化问答集 → 支持练习、反馈、评分。

**目标：** 按 V1 → V2 → V3 → V4 → V5 → V6 版本迭代，逐步复刻原项目核心能力。当前在 V3 阶段。

**当前分支：** `v3-ADD-PostgreSQL`

## 技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | - |
| 框架 | Spring Boot | 3.4.3 | 父 POM |
| AI 框架 | LangChain4j BOM | 1.14.0 | 管理所有 langchain4j 模块版本 |
| AI 框架 | langchain4j-agentic | 1.14.0-beta24 | V3+ Agent DAG |
| 对话模型 | DeepSeek v4-pro | OpenAI 协议 | `OpenAiChatModel` |
| Embedding | 阿里云 DashScope text-embedding-v4 | 1024维 | `OpenAiEmbeddingModel`（OpenAI 兼容协议） |
| Rerank | 阿里云 DashScope gte-rerank-v2 | - | DashScope 原生 SDK |
| 向量存储 | PostgreSQL pgvector | pg17 | 自定义 `PgVectorStore` (JdbcTemplate) |
| ORM | MyBatis-Plus | 3.5.16 | MySQL 业务数据 |
| 数据库 | MySQL | 8.0+ | 远程 134.175.232.110:13306 |
| 数据库 | PostgreSQL + pgvector + zhparser | pg17 | 远程 134.175.232.110:15432 |
| 消息队列 | Kafka | 4.0 | 异步索引、消息追踪 |
| Markdown 解析 | flexmark | 0.64.8 | AST 按标题层级分块 |
| DashScope SDK | dashscope-sdk-java | 2.22.17 | Rerank 重排序 |
| JSON | fastjson2 | 2.0.61 | - |
| 工具 | Hutool | 5.8.36 | - |

## 项目结构（DDD 六模块）

```
QA-Agent-Pumluda/
├── QA-Agent-api/          API 契约 DTO（对外接口定义）
├── QA-Agent-types/        共享类型、常量、枚举、异常、Response 包装
├── QA-Agent-domain/       领域模型 + 领域服务接口 + 实现
├── QA-Agent-infrastructure/  DAO/PO/Repository 实现、PgVectorStore
├── QA-Agent-trigger/      HTTP Controller、Kafka Consumer、定时Job、全局异常处理
├── QA-Agent-app/          Spring Boot 启动入口 + 配置类 + 数据源
├── docs/
│   ├── dev-docs/V2/       V2 开发日志（V2.1~V2.5）
│   ├── dev-docs/V3/       V3 开发日志
│   ├── dev-ops/           Docker Compose + Dockerfile
│   └── sql/               建表/迁移 SQL
└── CLAUDE.md              本文件
```

### DDD 层依赖

```
app → trigger → api/types/domain
app → infrastructure → domain
trigger → api, types, domain
infrastructure → domain → types
```

### 与原项目 QA-Agent 的层映射

| 原项目 | Pumluda | 说明 |
|--------|---------|------|
| `qa-agent-types` | `QA-Agent-types` + `QA-Agent-api` | 共享类型放 types，API DTO 放 api |
| `qa-agent-domain` | `QA-Agent-domain` | 业务逻辑 + 领域服务 |
| `qa-agent-infrastructure` | `QA-Agent-infrastructure` | DAO/PO/Repository 实现 |
| `qa-agent-interfaces` | `QA-Agent-trigger` | Controller、Job、Listener |
| `qa-agent-application` | `QA-Agent-app` | 启动 + 配置 |

### 包名规范

基础包名：`cn.pumluda`

| 层 | 包名模式 |
|----|----------|
| domain | `cn.pumluda.domain.<bounded-context>.model.{entity,valobj,aggregate}` |
| domain service | `cn.pumluda.domain.<bounded-context>.service` |
| domain repository | `cn.pumluda.domain.<bounded-context>.adapter.repository` |
| domain producer | `cn.pumluda.domain.<bounded-context>.adapter.producer` |
| infrastructure DAO | `cn.pumluda.infrastructure.dao` |
| infrastructure PO | `cn.pumluda.infrastructure.dao.po` |
| infrastructure adapter | `cn.pumluda.infrastructure.adapter.repository` |
| trigger HTTP | `cn.pumluda.trigger.http` |
| trigger consumer | `cn.pumluda.trigger.consumer` |
| trigger job | `cn.pumluda.trigger.job` |
| app config | `cn.pumluda.config` |

## 版本规划

### 总体路线

| 大版本 | 核心能力 | 状态 |
|--------|----------|------|
| V1 | 框架搭建 + 依赖导入 + LangChain4j 配置 | ✅ 完成 |
| V2 | 文档上传 + Markdown 分块 + Embedding + RAG 检索 | ✅ 完成 |
| V3 | PostgreSQL pgvector 检索引擎 | ✅ 完成 |
| V4 | QA 生成 Agent DAG | ⏳ 下一步 |
| V5 | 反馈 + 评分 Agent | ⏳ |
| V6 | Memory & History | ⏳ |

### V2 完成情况

| 子版本 | 核心功能 | Git 提交 |
|--------|----------|----------|
| V2.1 | 文档上传 + MySQL + MyBatis-Plus | ✅ |
| V2.2 | flexmark AST 按标题分块 | ✅ |
| V2.3 | DashScope Embedding + 语义检索 | ✅ |
| V2.3.1 | 事务 + MD5 幂等 + 全局异常处理 | ✅ |
| V2.4 | 关键词检索 + RRF 多路融合混合检索 | ✅ |
| V2.5 | DashScope gte-rerank 重排序 | ✅ |

### V3 完成情况

| 子版本 | 核心功能 | Git 提交 |
|--------|----------|----------|
| V3.1 | PostgreSQL + pgvector + zhparser 部署 + chunk_search 建表 | ✅ |
| V3.2 | PgVectorStore 替换 InMemoryEmbeddingStore（JdbcTemplate + 手写 SQL） | ✅ |
| V3.3 | 中文分词全文检索（zhparser tsvector + GIN 替换 MySQL LIKE） | ✅ |
| V3.4 | Kafka 异步索引 + message_job 消息追踪 + 兜底轮询 | ✅ |

### V4 规划 —— QA 生成 Agent DAG

**目标：** 基于 LangChain4j Agentic 框架，实现文档→问答集的自动生成管线

待具体规划。

## 开发约束

### 依赖管理
- 所有依赖版本号统一在**父 POM `<properties>`** 中声明
- 子模块引用使用 `${property.name}`，禁止硬编码版本号
- 版本号优先对齐 QA-Agent 原项目

### Git 提交
- **Commit message 使用中文**
- 格式：`✨ feat: <描述>`
- 当前分支：`v3-ADD-PostgreSQL`（从 v2-Document 创建）

### 代码风格
- 所有 DDD 层可用 Spring 注解（`@Service`、`@Repository` 等）
- 服务接口命名：`IXxxService` / `IXxxRetriever` / `IXxxer` 等
- 实体字段必须加 `/** */` 注释
- DTO 转换方法按实体类型命名：`toXxxResponse()` / `toXxxPO()` / `toXxxEntity()`
- 多用 `@Slf4j` + `log.info("[模块] ...")` 标注执行状态
- Optional + `orElseThrow(() -> new AppException(...))` 模式
- 自定义业务异常统一通过 `ResponseCode` 枚举 + `AppException` 抛出
- API 版本显式写在参数注解：`@PathVariable("id")`、`@RequestParam("file")`

### API 约定
- 全局异常处理：`GlobalExceptionHandler` 拦截 `AppException` + `Exception`
- 响应统一用 `Response<T>` 包装（code + info + data）
- 中文文件名需 URL 解码：`URLDecoder.decode(rawFileName, StandardCharsets.UTF_8)`

### 开发日志
- 路径：`docs/dev-docs/<版本号>/`
- 风格：个人学习笔记，Obsidian 格式，多用 callout 和 mermaid 图表
- 调用链/流程图统一用 ````mermaid` 代码块

## DDD 架构红线（V3 踩坑总结）

以下是在 V3 开发中实际犯过的错误，以后禁止：

### 1. domain 层禁止直接引 infrastructure 层
- ❌ `DocumentServiceImpl` 注入 `MessageJobDao`（MyBatis-Plus DAO）
- ✅ `DocumentServiceImpl` 注入 `IMessageJobRepository`（domain 接口），infra 层实现
- ❌ `DocumentController` 注入 `MessageJobDao` 查状态
- ✅ Controller → `IDocumentService.getEmbeddingStatus()` → `IMessageJobRepository.getStatus()`

**原则：** domain 和 trigger 层只依赖接口，infrastructure 做具体实现。需要一个外部依赖时，先在 domain 定义接口（`adapter/repository/` 或 `adapter/producer/`），在 infra 实现。

### 2. 双数据源不要暴露两个 DataSource Bean
- ❌ 同时创建 `dataSource` 和 `postgresDataSource` 两个 Bean → Spring Boot 自动配置被跳过
- ❌ 两个都显式声明 + `@Primary` / `@Qualifier` 反复拉扯 → jdbcUrl 绑定失败
- ✅ MySQL 交给 Spring Boot 自动配置，PostgreSQL 在 `LangChain4jConfig` 内部 `new HikariDataSource()`，不暴露为 Bean

**原则：** 双数据源最简单的方式是只留一个自动配置，另一个完全手动管理，不注册为 Spring Bean。

### 3. LangChain4j API 版本一致性
- ❌ `EmbeddingStore.findRelevant()` — 1.14.0 已废弃，只有 `search(EmbeddingSearchRequest)`
- ❌ 引入不存在的 `langchain4j-pgvector` — 1.14.0 BOM 中没有这个模块
- ✅ 所有 SDK 调用前确认 properties 中声明的版本号，以该版本的 API 文档为准

**原则：** 写代码前以父 POM `<properties>` 中声明的版本为准，不用记忆中的 API，不确定就查原项目怎么写的。

### 4. Kafka 消费者参数类型
- ❌ `@KafkaListener` 方法参数用 `Map<String, Object>` — Spring Kafka 反序列化失败
- ✅ 用 `String` 接收原始 JSON → `JSON.parseObject()` 手动解析

### 5. EmbeddingService 只做 Embedding，不做业务编排
- `DocumentServiceImpl.uploadDocument()` 负责 ① ② 步（MySQL 事务内）
- `embedDocumentChunks()` 负责 ③ 步（异步，无事务）
- 拆分比一把梭更灵活

### 6. YAML 一个文件只能有一个同名根节点
- ❌ 两个 `spring:` 根节点 → `DuplicateKeyException`
- ✅ 所有 spring 配置合并到一个 `spring:` 块下

## 数据库表

### MySQL（业务数据）

| 表名 | 用途 | 版本 |
|------|------|------|
| `source_document` | 文档仓库，存上传的 Markdown 原文 + MD5 查重 | V2.1 |
| `document_chunk` | 分块文本，按标题层级切分后的语义块（业务真数据） | V2.2 |
| `message_job` | Kafka 异步索引消息追踪（PENDING/COMPLETED/FAILED） | V3.4 |

### PostgreSQL（RAG 检索引擎）

| 表名 | 用途 | 版本 |
|------|------|------|
| `chunk_search` | 向量 + 全文索引 + 标签（一表双引擎：HNSW + GIN） | V3.1 |

## 当前状态

### 远程服务
| 服务 | 地址 | 状态 |
|------|------|------|
| MySQL | 134.175.232.110:13306 | ✅ 运行中 |
| PostgreSQL | 134.175.232.110:15432 | ✅ 运行中（pgvector + zhparser） |
| Kafka | 134.175.232.110:19092 | ✅ 运行中 |

### V3 已完成
- [x] PostgreSQL + zhparser 容器部署（Dockerfile 编译 scws + zhparser）
- [x] chunk_search 表 + HNSW 向量索引 + GIN 全文索引 + GIN 标签索引
- [x] PgVectorStore（JdbcTemplate 手写）替换 InMemoryEmbeddingStore
- [x] 双数据源（MySQL 自动配置 + PostgreSQL `new HikariDataSource()` 内部创建）
- [x] zhparser 中文分词全文检索（ts_rank + to_tsquery + GIN）替代 MySQL LIKE
- [x] 上传 → Kafka 消息 → 异步 Embedding → message_job 追踪 → 兜底轮询
- [x] Re-embed API（ON CONFLICT DO UPDATE 自动覆写）
- [x] DDD 层间解耦（infra 实现的接口定义在 domain）

