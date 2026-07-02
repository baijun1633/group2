# 图书管理系统 (Library Management System)

基于 Spring Boot 3.2.5 + MyBatis Plus 3.5.7 + Neo4j 开发的图书管理系统后端服务，支持用户认证、图书管理、推荐引擎、知识图谱等功能。

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| 持久层 | MyBatis Plus | 3.5.7 |
| 关系数据库 | MySQL | 8.0+ |
| 图数据库 | Neo4j | 5.x |
| 连接池 | Druid | 1.2.27 |
| API文档 | Knife4j (Swagger) | 4.3.0 |
| 安全框架 | Spring Security | 6.2.x |
| JWT | jjwt | 0.12.x |
| Java版本 | JDK | 17 |

## 项目架构

本项目采用经典的分层架构设计，遵循 MVC 模式，并集成了 Neo4j 图数据库支持：

```
┌─────────────────────────────────────────────────────────────────────┐
│                       前端/客户端                                    │
└─────────────────────────┬───────────────────────────────────────────┘
                          │ HTTP Request
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Controller 层 (控制器)                                  │
│         接收请求，参数校验，调用 Service，返回响应                      │
│  ┌─────────────┬─────────────┐                                      │
│  │  用户端API  │  管理端API   │                                      │
│  └─────────────┴─────────────┘                                      │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Service 层 (服务层)                                     │
│     接口定义: XxxService.java                                        │
│     实现类:   XxxServiceImpl.java                                    │
│         业务逻辑处理，事务管理                                        │
│  ┌─────────────┬─────────────┬─────────────┐                        │
│  │  业务服务   │  推荐引擎    │  知识图谱    │                        │
│  └─────────────┴─────────────┴─────────────┘                        │
└─────────────────────────┬───────────────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   Mapper 层     │ │  Repository层   │ │   安全层        │
│  (MySQL CRUD)   │ │  (Neo4j操作)    │ │  (JWT/Security) │
└─────────────────┘ └─────────────────┘ └─────────────────┘
          │               │
          ▼               ▼
┌─────────────────┐ ┌─────────────────┐
│   MySQL         │ │   Neo4j         │
│   library_db    │ │   Graph DB      │
└─────────────────┘ └─────────────────┘
```

### 分层职责说明

| 层级 | 职责 | 示例文件 |
|------|------|----------|
| Controller | 接收HTTP请求，参数校验，调用Service，返回JSON | `BooksController.java` |
| Service | 定义业务接口 | `BooksService.java` |
| ServiceImpl | 实现业务逻辑，事务管理 | `BooksServiceImpl.java` |
| Mapper | MySQL CRUD操作（继承BaseMapper） | `BooksMapper.java` |
| Repository | Neo4j 图数据库操作 | `BookNodeRepository.java` |
| Entity | MySQL实体类，映射数据库表结构 | `Books.java` |
| kg.Entity | Neo4j节点实体 | `BookNode.java` |
| DTO | 数据传输对象 | `LoginRequest.java`, `AuthResponse.java` |
| Security | 认证授权相关 | `JwtTokenProvider.java`, `SecurityConfig.java` |
| Config | 配置类 | `FileConfig.java`, `Neo4jConfig.java` |

## 模块介绍

### 用户端模块

| 模块 | 说明 | Controller |
|------|------|------------|
| 认证模块 | 用户注册、登录、Token刷新、退出、二级密码 | `AuthController.java` |
| 图书管理 | 图书列表、详情、搜索、评分 | `BooksController.java`, `RatingController.java` |
| 分类管理 | 图书分类列表、层级管理 | `CategoriesController.java` |
| 用户管理 | 用户信息、阅读进度、购买记录 | `UsersController.java`, `ReadingController.java`, `PurchaseRecordsController.java` |
| 书架管理 | 个人书架、收藏书籍 | `ShelvesController.java` |
| 书评管理 | 书评发表、点赞、回复 | `ReviewsController.java` |
| 推荐引擎 | 热门推荐、个性化推荐、协同过滤 | `RecommendController.java`, `RecommendationsController.java` |
| 知识图谱 | KG查询、实体浏览、关系展示 | `KgController.java` |
| 用户行为 | 浏览记录、行为统计 | `UserBehaviorsController.java` |
| 首页 | 轮播图、首页统计 | `BannersController.java` |

