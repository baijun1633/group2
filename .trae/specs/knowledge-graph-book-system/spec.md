# 基于知识图谱的个性化荐书系统 Spec

## Why
当前项目仅具备基础的CRUD框架（Spring Boot + MyBatis Plus），缺少用户认证、知识图谱、个性化推荐、社区互动等核心业务能力。需要将其从简单的图书管理后台，升级为一个面向数字化阅读场景的"知人知书"智慧阅读平台，通过知识图谱推理与协同过滤实现精准的个性化图书推荐，并提供完整的阅读生态闭环。

## What Changes
- 新增统一响应格式、全局异常处理、JWT认证体系
- 重构现有用户/书籍/分类模块以适配新接口规范（`/api/v1/`前缀）
- 新增用户注册/登录/Token刷新等认证流程
- 新增知识图谱模块（Neo4j集成、图谱构建、查询、可视化）
- 新增个性化推荐模块（ItemCF、图谱推理、混合推荐、推荐理由）
- 新增用户画像模块（行为采集、兴趣建模、画像管理）
- 新增书架管理模块（创建书架、图书归类、阅读状态）
- 新增阅读进度同步模块（跨设备进度同步、阅读历史）
- 新增图书试读模块（在线预览PDF/epub内容）
- 新增书评社区模块（发布、点赞、回复、审核）
- 新增图书评分模块（1-5星评分、评分统计）
- 新增购书链接模块（第三方平台链接聚合）
- 新增管理员后台模块（用户管理、统计、推荐配置、批量导入）
- 新增数据库表结构（用户认证信息、书架、书评、评分、阅读进度、推荐记录等）
- **BREAKING** 现有API路径从无前缀改为 `/api/v1/`，响应格式统一为 `{code, message, data}`
- **BREAKING** 用户表结构需扩展（增加preference_tags、avatar、role字段）
- **BREAKING** 书籍表结构需扩展（增加tags、avg_rating、rating_count、pages、series_info字段）

## Impact
- Affected specs: 全部（新建系统）
- Affected code:
  - `pom.xml` — 新增Spring Security、JWT、Neo4j、Redis等依赖
  - `application.yml` — 新增Neo4j、JWT、Redis等配置
  - `entity/` — 所有实体类需要扩展字段
  - `controller/` — 所有Controller需要重构为RESTful风格 + `/api/v1/`前缀
  - `service/` — 所有Service需要重写业务逻辑
  - `mapper/` — Mapper及XML映射文件需更新
  - `config/` — 新增Security配置、JWT工具类、Neo4j配置等
  - 新增 `common/` 包 — 统一响应体、异常处理、分页工具等
  - 新增 `security/` 包 — JWT过滤器、认证处理器等
  - 新增 `neo4j/` 包 — Neo4j Repository、知识图谱服务

## 分期规划

### 第一期：基础设施与认证系统
构建项目骨架：统一响应、全局异常、JWT认证、用户管理。
**目标**：完成用户注册/登录/Token刷新，所有API迁移至 `/api/v1/` 前缀。

### 第二期：图书核心功能
完善图书管理：搜索、详情、评分、购书链接、试读。
**目标**：用户可搜索浏览图书、查看详情、评分、查看购书链接。

### 第三期：用户互动与个性化基础
书架管理、阅读进度同步、用户画像。
**目标**：用户可管理书架、同步阅读进度、查看/更新兴趣画像。

### 第四期：社区功能
书评发布、点赞、回复、管理员审核。
**目标**：用户可发布书评、互动，管理员可审核。

### 第五期：知识图谱
Neo4j集成、图谱构建、查询、可视化。
**目标**：管理员可构建图谱，用户可查询和可视化展示。

### 第六期：推荐引擎
协同过滤、图谱推理推荐、混合策略、推荐理由。
**目标**：用户首页获得个性化推荐，每条推荐有可解释理由。

### 第七期：管理后台完善
统计面板、推荐配置、批量导入、用户管理。
**目标**：管理员可查看全局统计、配置推荐权重、批量管理。

---

## ADDED Requirements

### 第一期：基础设施与认证系统

#### Requirement: 统一响应格式
系统 SHALL 返回统一JSON响应结构 `{code: 0, message: "success", data: {}}`。

