# QA-Agent-Pumluda 项目开发指引

## 项目概述

复刻项目 [QA-Agent](D:\JavaProject\QA-Agent)，一个**技术面试训练工作台**。用户上传 Markdown 学习材料 → 系统自动分块、向量化 → 基于 LangChain4j Agent DAG 自动生成结构化问答集 → 支持练习、反馈、评分。

**目标：** 按 V1 → V2 → V3 → V4 → V5 → V6 版本迭代，逐步复刻原项目核心能力。当前在 V2 阶段。

## 技术栈

| 类别 | 技术 | 版本 | 来源 |
|------|------|------|------|
| 语言 | Java | 17 | - |
| 框架 | Spring Boot | 3.4.3 | 当前 POM |
| AI 框架 | LangChain4j BOM | 1.14.0 | 对齐 QA-Agent |
| AI 模块 | langchain4j-open-ai | 1.14.0 | DeepSeek 兼容 OpenAI 协议 |
| AI 模块 | langchain4j-agentic | 1.14.0-beta24 | V3 Agent DAG |
| 对话模型 | DeepSeek (OpenAiChatModel) | - | 已配置 |
| Embedding | DeepSeek (OpenAiEmbeddingModel) | - | 已配置，向量维度待确认 |
| 向量存储 | InMemoryEmbeddingStore | - | V2 临时方案，后续升级 pgvector |
| ORM | MyBatis-Plus | 3.5.16 | 对齐 QA-Agent |
| 数据库 | MySQL | 8.0+ | 业务数据 |
| 数据库 | PostgreSQL + pgvector | - | V3+ 向量检索 |
| Markdown 解析 | flexmark | 0.64.8 | 对齐 QA-Agent |
| JSON | fastjson2 | 2.0.61 | 对齐 QA-Agent |
| JWT | jjwt | 0.12.6 | 后续版本启用 |
| 工具 | Hutool | 5.8.36 | 对齐 QA-Agent |
| 测试 | JUnit + Spring Boot Test | - | API 测试验证 |

**模型配置（application-dev.yml）：**
- Chat Model: `deepSeek.api-key`, `deepSeek.base-url`, `deepSeek.model`
- Embedding Model: 同 API key + base URL，独立 model 名

## 项目结构（DDD 六模块）

```
QA-Agent-Pumluda/
├── QA-Agent-api/          API 契约 DTO（对外接口定义）
├── QA-Agent-types/        共享类型、常量、枚举、异常、Response 包装
├── QA-Agent-domain/       领域模型 + 领域服务接口
├── QA-Agent-infrastructure/  DAO/PO/Repository 实现、技术适配
├── QA-Agent-trigger/      HTTP Controller、Job、Listener（入口层）
├── QA-Agent-app/          Spring Boot 启动入口 + 配置类
├── docs/                  设计文档（用户手动编写）
└── CLAUDE.md              本文件
```

### DDD 层依赖关系

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
| infrastructure DAO | `cn.pumluda.infrastructure.dao` |
| infrastructure PO | `cn.pumluda.infrastructure.dao.po` |
| infrastructure adapter | `cn.pumluda.infrastructure.adapter.repository` |
| trigger HTTP | `cn.pumluda.trigger.http` |
| app config | `cn.pumluda.config` |

## 版本规划

### 总体路线

| 大版本 | 核心能力 | 状态 |
|--------|----------|------|
| V1 | 框架搭建 + 依赖导入 + LangChain4j 配置 | ✅ 基本完成 |
| V2 | 文档上传 + Markdown 分块 + Embedding + 检索 + 数据库 | 🚧 当前阶段 |
| V3 | QA 生成 Agent DAG（DECIDE→PLAN→WRITE→VALIDATE→SUMMARIZE） | ⏳ |
| V4 | 反馈 Agent DAG | ⏳ |
| V5 | 评分 Agent DAG | ⏳ |
| V6 | Memory & History | ⏳ |

### V2 子版本规划

| 子版本 | 核心功能 | 关键交付 |
|--------|----------|----------|
| V2.1 | 文档上传 + MySQL | source_document 表 + 上传/查询 API + MyBatis-Plus 配置 |
| V2.2 | Markdown 分块 | flexmark AST 按标题层级切分 + document_chunk 表 |
| V2.3 | Embedding + 检索 | DeepSeek Embedding 向量化 + 语义检索 API |
| V2.4 | 混合检索（可选） | 关键词 + 语义 + RRF 融合 |

## 开发约束

### 依赖管理
- 所有依赖版本号统一在**父 POM `<properties>`** 中声明
- 子模块引用使用 `${property.name}`，禁止硬编码版本号
- 版本号对齐 QA-Agent 原项目（`D:\JavaProject\QA-Agent\backend\pom.xml`）

### Git 提交
- **Commit message 使用中文**
- 格式：`<gitmoji> <类型>: <描述>`
- 示例：`✨ feat: 添加文档上传API和source_document表`
- 分支：`dev/v1-my-implementation`（当前开发分支）

### 代码风格
- 尽量仿照 QA-Agent 原项目代码结构
- DDD 层职责清晰，禁止跨层调用
- 领域服务放在 domain 层，用接口+实现模式
- Controller 尽量薄，只做参数转换和调用 domain 服务

### 开发流程
1. 每个子版本功能完成后，通过 API 测试验证
2. 前端暂不开发，通过 Postman/curl 测试 API
3. 用户认证暂缓，后续版本再做
4. 设计文档由用户手动编写到 `docs/` 目录，完成后让 AI 检查和优化

### 已废弃内容（忽略即可）
- Vault 扫描配置（VaultProperties）和相关代码 — 已废弃，改用上传 API 模式
- Obsidian 本地路径相关配置 — 不生效

## 数据库表设计

### V2 表

| 表名 | 用途 | 版本 |
|------|------|------|
| `source_document` | 文档仓库，存上传的 Markdown 原文 | V2.1 |
| `document_chunk` | 分块文本，按标题层级切分后的语义块 | V2.2 |

### source_document 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(36) PK | UUID |
| file_name | VARCHAR(255) | 上传时的原始文件名 |
| file_type | VARCHAR(20) | 文件类型，默认 MARKDOWN |
| raw_content | MEDIUMTEXT | 文档完整原始文本 |
| ref_count | INT | 被 QA 集引用次数 |
| is_deleted | TINYINT | 软删除标记 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### document_chunk 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(36) PK | UUID |
| document_id | VARCHAR(36) FK | 关联 source_document.id |
| chunk_index | INT | 块在文档中的序号 |
| title_path | VARCHAR(500) | 标题路径，如 "Java基础>集合>HashMap" |
| content | MEDIUMTEXT | 分块文本内容 |
| module_tags | VARCHAR(500) | 模块标签 JSON 数组 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## 当前状态

### 已完成（V1）
- [x] DDD 六模块骨架搭建
- [x] LangChain4j 依赖配置
- [x] DeepSeek ChatModel Bean
- [x] DeepSeek EmbeddingModel Bean
- [x] InMemoryEmbeddingStore Bean
- [x] SSE 聊天测试端点 `POST /api/v1/chat`
- [x] 多环境配置（dev/test/prod）
- [x] Logback 日志配置

### 下一步（V2.1）
- [ ] 父 POM 声明 MyBatis-Plus、MySQL 驱动版本
- [ ] MySQL DataSource 配置
- [ ] MyBatis-Plus 配置类
- [ ] source_document 建表 SQL
- [ ] SourceDocumentPO + SourceDocumentDao
- [ ] DocumentService 领域服务
- [ ] DocumentController (上传 API + 查询 API)
