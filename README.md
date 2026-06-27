# 图书管理系统 (Library Management System)

基于 Spring Boot 3.2.5 + MyBatis Plus 3.5.7 开发的图书管理系统后端服务。

## 技术栈

- **后端框架**: Spring Boot 3.2.5
- **持久层**: MyBatis Plus 3.5.7
- **数据库**: MySQL 8.0
- **连接池**: Druid 1.2.27
- **API文档**: Knife4j (Swagger) 4.3.0
- **模板引擎**: Thymeleaf
- **开发工具**: Lombok
- **Java版本**: 17

## 项目架构

本项目采用经典的分层架构设计，遵循 MVC 模式：

```
┌─────────────────────────────────────────────────────┐
│                   前端/客户端                        │
└─────────────────────┬───────────────────────────────┘
                      │ HTTP Request
                      ▼
┌─────────────────────────────────────────────────────┐
│              Controller 层 (控制器)                  │
│         接收请求，调用 Service，返回响应              │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│              Service 层 (服务层)                     │
│     接口定义: XxxService.java                        │
│     实现类:   XxxServiceImpl.java                    │
│         业务逻辑处理，事务管理                        │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│              Mapper 层 (数据访问层)                  │
│         继承 BaseMapper，操作数据库                   │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│              数据库 (MySQL)                          │
│              library_db                              │
└─────────────────────────────────────────────────────┘
```

### 分层职责说明

| 层级 | 职责 | 示例文件 |
|------|------|----------|
| Controller | 接收HTTP请求，参数校验，调用Service，返回JSON | `BooksController.java` |
| Service | 定义业务接口 | `BooksService.java` |
| ServiceImpl | 实现业务逻辑，事务管理 | `BooksServiceImpl.java` |
| Mapper | 数据库CRUD操作（继承BaseMapper） | `BooksMapper.java` |
| Entity | 实体类，映射数据库表结构 | `Books.java` |

## 模块介绍

### 1. 书籍管理模块 (Books)
- 书籍的增删改查
- 书籍分类关联
- 封面图片管理
- 库存管理

### 2. 分类管理模块 (Categories)
- 书籍分类的增删改查
- 分类层级管理

### 3. 借阅管理模块 (BorrowRecords)
- 借阅记录管理
- 借阅状态跟踪

### 4. 用户管理模块 (Users)
- 用户信息管理

## 数据库设计

数据库名: `library_db`

### 核心表结构

**books 表 (书籍表)**
| 字段 | 类型 | 说明 |
|------|------|------|
| book_id | BIGINT | 主键，自增 |
| title | VARCHAR | 书名 |
| author | VARCHAR | 作者 |
| isbn | VARCHAR | ISBN号 |
| publisher | VARCHAR | 出版社 |
| publish_date | DATE | 出版日期 |
| category_id | BIGINT | 分类ID |
| price | DECIMAL | 价格 |
| stock | INT | 库存数量 |
| description | TEXT | 书籍简介 |
| cover_image | VARCHAR | 封面图片URL |
| status | TINYINT | 状态: 0-下架, 1-上架 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

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
   
   编辑 `src/main/resources/application.yml`，修改数据库连接信息：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/library_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
       username: your_username
       password: your_password
   ```

4. **运行项目**
   ```bash
   mvn spring-boot:run
   ```
   
   或者使用 Maven Wrapper：
   ```bash
   ./mvnw spring-boot:run
   ```

5. **访问应用**
   - 应用地址: http://localhost:8080
   - API文档: http://localhost:8080/doc.html (Knife4j)
   - Druid监控: http://localhost:8080/druid/ (admin/admin)

## API接口

### 书籍管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/books/test` | GET | 测试接口 |
| `/books/getallbooks` | GET | 获取所有书籍 |

### API文档

启动项目后访问 Knife4j 文档页面查看完整API：
- 地址: http://localhost:8080/doc.html

## 项目结构

```
library_sys/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cqu/springboot/
│   │   │       ├── config/              # 配置类
│   │   │       │   ├── CorsConfig.java          # 跨域配置
│   │   │       │   ├── DruidConfig.java         # Druid连接池配置
│   │   │       │   ├── MyBatisPlusConfig.java   # MyBatis Plus配置
│   │   │       │   └── SwaggerConfig.java       # Swagger配置
│   │   │       ├── controller/          # 控制器层
│   │   │       │   ├── BooksController.java
│   │   │       │   ├── BorrowRecordsController.java
│   │   │       │   ├── CategoriesController.java
│   │   │       │   └── UsersController.java
│   │   │       ├── entity/              # 实体类
│   │   │       │   ├── Books.java
│   │   │       │   ├── BorrowRecords.java
│   │   │       │   ├── Categories.java
│   │   │       │   └── Users.java
│   │   │       ├── mapper/              # Mapper层
│   │   │       │   ├── BooksMapper.java
│   │   │       │   ├── BorrowRecordsMapper.java
│   │   │       │   ├── CategoriesMapper.java
│   │   │       │   └── UsersMapper.java
│   │   │       ├── service/             # Service层
│   │   │       │   ├── impl/            # Service实现类
│   │   │       │   │   ├── BooksServiceImpl.java
│   │   │       │   │   ├── BorrowRecordsServiceImpl.java
│   │   │       │   │   ├── CategoriesServiceImpl.java
│   │   │       │   │   └── UsersServiceImpl.java
│   │   │       │   ├── BooksService.java
│   │   │       │   ├── BorrowRecordsService.java
│   │   │       │   ├── CategoriesService.java
│   │   │       │   └── UsersService.java
│   │   │       ├── generator/           # 代码生成器
│   │   │       │   └── MyBatisPlusGenerator.java
│   │   │       └── LibrarySysApplication.java  # 启动类
│   │   └── resources/
│   │       ├── mapper/                  # MyBatis XML映射文件
│   │       ├── static/                  # 静态资源
│   │       │   └── covers/              # 书籍封面图片
│   │       └── application.yml          # 应用配置文件
├── pom.xml                              # Maven配置
└── README.md                            # 项目说明文档
```

## 配置说明

### 数据库配置
- 地址: `localhost:3306`
- 数据库: `library_db`
- 用户名: `root`
- 密码: `root123`

### Druid监控
- 访问地址: http://localhost:8080/druid/
- 用户名: `admin`
- 密码: `admin`

### 端口配置
- 应用端口: `8080` (可在 application.yml 中修改)

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

## 常见问题

**Q: 启动时报数据库连接错误？**
A: 请检查 MySQL 服务是否启动，以及 application.yml 中的数据库配置是否正确。

**Q: Java版本不兼容？**
A: 本项目需要 JDK 17+，请确保 JAVA_HOME 指向正确的 Java 版本。

**Q: 如何访问API文档？**
A: 启动项目后访问 http://localhost:8080/doc.html

## 许可证

本项目仅供学习参考使用。