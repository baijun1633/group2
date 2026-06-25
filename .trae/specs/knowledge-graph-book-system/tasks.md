# Tasks

> 总体分为7期，每期可独立交付。建议按期顺序逐步开发。

---

## 第一期：基础设施与认证系统

- [X] Task 1.1: 新增依赖 — 更新 `pom.xml`，添加 Spring Security、JWT (jjwt)、Redis 等依赖

  - [X] 1.1.1: 添加 `spring-boot-starter-security`
  - [X] 1.1.2: 添加 `jjwt-api`、`jjwt-impl`、`jjwt-jackson`
  - [X] 1.1.3: 添加 `spring-boot-starter-data-redis`（用于Token黑名单，可选）
  - [X] 1.1.4: 验证 Maven 依赖解析无冲突
- [X] Task 1.2: 创建通用基础组件 — `com.cqu.springboot.common` 包

  - [X] 1.2.1: 创建 `ApiResponse<T>` 统一响应体（code/message/data字段）
  - [ ] 1.2.2: 创建 `PageResponse<T>` 分页响应体（items/total/page/size字段）
  - [ ] 1.2.3: 创建 `ErrorCode` 错误码枚举（1001未认证、1002Token过期、1003用户名已存在、1004密码错误等）
  - [ ] 1.2.4: 创建 `BusinessException` 业务异常类
  - [ ] 1.2.5: 创建 `GlobalExceptionHandler` 全局异常处理器（@RestControllerAdvice）
  - [ ] 1.2.6: 创建 `PageParam` 分页请求参数封装类
- [x] Task 1.3: 创建JWT安全组件 — `com.cqu.springboot.security` 包

  - [ ] 1.3.1: 创建 `JwtProperties` 配置类（密钥、过期时间等，读取application.yml）
  - [ ] 1.3.2: 创建 `JwtTokenProvider` 工具类（生成/解析/验证 accessToken 和 refreshToken）
  - [ ] 1.3.3: 创建 `JwtAuthenticationFilter` 过滤器（从Header提取Token，设置SecurityContext）
  - [ ] 1.3.4: 创建 `SecurityConfig` 配置类（配置SecurityFilterChain，定义公开/受保护路径）
  - [ ] 1.3.5: 创建 `CustomUserDetailsService` 实现 UserDetailsService
  - [ ] 1.3.6: 在 `application.yml` 中添加 JWT 密钥和过期时间配置
- [x] Task 1.4: 重构 Users 表结构和实体

  - [ ] 1.4.1: 设计新 users 表结构SQL（增加 password_hash、role、preference_tags、avatar 字段）
  - [ ] 1.4.2: 更新 `Users.java` 实体类（对应新字段，role使用枚举）
  - [ ] 1.4.3: 更新 `UsersMapper.java` 及 `UsersMapper.xml`
  - [ ] 1.4.4: 创建 SQL 迁移脚本（ALTER TABLE 语句）
- [x] Task 1.5: 实现认证接口 — AuthController

  - [ ] 1.5.1: 创建 `AuthController.java`，路径 `/api/v1/auth`
  - [ ] 1.5.2: 实现 `POST /register` — 用户注册（参数校验、密码加密、返回Token）
  - [ ] 1.5.3: 实现 `POST /login` — 用户登录（验证密码、返回Token）
  - [ ] 1.5.4: 实现 `POST /refresh` — Token刷新（验证refreshToken、签发新Token对）
  - [ ] 1.5.5: 创建 `AuthService.java` 及实现类，封装认证业务逻辑
  - [ ] 1.5.6: 创建 DTO 类：`RegisterRequest`、`LoginRequest`、`RefreshRequest`、`AuthResponse`
- [x] Task 1.6: 实现用户信息接口 — UsersController 重构

  - [ ] 1.6.1: 重构 `UsersController.java`，路径改为 `/api/v1/users`
  - [ ] 1.6.2: 实现 `GET /me` — 获取当前用户信息
  - [ ] 1.6.3: 实现 `PUT /me/preferences` — 更新偏好标签
  - [ ] 1.6.4: 更新 `UsersService` 及实现类