### 管理端模块

| 模块 | 说明 | Controller |
|------|------|------------|
| 图书管理 | 图书CRUD、批量导入、封面管理 | `AdminBooksController.java` |
| 用户管理 | 用户CRUD、权限管理 | `AdminUsersController.java` |
| 书评审核 | 书评审核、管理 | `AdminReviewsController.java` |
| 系统统计 | 数据统计、报表 | `AdminStatsController.java` |
| 推荐配置 | 推荐算法配置 | `AdminRecommendController.java` |
| 购书链接 | 购书链接管理 | `AdminPurchaseLinksController.java` |
| 知识图谱 | KG构建、同步管理 | `AdminKgController.java` |
| 定时任务 | 任务管理、执行日志 | `AdminTasksController.java` |

### 核心服务

| 服务 | 说明 |
|------|------|
| `AuthService` | 认证服务（登录、注册、Token管理） |
| `RecommendService` | 推荐服务（热门、个性化、协同过滤） |
| `KnowledgeGraphService` | 知识图谱服务（构建、查询、同步） |
| `UserBehaviorService` | 用户行为分析服务 |
| `StatsService` | 统计服务 |
| `ScheduledTaskService` | 定时任务服务 |

## 数据库设计

### MySQL 数据库

数据库名: `library_db`

**核心表结构**

| 表名 | 说明 |
|------|------|
| `books` | 书籍表 |
| `categories` | 分类表 |
| `users` | 用户表 |
| `user_profile` | 用户资料表 |
| `book_ratings` | 书籍评分表 |
| `reviews` | 书评表 |
| `review_replies` | 书评回复表 |
| `review_likes` | 书评点赞表 |
| `shelves` | 书架表 |
| `shelf_books` | 书架书籍关联表 |
| `reading_progress` | 阅读进度表 |
| `purchase_records` | 购买记录表 |
| `purchase_links` | 购书链接表 |
| `recommendation` | 推荐结果表 |
| `book_similarity` | 书籍相似度表 |
| `user_behavior` | 用户行为表 |
| `recommend_config` | 推荐配置表 |
| `kg_sync_log` | KG同步日志表 |
| `user_kg_metadata` | 用户KG元数据表 |

**books 表结构**

| 字段 | 类型 | 说明 |
|------|------|------|
| book_id | BIGINT | 主键，自增 |
| title | VARCHAR(255) | 书名 |
| author | VARCHAR(100) | 作者 |
| isbn | VARCHAR(20) | ISBN号 |
| publisher | VARCHAR(100) | 出版社 |
| publish_date | DATE | 出版日期 |
| category_id | BIGINT | 分类ID |
| price | DECIMAL(10,2) | 价格 |
| stock | INT | 库存数量 |
| pages | INT | 页数 |
| tags | TEXT | 标签（JSON数组） |
| avg_rating | DECIMAL(3,2) | 平均评分 |
| rating_count | INT | 评分数量 |
| view_count | INT | 浏览次数 |
| collect_count | INT | 收藏次数 |
| series_info | VARCHAR(255) | 系列信息 |
| description | TEXT | 书籍简介 |
| preview_content | TEXT | 预览内容 |
| cover_image | VARCHAR(500) | 封面图片URL |
| ebook_url | VARCHAR(500) | 电子书URL |
| ebook_type | VARCHAR(20) | 电子书类型 |
| ebook_size | INT | 电子书大小 |
| status | TINYINT | 状态: 0-下架, 1-上架 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### Neo4j 图数据库

**节点类型**

