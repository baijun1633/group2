# Agent 工作日志 - 图书馆管理系统

## 项目概述

- **项目名称**: library_sys（图书馆管理系统）
- **技术栈**: Spring Boot 3.2.5 + MyBatis Plus 3.5.7 + Druid 1.2.27 + MySQL + Swagger/Knife4j
- **Java 版本**: 17
- **构建工具**: Maven

---

## 踩过的坑与教训总结

### 1. 配置文件格式转换问题（properties → yml）

**问题描述**: 将 `application.properties` 转换为 `application.yml` 时遇到多个问题。

**教训**:
- 转换配置文件格式时，必须仔细核对每一项配置，不能遗漏
- 用户原始的 yaml 配置文件中包含丰富的 Druid 连接池参数（initial-size、min-idle、max-active 等），转换时应当参考用户提供的原始文件，而非简单从 properties 直译

---

### 2. YAML 文件 BOM 头问题

**问题描述**: `application.yml` 文件包含 UTF-8 BOM 头（`EF BB BF`），导致 Spring Boot 无法正确解析 YAML 配置，DataSource 配置无法绑定，最终回退使用默认的 H2 驱动。

**错误现象**:
```
Cannot load driver class: org.h2.Driver
```

**教训**:
- **YAML 文件绝不能有 BOM 头**，Spring Boot 的 YAML 解析器对 BOM 头非常敏感
- 创建/编辑 YAML 文件时，务必使用 **UTF-8 无 BOM** 编码
- 排查配置不生效问题时，应第一时间检查文件编码（BOM 头、CRLF/LF 换行符）
- 使用 PowerShell 检查 BOM: `$bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF`

---

### 3. YAML 中 URL 的特殊字符 `&` 问题

**问题描述**: JDBC URL 中包含多个 `&` 字符（如 `useUnicode=true&characterEncoding=utf-8`），在 YAML 中 `&` 是锚点（anchor）的特殊字符，导致解析异常。

**教训**:
- **YAML 中包含特殊字符的值必须用引号括起来**
- `&`（锚点）、`*`（别名）、`!`（标签）、`|`（块）、`>`（折叠块）等都是 YAML 特殊字符
- 推荐所有 JDBC URL 都使用双引号包裹：
  ```yaml
  url: "jdbc:mysql://localhost:3306/db?useUnicode=true&characterEncoding=utf-8"
  ```

---

### 4. Druid Spring Boot 3 Starter 版本问题

**问题描述**: `druid-spring-boot-3-starter` 1.2.18 版本的 JAR 包中**缺少** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件，导致 Druid 的自动配置类没有被 Spring Boot 3 注册和加载。

**错误现象**:
- DataSource 配置无法正确绑定
- Druid 监控页面无法访问
- 自动回退到默认数据源

**教训**:
- **Spring Boot 3.x 使用 `AutoConfiguration.imports` 机制**，而非 Spring Boot 2.x 的 `spring.factories`
- 选择第三方 starter 时，必须确认其支持 Spring Boot 3 的自动配置机制
- **druid-spring-boot-3-starter 1.2.18 存在打包缺陷**，升级到 1.2.27 后问题解决
- 排查自动配置问题时，可用 `--debug` 参数查看条件评估报告，检查 `DataSourceAutoConfiguration` 是否匹配

---

### 5. 缺少 spring-boot-starter-data-jdbc 依赖

**问题描述**: 缺少 `spring-boot-starter-data-jdbc` 依赖，导致 `EmbeddedDatabaseType` 类不存在，`DataSourceAutoConfiguration` 无法正常工作。

**教训**:
- Spring Boot 3 项目中使用数据库时，应确保引入 `spring-boot-starter-data-jdbc` 或 `spring-boot-starter-jdbc`
- MyBatis Plus 不会自动引入 JDBC starter，需要手动添加

---

### 6. MySQL 8 连接认证问题

**问题描述**: MySQL 8 默认使用 `caching_sha2_password` 认证插件，连接时出现 `Public Key Retrieval is not allowed` 错误。

**教训**:
- MySQL 8+ 连接 URL 需要添加 `allowPublicKeyRetrieval=true` 参数
- 完整的 MySQL 8 连接 URL 参数建议：
  ```
  useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
  ```

---

### 7. 数据库用户名/密码配置错误

**问题描述**: 配置文件中的数据库用户名（`manager`）和密码（`abc123456`）与实际 MySQL 账户不匹配。

**教训**:
- 转换配置文件时，**数据库连接信息（用户名、密码、数据库名）必须向用户确认**，不能假设或沿用示例值
- 遇到 `Access denied for user` 错误时，优先确认用户名和密码

---

### 8. Java 版本不兼容问题

**问题描述**: 系统默认 Java 版本为 Java 8，而 Spring Boot 3.2.5 要求 Java 17+，导致 `UnsupportedClassVersionError`。

**教训**:
- **Spring Boot 3.x 最低要求 Java 17**
- 运行项目前必须确认 Java 版本：`java -version`
- 可通过设置 `JAVA_HOME` 环境变量指向正确的 JDK 版本

---

### 9. 端口占用问题

**问题描述**: 重复启动应用时，端口 8080 被前一个未完全关闭的进程占用。

**教训**:
- 启动新实例前，先停止之前的进程
- 使用 `netstat -ano | findstr :8080` 查找占用端口的进程
- 或在 `application.yml` 中配置 `server.port` 使用其他端口

---

## 排查问题的方法论

1. **先看错误信息** - 仔细阅读控制台输出的错误日志，定位根本原因
2. **使用 debug 模式** - `mvn spring-boot:run -Dspring-boot.run.arguments=--debug` 查看自动配置条件报告
3. **检查文件编码** - BOM 头、换行符（CRLF/LF）可能导致隐蔽问题
4. **检查依赖版本兼容性** - 特别是 Spring Boot 3 与第三方库的兼容性
5. **逐项排除** - 一次只改一个配置，验证后再进行下一个修改

---

## 当前项目配置

| 配置项 | 值 |
|--------|-----|
| 数据库 URL | `jdbc:mysql://localhost:3306/library_db` |
| 数据库用户名 | `root` |
| 数据库密码 | `root123` |
| 服务端口 | `8080` |
| Druid 监控 | `http://localhost:8080/druid/`（admin/admin）|
| Swagger 文档 | `http://localhost:8080/swagger-ui.html` |
| Java 版本 | 17（路径: `D:\java`）|

---

## Git 提交记录

- `2bcb596` - init: 搭建图书馆管理系统基础框架 - Spring Boot 3.2.5 + MyBatis Plus + Druid + Swagger