- [x] Task 1.7: 验证第一期

  - [ ] 1.7.1: 启动项目验证无编译错误
  - [ ] 1.7.2: 使用 Postman/Knife4j 测试注册、登录、Token刷新流程
  - [ ] 1.7.3: 测试受保护接口 `GET /api/v1/users/me` 需要Token才能访问
  - [ ] 1.7.4: 测试过期Token和无效Token返回正确错误码

---

## 第二期：图书核心功能

- [x] Task 2.1: 重构书籍表结构和实体

  - [ ] 2.1.1: 设计新 books 表SQL（增加 tags、avg_rating、rating_count、pages、series_info 字段）
  - [ ] 2.1.2: 更新 `Books.java` 实体类
  - [ ] 2.1.3: 创建 SQL 迁移脚本
  - [ ] 2.1.4: 创建 `purchase_links` 表（book_id、platform、url、price）
- [x] Task 2.2: 重构 BooksController — 路径 `/api/v1/books`

  - [ ] 2.2.1: 重构 `BooksController.java`，路径改为 `/api/v1/books`
  - [ ] 2.2.2: 实现 `GET /search?keyword=&page=&size=` — 多维度搜索（匹配书名/作者/ISBN，返回matchType）
  - [ ] 2.2.3: 实现 `GET /?category=&tag=&sortBy=&order=&page=&size=` — 分类/标签筛选+排序
  - [ ] 2.2.4: 实现 `GET /{bookId}` — 图书详情（含评分、关联图书占位）
  - [ ] 2.2.5: 更新 `BooksService` 及实现类
  - [ ] 2.2.6: 更新 `BooksMapper.xml` 添加搜索和筛选SQL
- [x] Task 2.3: 实现图书评分功能

  - [ ] 2.3.1: 创建 `book_ratings` 表（user_id、book_id、score、create_time、update_time）
  - [ ] 2.3.2: 创建 `BookRatings` 实体、`BookRatingsMapper`
  - [ ] 2.3.3: 实现 `POST /api/v1/books/{bookId}/rating` — 提交评分（upsert逻辑）
  - [ ] 2.3.4: 实现 `GET /api/v1/books/{bookId}/rating` — 获取评分统计（平均分、分布）
  - [ ] 2.3.5: 提交评分后自动更新 books 表的 avg_rating 和 rating_count
- [x] Task 2.4: 实现购书链接功能

  - [ ] 2.4.1: 创建 `PurchaseLinks` 实体、`PurchaseLinksMapper`
  - [ ] 2.4.2: 实现 `GET /api/v1/books/{bookId}/purchase-links`
  - [ ] 2.4.3: 插入测试购书链接数据
- [x] Task 2.5: 实现管理员图书管理

  - [ ] 2.5.1: 创建 `AdminBooksController.java`，路径 `/api/v1/admin/books`
  - [ ] 2.5.2: 实现 `POST /` — 单本录入
  - [ ] 2.5.3: 实现 `PUT /{bookId}` — 编辑图书（部分更新）
  - [ ] 2.5.4: 实现 `DELETE /{bookId}` — 删除图书
  - [ ] 2.5.5: 实现 `POST /batch` — 批量导入（CSV解析，可第三期再完善）
- [x] Task 2.6: 验证第二期

  - [ ] 2.6.1: 测试图书搜索（关键词、分类、标签）
  - [ ] 2.6.2: 测试图书详情返回完整信息
  - [ ] 2.6.3: 测试评分提交和统计
  - [ ] 2.6.4: 测试购书链接查询

---

## 第三期：用户互动与个性化基础

- [ ] Task 3.1: 实现书架管理

  - [ ] 3.1.1: 创建 `shelves` 表（shelf_id、user_id、name、description、create_time）
  - [ ] 3.1.2: 创建 `shelf_books` 表（id、shelf_id、book_id、reading_status、add_time）
  - [ ] 3.1.3: 创建 `Shelves`、`ShelfBooks` 实体及对应 Mapper
  - [ ] 3.1.4: 创建 `ShelvesController.java`，路径 `/api/v1/shelves`
  - [ ] 3.1.5: 实现 `POST /` — 创建书架
  - [ ] 3.1.6: 实现 `GET /` — 获取书架列表（含图书数量）
  - [ ] 3.1.7: 实现 `POST /{shelfId}/books` — 添加图书到书架
  - [ ] 3.1.8: 实现 `PUT /{shelfId}/books/{bookId}` — 移动图书
  - [ ] 3.1.9: 实现 `DELETE /{shelfId}` — 删除书架