##### Scenario: 成功响应
- **WHEN** 任何API调用成功
- **THEN** 返回 `{"code": 0, "message": "success", "data": {...}}`

##### Scenario: 分页响应
- **WHEN** 调用列表类API
- **THEN** 返回 `{"code": 0, "message": "success", "data": {"items": [], "total": N, "page": P, "size": S}}`

##### Scenario: 错误响应
- **WHEN** 任何API调用失败
- **THEN** 返回对应错误码，如 `{"code": 1002, "message": "Token已过期", "data": null}`

#### Requirement: JWT认证体系
系统 SHALL 使用JWT Token进行用户认证，通过 `Authorization: Bearer <token>` 传递。

##### Scenario: 登录获取Token
- **WHEN** 用户使用正确用户名密码登录
- **THEN** 返回 accessToken（2小时有效）和 refreshToken（7天有效）

##### Scenario: Token过期刷新
- **WHEN** accessToken过期，使用refreshToken调用刷新接口
- **THEN** 返回新的 accessToken 和 refreshToken

##### Scenario: 未认证访问受保护接口
- **WHEN** 未携带或携带无效Token访问受保护接口
- **THEN** 返回 `{"code": 1001, "message": "未认证"}`

#### Requirement: 用户注册
系统 SHALL 提供用户注册功能，支持用户名、密码、昵称及偏好标签。

##### Scenario: 正常注册
- **WHEN** 用户提交不重复的用户名和匹配的密码确认
- **THEN** 创建用户，返回用户信息和Token

##### Scenario: 用户名重复
- **WHEN** 用户提交已存在的用户名
- **THEN** 返回 `{"code": 1003, "message": "用户名已存在"}`

#### Requirement: 用户登录
系统 SHALL 提供基于用户名+密码的登录。

##### Scenario: 正常登录
- **WHEN** 用户提交正确的用户名和密码
- **THEN** 返回用户信息和Token

##### Scenario: 密码错误
- **WHEN** 用户提交错误密码
- **THEN** 返回 `{"code": 1004, "message": "用户名或密码错误"}`

#### Requirement: API路径规范化
所有接口 SHALL 使用 `/api/v1/` 前缀，RESTful风格设计。

---

### 第二期：图书核心功能

#### Requirement: 图书多维度搜索
系统 SHALL 支持按关键词搜索图书（匹配书名、作者、ISBN）。

##### Scenario: 关键词搜索
- **WHEN** 用户搜索关键词 "三体"
- **THEN** 返回匹配书名/作者/ISBN的分页结果，每项包含 matchType

#### Requirement: 图书分类/标签筛选
系统 SHALL 支持按分类和标签筛选图书，支持排序（热度、评分、出版日期）。

#### Requirement: 图书详情
系统 SHALL 返回图书完整信息，包含评分、关联图书（图谱数据可后期补充）。

#### Requirement: 图书评分
系统 SHALL 支持1-5星整数评分，每用户每书仅一次，可更新。

##### Scenario: 首次评分
- **WHEN** 用户对未评分图书提交评分
- **THEN** 记录评分，更新图书平均分和评分人数

##### Scenario: 更新评分
- **WHEN** 用户对已评分图书重新评分
- **THEN** 更新评分，重新计算平均分

#### Requirement: 购书链接
系统 SHALL 返回图书在各电商平台的购买链接和价格。

---

### 第三期：用户互动与个性化

#### Requirement: 书架管理
系统 SHALL 支持用户创建多个书架（如"想读"、"在读"、"已读"），管理图书归类。

##### Scenario: 创建书架
- **WHEN** 用户提交书架名称和描述
- **THEN** 创建书架

##### Scenario: 添加图书到书架
- **WHEN** 用户向指定书架添加图书并设置阅读状态
- **THEN** 图书加入书架

##### Scenario: 移动图书
- **WHEN** 用户将图书从一个书架移动到另一个
- **THEN** 更新归属和阅读状态

#### Requirement: 阅读进度同步
系统 SHALL 支持记录和同步用户阅读进度（当前页、百分比、设备信息）。

##### Scenario: 同步进度
- **WHEN** 用户上报当前阅读进度
- **THEN** 记录进度和设备信息，更新时间

##### Scenario: 查询进度
- **WHEN** 用户查询某本书的阅读进度
- **THEN** 返回最新进度信息