| 节点标签 | 说明 | 属性 |
|----------|------|------|
| `Book` | 书籍节点 | bookId, title, author, coverImage, avgRating |
| `Author` | 作者节点 | authorId, name |
| `Category` | 分类节点 | categoryId, name |
| `Publisher` | 出版社节点 | publisherId, name |
| `Tag` | 标签节点 | tagId, name |

**关系类型**

| 关系 | 说明 |
|------|------|
| `WRITTEN_BY` | 书籍-作者关系 |
| `BELONGS_TO` | 书籍-分类关系 |
| `PUBLISHED_BY` | 书籍-出版社关系 |
| `HAS_TAG` | 书籍-标签关系 |
| `SIMILAR_TO` | 书籍-书籍相似关系 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Neo4j 5.x（可选，知识图谱功能需要）

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd library_sys
   ```

2. **创建数据库**
   ```sql
   CREATE DATABASE library_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```

3. **修改配置**
   
   编辑 `src/main/resources/application.yml`：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/library_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
       username: your_username
       password: your_password
     neo4j:
       uri: bolt://localhost:7687
       authentication:
         username: neo4j
         password: your_password
   ```

4. **运行项目**
   ```bash
   mvn spring-boot:run
   ```

5. **访问应用**
   - 应用地址: http://localhost:8080
   - API文档: http://localhost:8080/doc.html (Knife4j)
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Druid监控: http://localhost:8080/druid/ (admin/admin)

## API 接口总览