- [ ] Task 3.2: 实现阅读进度同步

  - [ ] 3.2.1: 创建 `reading_progress` 表（id、user_id、book_id、current_page、percentage、device、update_time）
  - [ ] 3.2.2: 创建 `ReadingProgress` 实体及 Mapper
  - [ ] 3.2.3: 创建 `ReadingController.java`，路径 `/api/v1/reading`
  - [ ] 3.2.4: 实现 `PUT /progress` — 同步阅读进度（upsert）
  - [ ] 3.2.5: 实现 `GET /progress/{bookId}` — 获取阅读进度
  - [ ] 3.2.6: 实现 `GET /history?page=&size=` — 获取阅读历史
- [ ] Task 3.3: 实现用户画像

  - [ ] 3.3.1: 创建 `user_profiles` 表（id、user_id、tag_vector JSON、preferred_authors JSON、preferred_categories JSON、last_updated）
  - [ ] 3.3.2: 创建 `UserProfile` 实体及 Mapper
  - [ ] 3.3.3: 在 `UsersController` 中新增 `GET /me/profile` — 获取画像
  - [ ] 3.3.4: 实现 `POST /me/profile/refresh` — 手动触发画像更新（先做基础版本：根据书架和评分计算标签权重）
  - [ ] 3.3.5: 实现画像自动更新服务（基础版：定时任务，根据用户行为计算兴趣标签）
- [ ] Task 3.4: 验证第三期

  - [ ] 3.4.1: 测试书架创建、图书添加、移动
  - [ ] 3.4.2: 测试阅读进度同步和查询
  - [ ] 3.4.3: 测试用户画像查询和更新

---

## 第四期：社区功能

- [ ] Task 4.1: 实现书评发布和列表

  - [ ] 4.1.1: 创建 `reviews` 表（review_id、book_id、user_id、content、markdown、status、likes_count、create_time、update_time）
  - [ ] 4.1.2: 创建 `review_replies` 表（reply_id、review_id、user_id、content、create_time）
  - [ ] 4.1.3: 创建 `review_likes` 表（id、review_id、user_id、create_time）
  - [ ] 4.1.4: 创建 `Reviews`、`ReviewReplies`、`ReviewLikes` 实体及 Mapper
  - [ ] 4.1.5: 创建 `ReviewsController.java`，路径 `/api/v1/books/{bookId}/reviews` 及 `/api/v1/reviews`
  - [ ] 4.1.6: 实现 `POST /api/v1/books/{bookId}/reviews` — 发布书评（状态pending_audit）
  - [ ] 4.1.7: 实现 `GET /api/v1/books/{bookId}/reviews?sortBy=&page=&size=` — 书评列表
  - [ ] 4.1.8: 实现 `DELETE /api/v1/reviews/{reviewId}` — 删除自己未审核书评
- [ ] Task 4.2: 实现书评互动

  - [ ] 4.2.1: 实现 `POST /api/v1/reviews/{reviewId}/like` — 点赞/取消点赞（toggle逻辑）
  - [ ] 4.2.2: 实现 `POST /api/v1/reviews/{reviewId}/reply` — 回复书评
- [ ] Task 4.3: 实现书评审核（管理员）

  - [ ] 4.3.1: 创建 `AdminReviewsController.java`，路径 `/api/v1/admin/reviews`
  - [ ] 4.3.2: 实现 `GET /pending?page=&size=` — 获取待审核书评
  - [ ] 4.3.3: 实现 `PUT /{reviewId}/audit` — 审核（approve/reject/delete）
- [ ] Task 4.4: 验证第四期

  - [ ] 4.4.1: 测试书评发布（含Markdown内容）
  - [ ] 4.4.2: 测试点赞和回复
  - [ ] 4.4.3: 测试管理员审核流程

---

## 第五期：知识图谱

