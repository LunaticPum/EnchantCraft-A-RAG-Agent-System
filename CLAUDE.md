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
| 数据库 | PostgreSQL + pgvector + zhparser | pg17 | 本地 Docker 15432 |
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
├── QA-Agent-trigger/      HTTP Controller、全局异常处理
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

## 版本规划

### 总体路线

| 大版本 | 核心能力 | 状态 |
|--------|----------|------|
| V1 | 框架搭建 + 依赖导入 + LangChain4j 配置 | ✅ 完成 |
| V2 | 文档上传 + Markdown 分块 + Embedding + RAG 检索 | ✅ 完成 |
| V3 | PostgreSQL pgvector 检索引擎 | 🚧 当前阶段 |
| V4 | QA 生成 Agent DAG | ⏳ |
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

### V3 子版本规划

| 子版本 | 核心功能 | 状态 |
|--------|----------|------|
| V3.1 | PostgreSQL + pgvector + zhparser 部署 + chunk_search 建表 | ✅ 完成 |
| V3.2 | PgVectorStore 替换 InMemoryEmbeddingStore（JdbcTemplate + 手写 SQL） | ✅ 完成 |
| V3.3 | 中文分词全文检索（zhparser tsvector 替换 MySQL LIKE） | ⏳ 下一步 |
| V3.4 | 数据重建 + 增量同步 | ⏳ |

### V3.3 规划 —— 中文分词全文检索

**目标：** 关键词检索从 MySQL `LIKE '%keyword%'` 升级为 PostgreSQL `tsvector + zhparser + GIN` 索引检索

**当前问题：**
- `KeywordRetrieverImpl` 走 MySQL `LIKE`，全表扫，无索引
- 关键字段 `content_tsv`（tsvector 列）和 `module_tags`（JSONB 列）已建但未填充
- zhparser 中文分词扩展已编译部署（Dockerfile），`chinese` 分词配置已建

**要做的事：**
1. `PgVectorStore.addAll()` 插入时：`to_tsvector('chinese', content)` 写入 `content_tsv`
2. 新增 `IDocumentChunkRepository.findByFullText(query, limit)` → PostgreSQL `ts_rank + tsquery`
3. `KeywordRetrieverImpl` 切换为调用 PostgreSQL 全文检索（复用 `postgresDataSource`）
4. 验证：搜索"存储引擎"和"引擎存储"结果不同（分词生效），搜索速度对比

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

## 数据库表

### MySQL（业务数据）

| 表名 | 用途 | 版本 |
|------|------|------|
| `source_document` | 文档仓库 | V2.1 |
| `document_chunk` | 分块文本 | V2.2 |

### PostgreSQL（RAG 检索引擎）

| 表名 | 用途 | 版本 |
|------|------|------|
| `chunk_search` | 向量 + 全文索引（embedding / content_tsv / module_tags） | V3.1 |

## 当前状态

### 远程服务
| 服务 | 地址 | 状态 |
|------|------|------|
| MySQL | 134.175.232.110:13306 | ✅ 运行中 |
| PostgreSQL | 134.175.232.110:15432 | ✅ 运行中（pgvector + zhparser） |

### V3.2 已完成
- [x] PostgreSQL + zhparser 容器部署
- [x] chunk_search 建表 + V3.2 迁移
- [x] PgVectorStore（JdbcTemplate 手写）替换 InMemoryEmbeddingStore
- [x] 上传文档 → Embedding → PostgreSQL 持久化
- [x] 双数据源（MySQL 自动配置 + PostgreSQL 内部创建，不冲突）

### V3.2 待优化
- [ ] `module_tags` 字段写入已验证（需重新上传文档生效）
- [ ] `content_tsv` 字段待 V3.3 zhparser 全文检索填充