### 认证模块

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/v1/auth/register` | POST | 用户注册 | 公开 |
| `/api/v1/auth/login` | POST | 用户登录 | 公开 |
| `/api/v1/auth/refresh` | POST | 刷新Token | 需Token |
| `/api/v1/auth/logout` | POST | 用户退出 | 需Token |
| `/api/v1/auth/second-password` | POST | 设置/验证二级密码 | 需Token |

### 用户端接口

| 模块 | 基础路径 | 说明 |
|------|----------|------|
| 图书 | `/api/v1/books` | 图书列表、详情、搜索 |
| 分类 | `/api/v1/categories` | 分类列表 |
| 用户 | `/api/v1/users` | 用户信息、阅读进度 |
| 评分 | `/api/v1/ratings` | 书籍评分 |
| 书评 | `/api/v1/reviews` | 书评管理 |
| 书架 | `/api/v1/shelves` | 书架管理 |
| 阅读 | `/api/v1/reading` | 阅读进度、电子书 |
| 购买记录 | `/api/v1/purchase-records` | 购买记录 |
| 推荐 | `/api/v1/recommend` | 热门、个性化推荐 |
| 推荐列表 | `/api/v1/recommendations` | 用户推荐列表 |
| 用户行为 | `/api/v1/user-behaviors` | 浏览记录 |
| 知识图谱 | `/api/v1/kg` | KG查询、实体 |
| 首页 | `/api/v1/banners` | 轮播图 |

### 管理端接口

| 模块 | 基础路径 | 说明 |
|------|----------|------|
| 图书管理 | `/api/v1/admin/books` | 图书CRUD、批量导入 |
| 用户管理 | `/api/v1/admin/users` | 用户CRUD |
| 书评审核 | `/api/v1/admin/reviews` | 书评审核 |
| 系统统计 | `/api/v1/admin/stats` | 数据统计 |
| 推荐配置 | `/api/v1/admin/recommend` | 推荐配置 |
| 购书链接 | `/api/v1/admin/purchase-links` | 购书链接管理 |
| 知识图谱 | `/api/v1/admin/kg` | KG构建、同步 |
| 定时任务 | `/api/v1/admin/tasks` | 任务管理 |

### 完整API文档

- **Knife4j 在线文档**: 启动项目后访问 http://localhost:8080/doc.html
- **离线接口文档**: 查看项目根目录下的 [API接口文档.md](API接口文档.md)（包含86个接口的详细说明）

## 项目结构

```
library_sys/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cqu/springboot/
│   │   │       ├── config/              # 配置类
│   │   │       │   ├── CorsConfig.java
│   │   │       │   ├── DruidConfig.java
│   │   │       │   ├── FileConfig.java
│   │   │       │   ├── MyBatisPlusConfig.java
│   │   │       │   ├── Neo4jConfig.java
│   │   │       │   ├── SwaggerConfig.java
│   │   │       │   └── WebMvcConfig.java
│   │   │       ├── controller/          # 控制器层
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── BooksController.java
│   │   │       │   ├── CategoriesController.java
│   │   │       │   ├── UsersController.java
│   │   │       │   ├── RatingController.java
│   │   │       │   ├── ReviewsController.java
│   │   │       │   ├── ShelvesController.java
│   │   │       │   ├── ReadingController.java
│   │   │       │   ├── PurchaseRecordsController.java
│   │   │       │   ├── RecommendController.java
│   │   │       │   ├── RecommendationsController.java
│   │   │       │   ├── UserBehaviorsController.java
│   │   │       │   ├── KgController.java
│   │   │       │   ├── BannersController.java
│   │   │       │   ├── AdminBooksController.java       # 管理端-图书管理
│   │   │       │   ├── AdminUsersController.java       # 管理端-用户管理
│   │   │       │   ├── AdminReviewsController.java     # 管理端-书评审核
│   │   │       │   ├── AdminStatsController.java       # 管理端-系统统计
│   │   │       │   ├── AdminRecommendController.java   # 管理端-推荐配置
│   │   │       │   ├── AdminPurchaseLinksController.java # 管理端-购书链接
│   │   │       │   ├── AdminKgController.java          # 管理端-知识图谱
│   │   │       │   └── AdminTasksController.java       # 管理端-定时任务
│   │   │       ├── service/             # Service层
│   │   │       │   ├── impl/            # Service实现类
│   │   │       │   │   ├── AuthServiceImpl.java
│   │   │       │   │   ├── BooksServiceImpl.java
│   │   │       │   │   ├── CategoriesServiceImpl.java
│   │   │       │   │   ├── UsersServiceImpl.java
│   │   │       │   │   ├── RatingServiceImpl.java
│   │   │       │   │   ├── ReviewsServiceImpl.java
│   │   │       │   │   ├── ShelvesServiceImpl.java
│   │   │       │   │   ├── ReadingProgressServiceImpl.java
│   │   │       │   │   ├── PurchaseRecordServiceImpl.java
│   │   │       │   │   ├── RecommendServiceImpl.java
│   │   │       │   │   ├── RecommendationServiceImpl.java
│   │   │       │   │   ├── UserBehaviorServiceImpl.java
│   │   │       │   │   ├── KnowledgeGraphServiceImpl.java
│   │   │       │   │   ├── KgRecommendServiceImpl.java
│   │   │       │   │   ├── CollaborativeFilteringServiceImpl.java
│   │   │       │   │   ├── StatsServiceImpl.java
│   │   │       │   │   ├── ScheduledTaskServiceImpl.java
│   │   │       │   │   ├── EbookServiceImpl.java
│   │   │       │   │   ├── CoverImageServiceImpl.java
│   │   │       │   │   └── UserProfileServiceImpl.java
│   │   │       │   ├── AuthService.java
│   │   │       │   ├── BooksService.java
│   │   │       │   ├── CategoriesService.java
│   │   │       │   ├── UsersService.java
│   │   │       │   ├── RatingService.java
│   │   │       │   ├── ReviewsService.java
│   │   │       │   ├── ShelvesService.java
│   │   │       │   ├── ReadingProgressService.java
│   │   │       │   ├── PurchaseRecordService.java
│   │   │       │   ├── RecommendService.java
│   │   │       │   ├── RecommendationService.java
│   │   │       │   ├── UserBehaviorService.java
│   │   │       │   ├── KnowledgeGraphService.java
│   │   │       │   ├── KgRecommendService.java
│   │   │       │   ├── CollaborativeFilteringService.java
│   │   │       │   ├── StatsService.java
│   │   │       │   ├── ScheduledTaskService.java
│   │   │       │   ├── EbookService.java
│   │   │       │   ├── CoverImageService.java
│   │   │       │   └── UserProfileService.java
│   │   │       ├── mapper/              # Mapper层 (MySQL)
│   │   │       │   ├── BooksMapper.java
│   │   │       │   ├── CategoriesMapper.java
│   │   │       │   ├── UsersMapper.java
│   │   │       │   ├── BookRatingsMapper.java
│   │   │       │   ├── ReviewsMapper.java
│   │   │       │   ├── ShelvesMapper.java
│   │   │       │   ├── ReadingProgressMapper.java
│   │   │       │   ├── PurchaseRecordMapper.java
│   │   │       │   ├── RecommendationMapper.java
│   │   │       │   ├── UserBehaviorMapper.java
│   │   │       │   └── ...
│   │   │       ├── repository/          # Repository层 (Neo4j)
│   │   │       │   ├── BookNodeRepository.java
│   │   │       │   ├── AuthorNodeRepository.java
│   │   │       │   ├── CategoryNodeRepository.java
│   │   │       │   ├── PublisherNodeRepository.java
│   │   │       │   └── TagNodeRepository.java
│   │   │       ├── entity/              # 实体类 (MySQL)
│   │   │       │   ├── Books.java
│   │   │       │   ├── Categories.java
│   │   │       │   ├── Users.java
│   │   │       │   ├── BookRatings.java
│   │   │       │   ├── Reviews.java
│   │   │       │   ├── Shelves.java
│   │   │       │   ├── ReadingProgress.java
│   │   │       │   ├── PurchaseRecord.java
│   │   │       │   ├── Recommendation.java
│   │   │       │   ├── UserBehavior.java
│   │   │       │   └── ...
│   │   │       │   └── kg/              # 实体类 (Neo4j)
│   │   │       │       ├── BookNode.java
│   │   │       │       ├── AuthorNode.java
│   │   │       │       ├── CategoryNode.java
│   │   │       │       ├── PublisherNode.java
│   │   │       │       └── TagNode.java
│   │   │       ├── dto/                 # 数据传输对象
│   │   │       │   ├── LoginRequest.java
│   │   │       │   ├── RegisterRequest.java
│   │   │       │   ├── AuthResponse.java
│   │   │       │   ├── RecommendItem.java
│   │   │       │   ├── ReadingProgressRequest.java
│   │   │       │   └── ...
│   │   │       ├── security/            # 安全模块
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── JwtTokenProvider.java
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   ├── JwtProperties.java
│   │   │       │   ├── CustomUserDetailsService.java
│   │   │       │   ├── SecurityUtils.java
│   │   │       │   └── SecondFactorInterceptor.java
│   │   │       ├── common/              # 公共模块
│   │   │       │   ├── ApiResponse.java
│   │   │       │   ├── PageResponse.java
│   │   │       │   ├── BusinessException.java
│   │   │       │   ├── ErrorCode.java
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── RoleConstants.java
│   │   │       │   └── RequireSecondFactor.java
│   │   │       ├── event/               # 事件模块
│   │   │       │   ├── UserKgChangeEvent.java
│   │   │       │   └── UserKgEventListener.java
│   │   │       ├── util/                # 工具类
│   │   │       │   ├── FileUrlUtil.java
│   │   │       │   └── CsvUtil.java
│   │   │       ├── generator/           # 代码生成器
│   │   │       │   └── MyBatisPlusGenerator.java
│   │   │       └── LibrarySysApplication.java  # 启动类
│   │   └── resources/
│   │       ├── mapper/                  # MyBatis XML映射文件
│   │       ├── static/                  # 静态资源
│   │       │   ├── covers/              # 书籍封面图片
│   │       │   └── uploads/             # 上传文件
│   │       └── application.yml          # 应用配置文件
├── pom.xml                              # Maven配置
└── README.md                            # 项目说明文档
```

## 配置说明

### 数据库配置

| 配置项 | 值 |
|--------|-----|
| MySQL地址 | `localhost:3306` |
| 数据库名 | `library_db` |
| 用户名 | `root` |
| 密码 | `root123` |

### Neo4j配置

| 配置项 | 值 |
|--------|-----|
| 地址 | `bolt://localhost:7687` |
| 用户名 | `neo4j` |
| 密码 | `neo4j123` |