- [ ] Task 5.1: 集成 Neo4j

  - [ ] 5.1.1: 添加 `spring-boot-starter-data-neo4j` 依赖到 pom.xml
  - [ ] 5.1.2: 在 `application.yml` 配置 Neo4j 连接信息
  - [ ] 5.1.3: 创建 Neo4j 配置类 `Neo4jConfig.java`
  - [ ] 5.1.4: 安装并启动本地 Neo4j 数据库（需要用户确认）
- [ ] Task 5.2: 定义知识图谱模型

  - [ ] 5.2.1: 定义图谱节点类型：Book、Author、Publisher、Category、Tag
  - [ ] 5.2.2: 定义关系类型：written_by、belongs_to、published_by、tagged_as、related_to
  - [ ] 5.2.3: 创建 Neo4j 节点实体类
  - [ ] 5.2.4: 创建 Neo4j Repository（Spring Data Neo4j）
- [ ] Task 5.3: 实现图谱构建

  - [ ] 5.3.1: 创建 `KnowledgeGraphService.java` — 图谱构建核心逻辑
  - [ ] 5.3.2: 实现从 MySQL 书籍数据提取实体和关系
  - [ ] 5.3.3: 实现写入 Neo4j 的逻辑
  - [ ] 5.3.4: 支持增量构建和全量重建（forceRebuild参数）
  - [ ] 5.3.5: 创建 `AdminKgController.java`，路径 `/api/v1/admin/kg`
  - [ ] 5.3.6: 实现 `POST /build` — 触发图谱构建（返回taskId，异步执行）
- [ ] Task 5.4: 实现图谱查询和可视化

  - [ ] 5.4.1: 创建 `KgController.java`，路径 `/api/v1/kg`
  - [ ] 5.4.2: 实现 `POST /query` — Cypher查询（需安全审查，防止注入）
  - [ ] 5.4.3: 实现 `GET /graph/{entityId}?depth=` — 图谱可视化数据（返回nodes和edges）
  - [ ] 5.4.4: 实现 `POST /admin/kg/relations` — 管理员手动编辑图谱关系
- [ ] Task 5.5: 验证第五期

  - [ ] 5.5.1: 测试图谱构建（指定书籍列表）
  - [ ] 5.5.2: 测试Cypher查询返回正确结果
  - [ ] 5.5.3: 测试可视化数据格式正确
  - [ ] 5.5.4: 测试手动编辑图谱关系

---

## 第六期：推荐引擎

- [ ] Task 6.1: 协同过滤推荐（ItemCF）

  - [ ] 6.1.1: 设计用户-图书评分矩阵（基于book_ratings表和bookshelf数据）
  - [ ] 6.1.2: 实现 ItemCF 算法（计算图书相似度矩阵）
  - [ ] 6.1.3: 实现基于相似度的推荐生成
  - [ ] 6.1.4: 创建 `CollaborativeFilteringService.java`
- [ ] Task 6.2: 知识图谱推理推荐

  - [ ] 6.2.1: 实现基于图谱路径的推荐（利用用户偏好标签→Tag→Book路径）
  - [ ] 6.2.2: 实现多跳推理（至少3跳：Book→Tag→Book→Author→Book）
  - [ ] 6.2.3: 实现推理路径记录（用于推荐理由生成）
  - [ ] 6.2.4: 创建 `KgRecommendService.java`
- [ ] Task 6.3: 混合推荐策略

  - [ ] 6.3.1: 创建 `RecommendConfig` 实体及管理接口（权重配置持久化）
  - [ ] 6.3.2: 实现多策略结果合并和加权排序
  - [ ] 6.3.3: 实现未登录用户兜底策略（热门/新书）
  - [ ] 6.3.4: 创建 `RecommendService.java` — 统一推荐入口
- [ ] Task 6.4: 推荐理由生成

  - [ ] 6.4.1: 实现基于推理路径的自然语言理由生成
  - [ ] 6.4.2: 实现 reasonPath 记录（节点ID列表）
  - [ ] 6.4.3: 创建推荐理由模板（如"因为你喜欢《X》，同属Y题材"）
