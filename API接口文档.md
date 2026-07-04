# 图书推荐系统 — API 接口文档

> **Spring Boot 服务** `http://localhost:8080` | **FastAPI 服务** `http://localhost:8000`
>
> **认证方式**: JWT Bearer Token（除标注"无需登录"外，所有接口需在 Header 中携带 `Authorization: Bearer <token>`）
>
> **在线文档**: Knife4j `http://localhost:8080/doc.html` | Swagger UI `http://localhost:8080/swagger-ui.html`
>
> **统一响应格式**:
> ```json
> { "code": 0, "message": "success", "data": { ... } }
> ```
> `code = 0` 表示成功，非 0 表示业务错误，详见 [错误码表](#三十八错误码一览)。

---

## 目录

- [一、权限规则总览](#一权限规则总览)
- [二、认证模块](#二认证模块-apiv1auth)
- [三、图书浏览模块](#三图书浏览模块-apiv1books)
- [四、图书评分模块](#四图书评分模块-apiv1booksbookidrating)
- [五、图书分类模块](#五图书分类模块-apiv1categories)
- [六、首页聚合模块](#六首页聚合模块-apiv1)
- [七、推荐引擎模块](#七推荐引擎模块-apiv1recommend)
- [八、用户模块](#八用户模块-apiv1users)
- [九、推荐历史模块](#九推荐历史模块-apiv1usersmerecommendations)
- [十、行为历史模块](#十行为历史模块-apiv1usersmebehaviors)
- [十一、阅读管理模块](#十一阅读管理模块-apiv1reading)
- [十二、书架管理模块](#十二书架管理模块-apiv1shelves)
- [十三、书评模块](#十三书评模块)
- [十四、购买记录模块](#十四购买记录模块-apiv1purchases)
- [十五、知识图谱模块](#十五知识图谱模块-apiv1kg)
- [十六、管理员 — 图书管理](#十六管理员--图书管理-apiv1adminbooks)
- [十七、管理员 — 用户管理](#十七管理员--用户管理-apiv1adminusers)
- [十八、管理员 — 书评审核](#十八管理员--书评审核-apiv1adminreviews)
- [十九、管理员 — 系统统计](#十九管理员--系统统计-apiv1adminstats)
- [二十、管理员 — 推荐配置](#二十管理员--推荐配置-apiv1adminrecommend)
- [二十一、管理员 — 购书链接](#二十一管理员--购书链接-apiv1adminpurchase-links)
- [二十二、管理员 — 知识图谱管理](#二十二管理员--知识图谱管理-apiv1adminkg)
- [二十三、管理员 — 定时任务](#二十三管理员--定时任务-apiv1admintasks)
- [二十四、Python FastAPI 服务](#二十四python-fastapi-服务端口-8000)
- [二十五、数据模型](#二十五数据模型)
- [二十六、错误码一览](#二十六错误码一览)

---

## 一、权限规则总览

| 路径规则 | 角色要求 |
| --- | --- |
| `/api/v1/auth/**` | 无需登录（部分接口实际需要 Token） |
| `GET /api/v1/books/**` | 无需登录 |
| `GET /api/v1/categories/**` | 无需登录 |
| `GET /api/v1/kg/graph/**`、`GET /api/v1/kg/stats` | 无需登录 |
| `/api/v1/recommend/**` | 无需登录 |
| `GET /api/v1/banners` | 无需登录 |
| `/api/v1/admin/books/**`、`/api/v1/admin/purchase-links/**`、`/api/v1/admin/kg/**` | BOOK_ADMIN 或 ADMIN |
| `/api/v1/admin/stats/**`、`/api/v1/admin/recommend/**`、`/api/v1/admin/users/**` | OPS_ADMIN 或 ADMIN |
| `/api/v1/admin/reviews/**` | COMMUNITY_ADMIN 或 ADMIN |
| `/api/v1/admin/**`（兜底） | ADMIN |
| 其余所有 `/api/v1/**` | 需登录 |

### 角色说明

| 角色标识 | 说明 |
| --- | --- |
| `USER` | 普通用户 |
| `BOOK_ADMIN` | 图书管理员（管理图书、购书链接、知识图谱） |
| `OPS_ADMIN` | 运营管理员（统计、推荐配置、用户管理） |
| `COMMUNITY_ADMIN` | 社区管理员（书评审核） |
| `ADMIN` | 超级管理员（拥有全部权限） |

---

## 二、认证模块 `/api/v1/auth`

| # | 方法 | 路径 | 请求体 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ------ | -------- | ---- | ---- |
| 1 | POST | `/register` | `RegisterRequest` | `ApiResponse<AuthResponse>` | 无需登录 | 用户注册 |
| 2 | POST | `/login` | `LoginRequest` | `ApiResponse<AuthResponse>` | 无需登录 | 用户登录 |
| 3 | POST | `/refresh` | `RefreshRequest` | `ApiResponse<AuthResponse>` | 无需登录 | 刷新 Token |
| 4 | POST | `/logout` | — | `ApiResponse<Void>` | 需登录 | 退出登录 |
| 5 | POST | `/second-password` | `Map<String,String>` | `ApiResponse<Void>` | 需登录 | 设置二级操作密码 |

### 请求/响应详情

#### 1. 用户注册 `POST /api/v1/auth/register`

**请求体** `RegisterRequest`:

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
| --- | --- | --- | --- | --- |
| `username` | String | 是 | `@NotBlank`, `@Size(min=3, max=30)` | 用户名 |
| `password` | String | 是 | `@NotBlank`, `@Size(min=6, max=50)` | 密码 |
| `confirmPassword` | String | 是 | `@NotBlank` | 确认密码 |
| `nickname` | String | 否 | — | 昵称 |
| `preferenceTags` | List\<String\> | 否 | — | 偏好标签 |

**响应体** `AuthResponse`:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | Long | 用户ID |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `accessToken` | String | 访问Token（有效期2小时） |
| `refreshToken` | String | 刷新Token（有效期7天） |
| `role` | String | 用户角色 |

---

#### 2. 用户登录 `POST /api/v1/auth/login`

**请求体** `LoginRequest`:

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
| --- | --- | --- | --- | --- |
| `username` | String | 是 | `@NotBlank` | 用户名 |
| `password` | String | 是 | `@NotBlank` | 密码 |

**响应体**: 同注册接口 `AuthResponse`。

---

#### 3. 刷新 Token `POST /api/v1/auth/refresh`

**请求体** `RefreshRequest`:

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
| --- | --- | --- | --- | --- |
| `refreshToken` | String | 是 | `@NotBlank` | 刷新Token |

**响应体**: 同注册接口 `AuthResponse`（返回新的 Token 对）。

---

#### 4. 退出登录 `POST /api/v1/auth/logout`

无需请求体。前端删除本地存储的 Token 即可。实际需要携带有效 JWT。

---

#### 5. 设置二级操作密码 `POST /api/v1/auth/second-password`

**请求体**:

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `currentPassword` | String | 是 | 当前登录密码（用于验证） |
| `newPassword` | String | 是 | 新的二级操作密码 |

---

## 三、图书浏览模块 `/api/v1/books`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | GET | `/search` | `keyword?`, `publisher?`, `tag?`, `page=1`, `size=20` | `ApiResponse<PageResponse<Books>>` | 无需登录 | 多维度搜索（书名/作者/ISBN/出版社/标签） |
| 2 | GET | `/` | `categoryId?`, `tag?`, `sortBy=createTime`, `order=desc`, `page=1`, `size=20` | `ApiResponse<PageResponse<Books>>` | 无需登录 | 分类/标签筛选+排序 |
| 3 | GET | `/{bookId}` | 路径: bookId | `ApiResponse<Map<String,Object>>` | 无需登录 | 图书详情（含标签、购书链接） |
| 4 | GET | `/{bookId}/preview` | 路径: bookId | `ApiResponse<Map<String,Object>>` | 无需登录 | 试读内容预览 |
| 5 | GET | `/{bookId}/ebook` | 路径: bookId | `ApiResponse<EbookInfo>` | 无需登录 | 电子书试读 |

### 参数说明

| 参数名 | 位置 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `keyword` | Query | String | — | 搜索关键词（模糊匹配书名/作者/ISBN） |
| `publisher` | Query | String | — | 出版社关键词（模糊匹配） |
| `tag` | Query | String | — | 标签关键词（模糊匹配） |
| `categoryId` | Query | Long | — | 分类ID |
| `sortBy` | Query | String | `createTime` | 排序字段：createTime/avgRating/publishDate |
| `order` | Query | String | `desc` | 排序方向：asc/desc |
| `page` | Query | int | `1` | 页码 |
| `size` | Query | int | `20` | 每页条数 |

### 响应详情

#### 图书详情响应 `GET /api/v1/books/{bookId}`

返回 `Map<String, Object>`，包含 Books 实体全部字段及：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tags` | List\<String\> | 标签列表（从JSON解析） |
| `purchaseLinks` | List\<PurchaseLinks\> | 购书链接列表 |

> 会记录浏览行为埋点（5分钟内去重）。

#### 电子书试读响应 `GET /api/v1/books/{bookId}/ebook`

**响应体** `EbookInfo`:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `bookId` | Long | 图书ID |
| `title` | String | 书名 |
| `ebookType` | String | 电子书类型：epub/pdf |
| `totalChapters` | Integer | 总章节数 |
| `previewChapters` | Integer | 可试读章节数（未登录3章，登录10章） |
| `isLogin` | Boolean | 是否已登录 |
| `truncated` | Boolean | 是否截断 |
| `chapters` | List\<EbookChapter\> | 章节列表 |
| `message` | String | 提示信息 |

**EbookChapter**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `index` | int | 章节序号（从1开始） |
| `title` | String | 章节标题 |
| `content` | String | 章节内容（HTML） |
| `length` | int | 内容字符数 |

---

## 四、图书评分模块 `/api/v1/books/{bookId}/rating`

| # | 方法 | 路径 | 请求体 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ------ | -------- | ---- | ---- |
| 1 | POST | `/` | `{score: Integer}` | `ApiResponse<Void>` | 需登录 | 提交评分（1-5分） |
| 2 | GET | `/` | — | `ApiResponse<Map<String,Object>>` | 无需登录 | 获取评分统计 |

### 评分提交请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `score` | Integer | 是 | 评分（1-5） |

### 评分统计响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `avgRating` | BigDecimal | 平均评分 |
| `ratingCount` | Integer | 评分人数 |
| `distribution` | Map | 评分分布（1-5分各数量） |

> 评分会触发知识图谱变更事件，更新用户画像。

---

## 五、图书分类模块 `/api/v1/categories`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | GET | `/` | — | `ApiResponse<List<Categories>>` | 无需登录 | 所有分类列表 |
| 2 | GET | `/{categoryId}` | 路径: categoryId | `ApiResponse<Categories>` | 无需登录 | 分类详情 |

---

## 六、首页聚合模块 `/api/v1`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | GET | `/banners` | `limit=6` | `ApiResponse<List<RecommendItem>>` | 无需登录 | 轮播图（热门图书） |

**RecommendItem 响应结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `bookId` | Long | 图书ID |
| `recommendId` | Long | 推荐记录ID |
| `title` | String | 书名 |
| `author` | String | 作者 |
| `coverImage` | String | 封面图片URL |
| `avgRating` | Double | 平均评分 |
| `score` | Double | 推荐分数（0-1） |
| `reason` | String | 推荐理由 |
| `reasonPath` | List\<String\> | 推理路径 |
| `source` | String | 推荐来源：KG/ITEMCF/HOT/NEW/HYBRID |

---

## 七、推荐引擎模块 `/api/v1/recommend`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | GET | `/home` | `limit=10` | `ApiResponse<List<RecommendItem>>` | 无需登录 | 首页推荐 |
| 2 | GET | `/hot` | `days=90`, `limit=10` | `ApiResponse<List<RecommendItem>>` | 无需登录 | 热门图书 |
| 3 | GET | `/new` | `months=6`, `limit=10` | `ApiResponse<List<RecommendItem>>` | 无需登录 | 新书推荐 |
| 4 | GET | `/{bookId}/similar` | `limit=10` | `ApiResponse<List<RecommendItem>>` | 需登录 | 相似读物 |
| 5 | GET | `/{bookId}/extended` | `limit=10` | `ApiResponse<List<RecommendItem>>` | 需登录 | 延伸阅读 |
| 6 | POST | `/{recommendId}/click` | 路径: recommendId | `ApiResponse<Boolean>` | 需登录 | 推荐点击回调 |
| 7 | GET | `/{bookId}/explain` | 路径: bookId | `ApiResponse<RecommendItem>` | 无需登录 | 推荐解释详情 |

### 推荐算法说明

| 推荐类型 | 来源标识 | 算法 | 说明 |
| --- | --- | --- | --- |
| 首页推荐 | `HYBRID` | 加权混合 | KG+ItemCF+热门+新书，登录用户个性化，未登录返回热门/新书 |
| 热门图书 | `HOT` | 排序 | 按评分人数和评分排序 |
| 新书推荐 | `NEW` | 排序 | 按出版日期排序 |
| 相似读物 | `KG` | 知识图谱1-2跳推理 | 同作者/同分类/同标签的图书 |
| 延伸阅读 | `KG` | 知识图谱3跳+推理 | 间接关联的高质量图书 |

> 首页推荐会调用 `recommendationService.generateRecommendation` 记录推荐历史。

---

## 八、用户模块 `/api/v1/users`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | GET | `/me` | — | `ApiResponse<Map<String,Object>>` | 需登录 | 获取当前用户信息 |
| 2 | PUT | `/me/preferences` | Body | `ApiResponse<String>` | 需登录 | 更新偏好标签 |
| 3 | GET | `/me/profile` | — | `ApiResponse<Map<String,Object>>` | 需登录 | 获取用户画像 |
| 4 | POST | `/me/profile/refresh` | — | `ApiResponse<String>` | 需登录 | 刷新用户画像 |

### 请求/响应详情

#### 获取当前用户信息 `GET /api/v1/users/me`

**响应字段**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | Long | 用户ID |
| `username` | String | 用户名 |
| `nickname` | String | 昵称 |
| `avatar` | String | 头像URL |
| `role` | String | 角色 |
| `createdAt` | LocalDateTime | 创建时间 |
| `preferenceTags` | List\<String\> | 偏好标签列表 |

#### 更新偏好标签 `PUT /api/v1/users/me/preferences`

**请求体**:

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `preferenceTags` | List\<String\> | 是 | 新的偏好标签列表 |

#### 获取用户画像 `GET /api/v1/users/me/profile`

**响应字段**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tagVector` | Map | 标签权重向量 |
| `preferredAuthors` | List | 偏好作者 |
| `preferredCategories` | List | 偏好分类 |

---

## 九、推荐历史模块 `/api/v1/users/me/recommendations`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | GET | `/` | `clicked?`, `collected?`, `page=1`, `size=20` | `ApiResponse<PageResponse<Recommendation>>` | 需登录 | 推荐历史查询 |

### 参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `clicked` | Boolean | 否 | 是否只看已点击的 |
| `collected` | Boolean | 否 | 是否只看已收藏的 |
| `page` | int | 否 | 页码，默认1 |
| `size` | int | 否 | 每页条数，默认20 |

---

## 十、行为历史模块 `/api/v1/users/me/behaviors`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | GET | `/` | `type?`, `page=1`, `size=20` | `ApiResponse<PageResponse<UserBehavior>>` | 需登录 | 行为历史查询 |

### 参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `type` | String | 否 | 行为类型：view/search/rate/collect/review |
| `page` | int | 否 | 页码，默认1 |
| `size` | int | 否 | 每页条数，默认20 |

---

## 十一、阅读管理模块 `/api/v1/reading`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | PUT | `/progress` | Body | `ApiResponse<Map<String,Object>>` | 需登录 | 同步阅读进度 |
| 2 | GET | `/progress/{bookId}` | 路径: bookId | `ApiResponse<ReadingProgress>` | 需登录 | 获取阅读进度 |
| 3 | GET | `/history` | `status?`, `page=1`, `size=20` | `ApiResponse<PageResponse<ReadingProgress>>` | 需登录 | 阅读历史 |
| 4 | GET | `/stats` | — | `ApiResponse<Map<String,Object>>` | 需登录 | 阅读统计 |

### 请求/响应详情

#### 同步阅读进度 `PUT /api/v1/reading/progress`

**请求体** `ReadingProgressRequest`:

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bookId` | Long | 是 | 图书ID |
| `currentPage` | Integer | 是 | 当前页码 |
| `chapter` | String | 否 | 当前章节 |
| `totalPages` | Integer | 否 | 总页数 |
| `readDuration` | Integer | 否 | 本次会话阅读时长（秒） |
| `device` | String | 否 | 设备信息：PC/Android/iOS |

**响应字段**: `{ progressId: Long }`

> 后端自动计算 `percentage`，累加 `readDuration`，并记录阅读行为用于统计。

#### 阅读统计响应 `GET /api/v1/reading/stats`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `weekReadDuration` | int | 本周阅读时长（秒） |
| `monthReadDuration` | int | 本月阅读时长（秒） |
| `finishedCount` | int | 已读完数量 |
| `readingCount` | int | 在读数量 |

---

## 十二、书架管理模块 `/api/v1/shelves`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | POST | `/` | Query: `name`, `description?` | `ApiResponse<Map<String,Object>>` | 需登录 | 创建书架 |
| 2 | GET | `/` | — | `ApiResponse<List<Map<String,Object>>>` | 需登录 | 书架列表 |
| 3 | GET | `/{shelfId}` | 路径: shelfId | `ApiResponse<Map<String,Object>>` | 需登录 | 书架详情+图书列表 |
| 4 | POST | `/{shelfId}/books` | Query: `bookId`, `readingStatus=0` | `ApiResponse<Void>` | 需登录 | 添加图书到书架 |
| 5 | PUT | `/{shelfId}/books/{bookId}` | Query: `readingStatus` | `ApiResponse<Void>` | 需登录 | 更新阅读状态 |
| 6 | DELETE | `/{shelfId}/books/{bookId}` | — | `ApiResponse<Void>` | 需登录 | 从书架移除 |
| 7 | DELETE | `/{shelfId}` | — | `ApiResponse<Void>` | 需登录 | 删除书架 |

### 阅读状态枚举

| 值 | 说明 |
| --- | --- |
| `0` | 未读 |
| `1` | 在读 |
| `2` | 已读 |

> 添加图书到书架会：记录收藏行为埋点、触发知识图谱变更事件、联动标记推荐记录为已收藏。

---

## 十三、书评模块

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | POST | `/api/v1/books/{bookId}/reviews` | Body: `{content, markdown?}` | `ApiResponse<Map<String,Object>>` | 需登录 | 发布书评 |
| 2 | GET | `/api/v1/books/{bookId}/reviews` | `sortBy=likes`, `page=1`, `size=10` | `ApiResponse<Map<String,Object>>` | 无需登录 | 书评列表（已审核通过） |
| 3 | DELETE | `/api/v1/reviews/{reviewId}` | — | `ApiResponse<Void>` | 需登录 | 删除自己的书评 |
| 4 | POST | `/api/v1/reviews/{reviewId}/like` | — | `ApiResponse<Map<String,Object>>` | 需登录 | 点赞/取消（toggle） |
| 5 | POST | `/api/v1/reviews/{reviewId}/reply` | Body: `{content}` | `ApiResponse<Map<String,Object>>` | 需登录 | 回复书评 |
| 6 | GET | `/api/v1/reviews/{reviewId}/replies` | — | `ApiResponse<List<Map<String,Object>>>` | 需登录 | 书评回复列表 |

### 请求/响应详情

#### 发布书评 `POST /api/v1/books/{bookId}/reviews`

**请求体**:

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `content` | String | 是 | 书评内容（纯文本） |
| `markdown` | String | 否 | 书评内容（Markdown格式） |

**响应字段**: `{ reviewId: Long, status: "pending_audit" }`

> 书评状态为"待审核"，需管理员审核后才能公开显示。发布后触发知识图谱变更事件。

#### 书评列表排序

| sortBy值 | 说明 |
| --- | --- |
| `likes` | 按点赞数排序（默认） |
| `createTime` | 按创建时间排序 |

---

## 十四、购买记录模块 `/api/v1/purchases`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | POST | `/` | Body | `ApiResponse<Map<String,Object>>` | 需登录 | 记录购买意向 |
| 2 | GET | `/` | `bookId?`, `platform?`, `page=1`, `size=20` | `ApiResponse<PageResponse<PurchaseRecord>>` | 需登录 | 购买记录列表 |

### 购买记录提交 `POST /api/v1/purchases`

**请求体** `PurchaseRecordRequest`:

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bookId` | Long | 是 | 图书ID |
| `platform` | String | 是 | 购买平台：京东/当当/淘宝等 |
| `price` | BigDecimal | 否 | 实际支付价格 |
| `url` | String | 否 | 购买链接URL |

**响应字段**: `{ purchaseId: Long }`

---

## 十五、知识图谱模块 `/api/v1/kg`

| # | 方法 | 路径 | 参数 | 响应类型 | 权限 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- | ---- |
| 1 | POST | `/build` | Body (可选) | `ApiResponse<Map<String,Object>>` | 需登录 | 触发用户图谱构建 |
| 2 | GET | `/build/status/{taskId}` | 路径: taskId | `ApiResponse<Map<String,Object>>` | 需登录 | 查询构建任务状态 |
| 3 | POST | `/query` | Body | `ApiResponse<List<Map<String,Object>>>` | 需登录 | Cypher 只读查询 |
| 4 | GET | `/graph/{entityType}/{entityId}` | Query: `depth=2` | `ApiResponse<GraphData>` | 无需登录 | 图谱可视化数据 |
| 5 | GET | `/overview` | — | `ApiResponse<GraphData>` | 需登录 | 完整用户图谱数据 |
| 6 | GET | `/stats` | — | `ApiResponse<Map<String,Object>>` | 无需登录 | 图谱统计 |
| 7 | GET | `/metadata` | — | `ApiResponse<Map<String,Object>>` | 需登录 | 图谱元数据 |
| 8 | GET | `/entities/{type}` | Query: `page=1`, `size=20` | `ApiResponse<Map<String,Object>>` | 需登录 | 分页查询实体 |
| 9 | GET | `/entities/{type}/{name}` | — | `ApiResponse<Map<String,Object>>` | 需登录 | 实体详情 |
| 10 | DELETE | `/entities/{type}/{name}` | — | `ApiResponse<Void>` | 需登录 | 移除实体 |

### 请求/响应详情

#### 构建请求 `POST /api/v1/kg/build`

**请求体** `KgBuildRequest`（可选）:

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `bookIds` | List\<Long\> | null | 指定书籍ID列表，为空则全量构建 |
| `forceRebuild` | Boolean | false | 是否强制全量重建 |
| `targetUserIds` | List\<Long\> | null | 管理员指定目标用户ID列表 |

#### Cypher 查询请求 `POST /api/v1/kg/query`

**请求体** `KgQueryRequest`:

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `cypher` | String | — | Cypher 查询语句 |
| `limit` | Integer | 100 | 返回结果限制条数 |

#### 图谱可视化数据 `GET /api/v1/kg/graph/{entityType}/{entityId}`

**响应体** `GraphData`:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodes` | List\<GraphNode\> | 节点列表 |
| `edges` | List\<GraphEdge\> | 边列表 |

**GraphNode**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | String | 节点唯一ID |
| `label` | String | 显示标签 |
| `type` | String | 节点类型：Book/Author/Publisher/Category/Tag |
| `properties` | Map\<String,Object\> | 附加属性 |

**GraphEdge**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `source` | String | 起始节点ID |
| `target` | String | 目标节点ID |
| `type` | String | 关系类型：WRITTEN_BY/PUBLISHED_BY/BELONGS_TO/TAGGED_AS |
| `label` | String | 显示标签 |

#### 实体类型

| 类型标识 | 说明 | 唯一标识字段 |
| --- | --- | --- |
| `Book` | 图书 | bookId |
| `Author` | 作者 | name |
| `Publisher` | 出版社 | name |
| `Category` | 分类 | categoryId |
| `Tag` | 标签 | name |

> 图谱查询自动按当前用户过滤。移除 Category 类型实体会被拒绝。

---

## 十六、管理员 — 图书管理 `/api/v1/admin/books`

**权限**: BOOK_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 响应类型 | 二级密码 | 说明 |
| - | ---- | ---- | ---- | -------- | -------- | ---- |
| 1 | POST | `/` | Body: Map | `ApiResponse<Books>` | — | 单本录入图书 |
| 2 | PUT | `/{bookId}` | Body: Map | `ApiResponse<Books>` | — | 编辑图书 |
| 3 | DELETE | `/{bookId}` | — | `ApiResponse<Void>` | **需要** | 删除图书 |
| 4 | POST | `/batch` | Form: file (CSV) | `ApiResponse<Map<String,Object>>` | **需要** | 批量导入图书 |
| 5 | PUT | `/{bookId}/preview` | Body: Map | `ApiResponse<Void>` | — | 更新试读内容 |
| 6 | POST | `/{bookId}/ebook` | Form: file | `ApiResponse<Map<String,Object>>` | — | 上传电子书 |
| 7 | POST | `/{bookId}/cover` | Form: file | `ApiResponse<Map<String,Object>>` | — | 上传封面图片 |

### 请求/响应详情

#### 单本录入 `POST /api/v1/admin/books`

**请求体** (Map):

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `title` | String | 是 | 书名 |
| `author` | String | 是 | 作者 |
| `isbn` | String | 否 | ISBN |
| `publisher` | String | 否 | 出版社 |
| `description` | String | 否 | 简介 |
| `coverImage` | String | 否 | 封面图片URL |
| `tags` | Object | 否 | 标签（序列化为JSON） |
| `price` | BigDecimal | 否 | 价格 |
| `stock` | Integer | 否 | 库存 |
| `pages` | Integer | 否 | 页数 |
| `categoryId` | Long | 否 | 分类ID |
| `seriesInfo` | String | 否 | 系列信息 |

#### 批量导入 `POST /api/v1/admin/books/batch`

**CSV 表头**: `title, author, isbn, publisher, price, stock, pages, categoryId, description`

**响应字段**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `total` | int | 总行数 |
| `success` | int | 成功数 |
| `failed` | int | 失败数 |
| `errors` | List | 错误详情 `[{row, message}]` |

#### 上传电子书 `POST /api/v1/admin/books/{bookId}/ebook`

**支持格式**: .epub / .pdf / .txt，最大 100MB
**文件名规则**: `{bookId}.{ext}`，重复上传覆盖旧文件

**响应字段**: `{ bookId, ebookUrl, ebookType, size }`

#### 上传封面图片 `POST /api/v1/admin/books/{bookId}/cover`

**支持格式**: jpg/jpeg/png/gif/webp，最大 5MB
**文件名规则**: `{bookId}.{ext}`，重复上传覆盖旧文件

**响应字段**: `{ bookId, coverImage, size }`

---

## 十七、管理员 — 用户管理 `/api/v1/admin/users`

**权限**: OPS_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 响应类型 | 二级密码 | 说明 |
| - | ---- | ---- | ---- | -------- | -------- | ---- |
| 1 | GET | `/` | `page=1`, `size=10`, `keyword?`, `role?`, `status?` | `ApiResponse<PageResponse<Users>>` | — | 用户列表 |
| 2 | POST | `/` | Body: Map | `ApiResponse<Void>` | — | 新增用户 |
| 3 | PUT | `/{userId}` | Body: Map | `ApiResponse<Void>` | **需要** | 修改用户 |
| 4 | DELETE | `/{userId}` | — | `ApiResponse<Void>` | **需要** | 删除用户 |
| 5 | GET | `/{userId}/reading/stats` | — | `ApiResponse<Map<String,Object>>` | — | 用户阅读统计 |
| 6 | GET | `/{userId}/reading/history` | `status?`, `page=1`, `size=20` | `ApiResponse<PageResponse<Map<String,Object>>>` | — | 用户阅读历史 |

### 参数说明

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `keyword` | String | 否 | 搜索关键词（匹配用户名或昵称） |
| `role` | String | 否 | 角色筛选：ADMIN/USER |
| `status` | Byte | 否 | 状态筛选：0-禁用/1-启用 |

#### 新增用户请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | String | 是 | 用户名 |
| `password` | String | 是 | 密码 |
| `nickname` | String | 否 | 昵称 |
| `email` | String | 否 | 邮箱 |
| `phone` | String | 否 | 手机号 |
| `role` | String | 否 | 角色，默认USER |

#### 修改用户请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `nickname` | String | 否 | 昵称 |
| `email` | String | 否 | 邮箱 |
| `phone` | String | 否 | 手机号 |
| `role` | String | 否 | 角色 |
| `password` | String | 否 | 新密码 |
| `status` | Byte | 否 | 状态：0/1 |

> 仅传入的字段才会更新（部分更新）。

#### 用户阅读历史响应

每项包含: `id`, `userId`, `bookId`, `bookTitle`, `currentPage`, `chapter`, `totalPages`, `percentage`, `readDuration`, `device`, `updateTime`

---

## 十八、管理员 — 书评审核 `/api/v1/admin/reviews`

**权限**: COMMUNITY_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 响应类型 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- |
| 1 | GET | `/pending` | `page=1`, `size=20` | `ApiResponse<Map<String,Object>>` | 待审核书评列表 |
| 2 | PUT | `/{reviewId}/audit` | Body: `{action}` | `ApiResponse<Void>` | 审核书评 |
| 3 | PUT | `/{reviewId}/featured` | Body: `{featured: Boolean}` | `ApiResponse<Void>` | 标记优质书评 |
| 4 | GET | `/featured` | `page=1`, `size=20` | `ApiResponse<Map<String,Object>>` | 优质书评列表 |
| 5 | POST | `/batch-delete` | Body: `{reviewIds: [1,2,3]}` | `ApiResponse<Map<String,Object>>` | 批量软删除 |
| 6 | GET | `/search` | `keyword?`, `status?`, `page=1`, `size=20` | `ApiResponse<Map<String,Object>>` | 搜索书评 |

### 审核操作 `PUT /{reviewId}/audit`

**请求体**:

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `action` | String | 是 | `approve`=通过 / `reject`=拒绝 / `delete`=删除 |

### 书评状态枚举

| 值 | 说明 |
| --- | --- |
| `0` | 待审核 |
| `1` | 已通过 |
| `2` | 已拒绝 |
| `3` | 已删除 |

### 搜索书评参数

| 参数名 | 类型 | 说明 |
| --- | --- | --- |
| `keyword` | String | 搜索关键词（匹配书评内容） |
| `status` | Byte | 状态：0-待审核/1-已通过/2-已拒绝（不传=全部不含已删除） |

---

## 十九、管理员 — 系统统计 `/api/v1/admin/stats`

**权限**: OPS_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 响应类型 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- |
| 1 | GET | `/` | — | `ApiResponse<Map<String,Object>>` | 系统统计总览 |
| 2 | GET | `/user-growth` | — | `ApiResponse<List<Map<String,Object>>>` | 用户增长趋势 |
| 3 | GET | `/rating-distribution` | — | `ApiResponse<List<Map<String,Object>>>` | 评分分布 |
| 4 | GET | `/top-books` | `limit=10` | `ApiResponse<List<Map<String,Object>>>` | 热门图书 Top N |
| 5 | GET | `/behavior-distribution` | — | `ApiResponse<List<Map<String,Object>>>` | 行为类型分布 |

### 响应说明

| 接口 | 响应内容 |
| --- | --- |
| 系统统计总览 | 总用户数、图书数、书评数、评分总数、分类数、待审核书评数、近7天活跃用户 |
| 用户增长趋势 | 近30天每日新增用户数（折线图数据） |
| 评分分布 | 各评分段(0-1/1-2/2-3/3-4/4-5)的图书数量（柱状图数据） |
| 热门图书 | 按浏览量排序的图书列表（横向柱状图数据） |
| 行为分布 | 各行为类型(view/collect/rate/review/read/search)的数量（饼图数据） |

---

## 二十、管理员 — 推荐配置 `/api/v1/admin/recommend`

**权限**: OPS_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 响应类型 | 二级密码 | 说明 |
| - | ---- | ---- | ---- | -------- | -------- | ---- |
| 1 | GET | `/config` | — | `ApiResponse<Map<String,Object>>` | — | 获取推荐权重配置 |
| 2 | PUT | `/config` | Body: Map | `ApiResponse<Void>` | **需要** | 更新权重 |

### 权重配置请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `kg_weight` | BigDecimal | 是 | 知识图谱推荐权重 |
| `itemcf_weight` | BigDecimal | 是 | ItemCF协同过滤权重 |
| `hot_weight` | BigDecimal | 是 | 热门推荐权重 |
| `new_weight` | BigDecimal | 是 | 新书推荐权重 |

> 权重之和必须为 1.0，否则返回错误。

---

## 二十一、管理员 — 购书链接 `/api/v1/admin/purchase-links`

**权限**: BOOK_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 响应类型 | 二级密码 | 说明 |
| - | ---- | ---- | ---- | -------- | -------- | ---- |
| 1 | POST | `/batch` | Form: file (CSV) | `ApiResponse<Map<String,Object>>` | **需要** | 批量导入购书链接 |

### CSV 格式

**表头**: `book_id, platform, url, price`

**响应字段**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `total` | int | 总行数 |
| `success` | int | 成功数 |
| `failed` | int | 失败数 |
| `errors` | List | 错误详情 `[{row, message}]` |

---

## 二十二、管理员 — 知识图谱管理 `/api/v1/admin/kg`

**权限**: BOOK_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 响应类型 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- |
| 1 | POST | `/build` | Body: KgBuildRequest (可选) | `ApiResponse<Map<String,Object>>` | 触发图谱构建 |
| 2 | GET | `/build/status/{taskId}` | 路径: taskId | `ApiResponse<Map<String,Object>>` | 构建任务状态 |
| 3 | GET | `/stats` | — | `ApiResponse<Map<String,Object>>` | 全部用户图谱统计 |

### 构建请求 `POST /api/v1/admin/kg/build`

**请求体** `KgBuildRequest`（可选）:

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `bookIds` | List\<Long\> | null | 指定书籍ID列表 |
| `forceRebuild` | Boolean | false | 是否强制全量重建 |
| `targetUserIds` | List\<Long\> | null | 目标用户ID列表（为空则全量） |

---

## 二十三、管理员 — 定时任务 `/api/v1/admin/tasks`

**权限**: ADMIN

| # | 方法 | 路径 | 参数 | 响应类型 | 说明 |
| - | ---- | ---- | ---- | -------- | ---- |
| 1 | POST | `/compute-similarity` | — | `ApiResponse<Map<String,Object>>` | 手动触发相似度矩阵计算 |
| 2 | POST | `/sync-kg` | — | `ApiResponse<Map<String,Object>>` | 手动触发 KG 数据同步 |
| 3 | GET | `/logs` | `limit=20` | `ApiResponse<List<KgSyncLog>>` | 同步日志查询 |

### 响应说明

| 接口 | 响应内容 |
| --- | --- |
| 相似度矩阵计算 | `{ similarityPairs: int, message: String }` |
| KG数据同步 | `{ recordsSynced: int, message: String }` |
| 同步日志 | `KgSyncLog` 列表：`{ id, syncType, status, recordsSynced, errorMessage, startTime, endTime }` |

---

## 二十四、Python FastAPI 服务（端口 8000）

### 推荐服务 `/api/v1/recommend`

| # | 方法 | 路径 | 参数 | 说明 |
| - | ---- | ---- | ---- | ---- |
| 1 | POST | `/home` | Body: `{userId?, limit=10}` | 混合推荐 |
| 2 | GET | `/hot` | `days=90`, `limit=10` | 热门图书 |
| 3 | GET | `/new` | `months=6`, `limit=10` | 新书推荐 |
| 4 | GET | `/itemcf/{user_id}` | `limit=10` | ItemCF 协同过滤 |
| 5 | GET | `/kg/{user_id}` | `limit=10` | 知识图谱推荐 |

### 知识图谱 `/api/v1/kg`

| # | 方法 | 路径 | 参数 | 说明 |
| - | ---- | ---- | ---- | ---- |
| 1 | POST | `/query` | Body: `{cypher, limit=100}` | Cypher 查询 |
| 2 | GET | `/stats` | — | 图谱统计 |
| 3 | GET | `/subgraph` | `bookId?`, `depth=2`, `limit=50` | 子图数据 |

### 离线任务 `/api/v1/tasks`

| # | 方法 | 路径 | 说明 |
| - | ---- | ---- | ---- |
| 1 | POST | `/compute-similarity` | 相似度矩阵计算 |
| 2 | POST | `/sync-kg` | KG 数据同步 |

---

## 二十五、数据模型

### 核心实体字段说明

#### Users — 用户

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | Long | 用户ID（自增主键） |
| `username` | String | 用户名 |
| `password` | String | 密码（BCrypt加密，接口返回时置为null） |
| `secondPassword` | String | 二级操作密码（BCrypt） |
| `nickname` | String | 昵称 |
| `email` | String | 邮箱 |
| `phone` | String | 手机号 |
| `avatar` | String | 头像URL |
| `role` | String | 角色 |
| `preferenceTags` | String | 偏好标签（JSON） |
| `status` | Byte | 状态：0-禁用/1-启用 |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

#### Books — 图书

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `bookId` | Long | 图书ID（自增主键） |
| `title` | String | 书名 |
| `author` | String | 作者 |
| `isbn` | String | ISBN |
| `publisher` | String | 出版社 |
| `publishDate` | LocalDate | 出版日期 |
| `categoryId` | Long | 分类ID |
| `price` | BigDecimal | 价格 |
| `stock` | Integer | 库存 |
| `pages` | Integer | 页数 |
| `tags` | String | 标签（JSON） |
| `avgRating` | BigDecimal | 平均评分 |
| `ratingCount` | Integer | 评分人数 |
| `viewCount` | Integer | 浏览次数 |
| `collectCount` | Integer | 收藏次数 |
| `seriesInfo` | String | 系列信息 |
| `description` | String | 简介 |
| `previewContent` | String | 试读内容 |
| `ebookUrl` | String | 电子书URL |
| `ebookType` | String | 电子书类型：epub/pdf |
| `ebookSize` | Long | 电子书文件大小（字节） |
| `coverImage` | String | 封面图片URL（JSON序列化名为 `coverUrl`） |
| `status` | Byte | 状态：0-下架/1-上架 |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

#### Reviews — 书评

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `reviewId` | Long | 书评ID（自增主键） |
| `bookId` | Long | 图书ID |
| `userId` | Long | 用户ID |
| `content` | String | 书评内容（纯文本） |
| `markdown` | String | 书评内容（Markdown） |
| `status` | Byte | 状态：0-待审核/1-已通过/2-已拒绝/3-已删除 |
| `likesCount` | Integer | 点赞数 |
| `repliesCount` | Integer | 回复数 |
| `isFeatured` | Byte | 是否优质：0-否/1-是 |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

#### Shelves — 书架

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `shelfId` | Long | 书架ID |
| `userId` | Long | 用户ID |
| `name` | String | 书架名称 |
| `description` | String | 书架描述 |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

#### ShelfBooks — 书架图书关联

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 主键ID |
| `shelfId` | Long | 书架ID |
| `bookId` | Long | 图书ID |
| `readingStatus` | Integer | 阅读状态：0-未读/1-在读/2-已读 |
| `addTime` | LocalDateTime | 添加时间 |

#### ReadingProgress — 阅读进度

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 主键ID |
| `userId` | Long | 用户ID |
| `bookId` | Long | 图书ID |
| `currentPage` | Integer | 当前页码 |
| `chapter` | String | 当前章节 |
| `totalPages` | Integer | 总页数 |
| `percentage` | BigDecimal | 阅读百分比 |
| `readDuration` | Integer | 总阅读时长（秒） |
| `device` | String | 设备信息 |
| `updateTime` | LocalDateTime | 更新时间 |

#### Recommendation — 推荐记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `recommendId` | Long | 推荐记录ID |
| `userId` | Long | 用户ID |
| `bookId` | Long | 图书ID |
| `recommendType` | String | 推荐类型 |
| `score` | BigDecimal | 推荐分数 |
| `reason` | String | 推荐理由 |
| `recommendTime` | LocalDateTime | 推荐时间 |
| `isClicked` | Boolean | 是否已点击 |
| `isCollected` | Boolean | 是否已收藏 |

#### PurchaseRecord — 购买记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `purchaseId` | Long | 购买记录ID |
| `userId` | Long | 用户ID |
| `bookId` | Long | 图书ID |
| `purchaseTime` | LocalDateTime | 购买时间 |
| `purchasePlatform` | String | 购买平台 |
| `purchasePrice` | BigDecimal | 购买价格 |
| `purchaseUrl` | String | 购买链接 |

#### PurchaseLinks — 购书链接

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `linkId` | Long | 链接ID |
| `bookId` | Long | 图书ID |
| `platform` | String | 平台名称 |
| `url` | String | 购买链接 |
| `price` | BigDecimal | 平台价格 |
| `createTime` | LocalDateTime | 创建时间 |

#### BookRatings — 图书评分

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `ratingId` | Long | 评分ID |
| `userId` | Long | 用户ID |
| `bookId` | Long | 图书ID |
| `score` | Byte | 评分（1-5） |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

#### BookSimilarity — 图书相似度矩阵

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 主键ID |
| `bookIdA` | Long | 图书A的ID |
| `bookIdB` | Long | 图书B的ID |
| `similarity` | BigDecimal | 相似度值 |
| `coCount` | Integer | 共现次数 |
| `computeTime` | LocalDateTime | 计算时间 |

#### UserBehavior — 用户行为

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `behaviorId` | Long | 行为ID |
| `userId` | Long | 用户ID |
| `bookId` | Long | 图书ID |
| `behaviorType` | String | 行为类型：view/search/rate/collect/review/read |
| `behaviorTime` | LocalDateTime | 行为时间 |
| `behaviorValue` | String | 行为值 |
| `duration` | Integer | 持续时长 |

#### UserProfile — 用户画像

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 主键ID |
| `userId` | Long | 用户ID |
| `tagVector` | String | 标签权重向量（JSON） |
| `preferredAuthors` | String | 偏好作者列表（JSON） |
| `preferredCategories` | String | 偏好分类列表（JSON） |
| `lastUpdated` | LocalDateTime | 最后更新时间 |

#### Categories — 分类

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `categoryId` | Long | 分类ID |
| `name` | String | 分类名称 |
| `parentId` | Long | 父分类ID |
| `description` | String | 分类描述 |
| `createTime` | LocalDateTime | 创建时间 |

#### KgSyncLog — 知识图谱同步日志

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long | 主键ID |
| `syncType` | String | 同步类型 |
| `status` | String | 状态 |
| `recordsSynced` | Integer | 同步记录数 |
| `errorMessage` | String | 错误信息 |
| `startTime` | LocalDateTime | 开始时间 |
| `endTime` | LocalDateTime | 结束时间 |

---

## 二十六、错误码一览

### 认证/权限类 (1xxx)

| 错误码 | 说明 |
| --- | --- |
| 1001 | 未认证，请先登录 |
| 1002 | Token已过期，请重新登录 |
| 1003 | Token无效 |
| 1004 | 权限不足 |
| 1005 | 权限不足，无法操作此资源 |

### 用户类 (2xxx)

| 错误码 | 说明 |
| --- | --- |
| 2001 | 用户名已存在 |
| 2002 | 用户不存在 |
| 2003 | 用户名或密码错误 |
| 2004 | 用户已被禁用 |

### 图书类 (3xxx)

| 错误码 | 说明 |
| --- | --- |
| 3001 | 图书不存在 |
| 3002 | 图书已存在 |

### 通用操作类 (4xxx)

| 错误码 | 说明 |
| --- | --- |
| 4001 | 参数错误 |
| 4002 | 数据不存在 |
| 4003 | 操作失败 |
| 4004 | 书评不存在 |
| 4005 | 该书评已审核，无法重复操作 |

### 系统类 (5xxx)

| 错误码 | 说明 |
| --- | --- |
| 5001 | 系统内部错误 |
| 5002 | 服务暂不可用 |

### 知识图谱类 (6xxx)

| 错误码 | 说明 |
| --- | --- |
| 6001 | 图谱构建任务正在进行中 |
| 6002 | 图谱节点不存在 |
| 6003 | Cypher查询语句不合法 |
| 6004 | 图谱关系已存在 |

### 推荐类 (7xxx)

| 错误码 | 说明 |
| --- | --- |
| 7001 | 推荐权重配置无效，权重之和必须为1 |
| 7002 | 暂无推荐数据 |
| 7003 | 推荐源图书不存在 |

### 用户导入类 (8xxx)

| 错误码 | 说明 |
| --- | --- |
| 8001 | 用户不存在 |
| 8002 | CSV文件解析失败 |
| 8003 | CSV文件格式错误 |
| 8004 | 批量导入部分失败 |

### 电子书类 (9xxx)

| 错误码 | 说明 |
| --- | --- |
| 9001 | 该书未上传电子书 |
| 9002 | 电子书文件不存在 |
| 9003 | 电子书格式不支持，仅支持 epub/pdf/txt |
| 9004 | 电子书解析失败 |
| 9005 | 电子书文件过大，最大支持 100MB |

### 二级操作密码类 (10xxx)

| 错误码 | 说明 |
| --- | --- |
| 10001 | 该操作为高危操作，需提供二级操作密码 |
| 10002 | 二级操作密码不正确 |
| 10003 | 尚未设置二级操作密码，请先设置 |

### 图片类 (11xxx)

| 错误码 | 说明 |
| --- | --- |
| 11001 | 图片格式不支持，仅支持 jpg/jpeg/png/gif/webp |
| 11002 | 图片文件过大，最大支持 5MB |

---

## 统计汇总

| 分类 | 数量 |
| --- | --- |
| Spring Boot 用户端接口 | **43 个** |
| Spring Boot 管理端接口 | **33 个** |
| FastAPI 微服务接口 | **10 个** |
| **总计** | **86 个** |

### 需要二级密码验证的接口（共 6 个）

| 接口 | 路径 |
| --- | --- |
| 删除图书 | `DELETE /api/v1/admin/books/{bookId}` |
| 图书批量导入 | `POST /api/v1/admin/books/batch` |
| 修改用户 | `PUT /api/v1/admin/users/{userId}` |
| 删除用户 | `DELETE /api/v1/admin/users/{userId}` |
| 购书链接批量导入 | `POST /api/v1/admin/purchase-links/batch` |
| 修改推荐权重配置 | `PUT /api/v1/admin/recommend/config` |