### Druid监控

| 配置项 | 值 |
|--------|-----|
| 访问地址 | http://localhost:8080/druid/ |
| 用户名 | `admin` |
| 密码 | `admin` |

### 文件存储配置

| 配置项 | 值 |
|--------|-----|
| 基础URL | `http://10.11.22.1:8080`（可在 `file.base-url` 配置） |
| 封面目录 | `static/covers/` |
| 电子书目录 | `static/uploads/ebooks/` |

### 端口配置

| 配置项 | 值 |
|--------|-----|
| 应用端口 | `8080` |

## 权限说明

系统采用 JWT + Spring Security 进行权限控制，支持以下角色：

| 角色 | 说明 | 权限范围 |
|------|------|----------|
| `USER` | 普通读者 | 访问用户端接口（图书浏览、评分、书评、书架、阅读等） |
| `BOOK_ADMIN` | 图书管理员 | 图书管理、电子书管理、购书链接、知识图谱构建 |
| `OPS_ADMIN` | 运营管理员 | 系统统计、推荐配置、用户管理 |
| `COMMUNITY_ADMIN` | 社区管理员 | 书评审核、优质标记、违规清理 |
| `ADMIN` | 超级管理员 | 拥有所有管理模块权限 |

### 管理端权限细分

| 管理模块 | 路径 | 需要角色 |
|----------|------|----------|
| 图书管理 | `/api/v1/admin/books/**` | `BOOK_ADMIN` / `ADMIN` |
| 购书链接 | `/api/v1/admin/purchase-links/**` | `BOOK_ADMIN` / `ADMIN` |
| 知识图谱 | `/api/v1/admin/kg/**` | `BOOK_ADMIN` / `ADMIN` |
| 系统统计 | `/api/v1/admin/stats/**` | `OPS_ADMIN` / `ADMIN` |
| 推荐配置 | `/api/v1/admin/recommend/**` | `OPS_ADMIN` / `ADMIN` |
| 用户管理 | `/api/v1/admin/users/**` | `OPS_ADMIN` / `ADMIN` |
| 书评审核 | `/api/v1/admin/reviews/**` | `COMMUNITY_ADMIN` / `ADMIN` |