- [ ] Task 6.5: 实现推荐接口

  - [ ] 6.5.1: 创建 `RecommendController.java`，路径 `/api/v1/recommend`
  - [ ] 6.5.2: 实现 `GET /home` — 首页推荐
  - [ ] 6.5.3: 实现 `GET /hot?days=&limit=` — 热门推荐
  - [ ] 6.5.4: 实现 `GET /new?months=&limit=` — 新书推荐
  - [ ] 6.5.5: 实现 `GET /{recId}/explain` — 推荐解释详情
  - [ ] 6.5.6: 在 `BooksController` 中新增 `GET /{bookId}/similar` — 相似读物
  - [ ] 6.5.7: 在 `BooksController` 中新增 `GET /{bookId}/extended` — 延伸阅读
- [ ] Task 6.6: 验证第六期

  - [ ] 6.6.1: 测试首页推荐（登录 vs 未登录）
  - [ ] 6.6.2: 测试推荐理由包含正确推理路径
  - [ ] 6.6.3: 测试相似读物和延伸阅读
  - [ ] 6.6.4: 测试权重配置生效

---

## 第七期：管理后台完善

- [ ] Task 7.1: 管理员用户管理

  - [ ] 7.1.1: 创建 `AdminController.java`，路径 `/api/v1/admin`
  - [ ] 7.1.2: 实现 `GET /users` — 分页查询用户列表
  - [ ] 7.1.3: 实现 `POST /users` — 新增用户（可设置角色）
  - [ ] 7.1.4: 实现 `PUT /users/{userId}` — 修改用户信息
  - [ ] 7.1.5: 实现 `DELETE /users/{userId}` — 删除用户
- [ ] Task 7.2: 批量导入图书

  - [ ] 7.2.1: 实现CSV解析逻辑（Apache Commons CSV 或 EasyExcel）
  - [ ] 7.2.2: 实现 `POST /api/v1/admin/books/batch` — 批量导入
  - [ ] 7.2.3: 返回成功/失败统计及错误详情
- [ ] Task 7.3: 系统统计

  - [ ] 7.3.1: 实现 `GET /api/v1/admin/stats` — 统计数据查询
  - [ ] 7.3.2: 统计总用户数、总图书数、总书评数
  - [ ] 7.3.3: 统计日活用户、推荐点击率
  - [ ] 7.3.4: 统计热门分类排行
- [ ] Task 7.4: 推荐算法配置

  - [ ] 7.4.1: 实现 `PUT /api/v1/admin/recommend/config` — 权重配置
  - [ ] 7.4.2: 校验权重之和等于1
- [ ] Task 7.5: 图书试读功能

  - [ ] 7.5.1: 设计试读文件存储方案（static/ebooks/目录或OSS）
  - [ ] 7.5.2: 实现 `GET /api/v1/books/{bookId}/preview` — 获取试读内容
  - [ ] 7.5.3: 区分登录/未登录用户的试读页数限制
- [ ] Task 7.6: 验证第七期

  - [ ] 7.6.1: 测试管理员CRUD用户
  - [ ] 7.6.2: 测试批量导入
  - [ ] 7.6.3: 测试统计数据
  - [ ] 7.6.4: 测试推荐权重配置
  - [ ] 7.6.5: 测试图书试读

---

## Task Dependencies

- [Task 1.2] 依赖 [Task 1.1]
- [Task 1.3] 依赖 [Task 1.2]
- [Task 1.5] 依赖 [Task 1.3, Task 1.4]
- [Task 1.6] 依赖 [Task 1.5]
- [Task 2.2] 依赖 [Task 1.2] (需要统一响应格式)
- [Task 2.3] 依赖 [Task 1.5] (需要认证系统)
- [Task 3.1] 依赖 [Task 1.5]
- [Task 3.2] 依赖 [Task 1.5]
- [Task 3.3] 依赖 [Task 1.5, Task 2.3] (画像需要评分数据)
- [Task 4.1] 依赖 [Task 1.5]
- [Task 5.1] 依赖 [Task 1.1] (新增依赖)
- [Task 5.3] 依赖 [Task 5.1, Task 5.2]
- [Task 6.1] 依赖 [Task 2.3] (ItemCF需要评分数据)
- [Task 6.2] 依赖 [Task 5.3] (图谱推理需要图谱数据)
- [Task 6.3] 依赖 [Task 6.1, Task 6.2]
- [Task 7.2] 依赖 [Task 2.5] (批量导入基础)