##### Scenario: 阅读历史
- **WHEN** 用户查看阅读历史
- **THEN** 返回按更新时间倒序的分页列表

#### Requirement: 用户画像
系统 SHALL 维护用户兴趣画像（标签向量、偏好作者、偏好分类）。

##### Scenario: 查看画像
- **WHEN** 用户查看自己的画像
- **THEN** 返回标签权重、偏好作者、偏好分类

##### Scenario: 更新偏好标签
- **WHEN** 用户更新偏好标签
- **THEN** 更新画像

---

### 第四期：社区功能

#### Requirement: 书评发布
系统 SHALL 支持登录用户发布书评（支持Markdown），发布后进入待审核状态。

##### Scenario: 发布书评
- **WHEN** 用户提交书评内容
- **THEN** 创建书评，状态为 `pending_audit`

#### Requirement: 书评互动
系统 SHALL 支持书评点赞和回复。

##### Scenario: 点赞/取消点赞
- **WHEN** 用户对书评点赞（再次调用取消）
- **THEN** 切换点赞状态

##### Scenario: 回复书评
- **WHEN** 用户回复某条书评
- **THEN** 创建回复

#### Requirement: 书评审核
管理员 SHALL 可审核书评（通过/驳回/删除）。

---

### 第五期：知识图谱

#### Requirement: 知识图谱构建
系统 SHALL 支持管理员触发书籍知识图谱构建（基于Neo4j）。

##### Scenario: 触发构建
- **WHEN** 管理员指定书籍ID列表或全量构建
- **THEN** 异步执行构建任务，返回任务ID和预估时间

#### Requirement: 图谱查询
系统 SHALL 支持Cypher查询语言查询图谱关系。

#### Requirement: 图谱可视化
系统 SHALL 返回图谱节点和边的数据结构，供前端可视化渲染。

#### Requirement: 图谱关系编辑
管理员 SHALL 可手动添加/删除/修改图谱关系。

---

### 第六期：推荐引擎

#### Requirement: 首页推荐
系统 SHALL 为登录用户提供个性化推荐，未登录用户返回热门/新书兜底。

##### Scenario: 登录用户首页推荐
- **WHEN** 登录用户访问首页
- **THEN** 返回带匹配分数和推荐理由的图书列表

##### Scenario: 未登录用户首页推荐
- **WHEN** 未登录用户访问首页
- **THEN** 返回热门/新书列表（无理由）

#### Requirement: 相似读物
系统 SHALL 基于图书特征推荐相似读物。

#### Requirement: 延伸阅读
系统 SHALL 基于多跳路径推理（至少3跳）推荐间接关联的高质量图书。

#### Requirement: 推荐理由生成
每条推荐 SHALL 包含可解释的推荐理由和推理路径。

#### Requirement: 混合推荐策略
系统 SHALL 支持配置各推荐策略权重（知识图谱、协同过滤、热门、新书），权重之和为1。

---

### 第七期：管理后台

#### Requirement: 管理员用户管理
管理员 SHALL 可分页查询、新增、修改、删除用户。

#### Requirement: 批量导入图书
管理员 SHALL 可通过CSV/Excel批量导入图书，返回成功/失败统计。

#### Requirement: 系统统计
管理员 SHALL 可查看总用户数、总图书数、总书评数、日活、推荐点击率等统计数据。

#### Requirement: 推荐算法配置
管理员 SHALL 可配置推荐算法各策略权重。

#### Requirement: 图书试读
系统 SHALL 支持图书在线试读（PDF/epub格式），未登录返回前3页，登录返回前10页或第一章。

---

## MODIFIED Requirements

### Requirement: 用户实体
原有 `Users` 实体需扩展：增加 `role`(ADMIN/USER)、`preference_tags`(JSON)、`avatar`、`password_hash` 字段。`Users` 表需重构以支持JWT认证。

### Requirement: 书籍实体
原有 `Books` 实体需扩展：增加 `tags`(JSON)、`avg_rating`、`rating_count`、`pages`、`series_info`(JSON) 字段。

### Requirement: 借阅记录
原有 `BorrowRecords` 模块保持，但阅读相关功能迁移至新的阅读进度模块。

---

## REMOVED Requirements
无。现有功能保留并升级。