## 开发说明

### 使用代码生成器

项目集成了 MyBatis Plus 代码生成器，可快速生成 Entity、Mapper、Service、Controller 代码：

```java
// 运行 MyBatisPlusGenerator.java
generator/MyBatisPlusGenerator.java
```

### 添加新模块

1. 在 `entity` 包下创建实体类
2. 在 `mapper` 包下创建 Mapper 接口
3. 在 `service` 包下创建 Service 接口
4. 在 `service/impl` 包下创建 Service 实现类
5. 在 `controller` 包下创建 Controller
6. 在 `resources/mapper` 下创建 XML 映射文件

### 知识图谱同步

系统支持将 MySQL 中的书籍数据同步到 Neo4j：

```bash
# 手动触发同步
POST /api/v1/admin/kg/build
```

## 常见问题

**Q: 启动时报数据库连接错误？**
A: 请检查 MySQL 服务是否启动，以及 application.yml 中的数据库配置是否正确。

**Q: Java版本不兼容？**
A: 本项目需要 JDK 17+，请确保 JAVA_HOME 指向正确的 Java 版本。

**Q: 如何访问API文档？**
A: 启动项目后访问 http://localhost:8080/doc.html

**Q: Neo4j连接失败？**
A: 请确保 Neo4j 服务正在运行，端口为 7687，并且用户名密码配置正确。知识图谱功能为可选功能，不影响核心业务。

**Q: 文件上传失败？**
A: 请确保 `static/covers/` 和 `static/uploads/ebooks/` 目录存在且有写入权限。

## 许可证

本项目仅供学习参考使用。