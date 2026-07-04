06 - 基于知识图谱的个性化荐书系统 - 接口文档
# 图书推荐系统 — API 接口文档
> Spring Boot 服务 `http://localhost:8080` | FastAPI 服务 `http://localhost:8000`
> 认证方式: JWT Bearer Token（除标注"无需登录"外，所有接口需携带 `Authorization: Bearer <token>`）
> 接口文档页面: Knife4j `http://localhost:8080/doc.html` | Swagger `http://localhost:8000/docs`
---
## 一、权限规则总览
---
## 二、认证模块 `/api/v1/auth`
---
## 三、图书浏览模块 `/api/v1/books`
---
## 四、图书评分 `/api/v1/books/{bookId}/rating`
---
## 五、分类模块 `/api/v1/categories`
---
## 六、首页聚合 `/api/v1`
---
## 七、推荐引擎 `/api/v1/recommend`
---
## 八、用户模块 `/api/v1/users`
---
## 九、推荐历史 `/api/v1/users/me/recommendations`
---
## 十、行为历史 `/api/v1/users/me/behaviors`
---
## 十一、阅读管理 `/api/v1/reading`
---
## 十二、书架管理 `/api/v1/shelves`
---
## 十三、书评模块
---
## 十四、购买记录 `/api/v1/purchases`
---
## 十五、知识图谱 `/api/v1/kg`
---
## 十六、管理员 — 图书管理 `/api/v1/admin/books`
权限: BOOK_ADMIN 或 ADMIN
| 3 | DELETE | `/{bookId}` | — | 删除图书 | 需要 |
| 4 | POST | `/batch` | Form: file (CSV) | 批量导入图书 | 需要 |
| 5 | PUT | /{bookId}/preview | Body: {previewContent} | 更新试读内容 | — |
| 6 | POST | /{bookId}/ebook | Form: file (EPUB/PDF) | 上传电子书 | — |
---
## 十七、管理员 — 用户管理 `/api/v1/admin/users`
权限: OPS_ADMIN 或 ADMIN
| 3 | PUT | `/{userId}` | Body: `{nickname, email, phone, role, status}` | 修改用户 | 需要 |
| 4 | DELETE | `/{userId}` | — | 删除用户 | 需要 |
| 5 | GET | /{userId}/reading/stats | — | 查看用户阅读统计 | — |
| 6 | GET | /{userId}/reading/history | status?, page=1, size=20 | 查看用户阅读历史 | — |
---
## 十八、管理员 — 书评审核 `/api/v1/admin/reviews`
权限: COMMUNITY_ADMIN 或 ADMIN
---
## 十九、管理员 — 系统统计 `/api/v1/admin/stats`
权限: OPS_ADMIN 或 ADMIN
---
## 二十、管理员 — 推荐配置 `/api/v1/admin/recommend`
权限: OPS_ADMIN 或 ADMIN
| 2 | PUT | `/config` | Body: `{kg_weight, itemcf_weight, hot_weight, new_weight}` | 更新权重 | 需要 |
---
## 二十一、管理员 — 购书链接 `/api/v1/admin/purchase-links`
权限: BOOK_ADMIN 或 ADMIN
| 1 | POST | `/batch` | Form: file (CSV) | 批量导入购书链接 | 需要 |
---
## 二十二、管理员 — 知识图谱 `/api/v1/admin/kg`
权限: BOOK_ADMIN 或 ADMIN
| 1 | POST | `/build` | Body: `{forceRebuild?}` | 触发图谱构建 | 需要 |
| 2 | GET | /build/status/{taskId} | — | 构建任务状态 | — |
| 3 | POST | `/relations` | Body: `{sourceType, sourceId, targetType, targetId, relationType, action}` | 编辑关系 | 需要 |
| 4 | POST | `/relations/batch` | Form: file (CSV) | 批量导入关系 | 需要 |
| 5 | POST | `/cypher` | Body: `{cypher, params?, limit?}` | Cypher 调试 | 需要 |
| 6 | POST | `/entities/{type}` | Body: `{name}` | 新增实体 | 需要 |
| 7 | GET | /entities/{type} | page=1, size=20 | 分页查询实体 | — |
| 8 | GET | /entities/{type}/{name} | — | 实体详情 | — |
| 9 | PUT | `/entities/{type}/{name}` | Body: `{newName}` | 重命名实体 | 需要 |
| 10 | DELETE | `/entities/{type}/{name}` | — | 删除实体 | 需要 |
---
## 二十三、管理员 — 定时任务 `/api/v1/admin/tasks`
权限: ADMIN
---
## 二十四、Python FastAPI 服务（端口 8000）
### 推荐服务 `/api/v1/recommend`
### 知识图谱 `/api/v1/kg`
### 离线任务 `/api/v1/tasks`
---
## 二十五、统计汇总
| Spring Boot 接口 | 67 个 |
| FastAPI 接口 | 10 个 |
| 总计 | 77 个 |
### 按角色统计
| BOOK_ADMIN | 图书管理(6) + 购书链接(1) + KG管理(10) = 17 |
| OPS_ADMIN | 统计(5) + 推荐配置(2) + 用户管理(6) = 13 |
| COMMUNITY_ADMIN | 书评审核(6) = 6 |
### 二级密码验证接口（共 13 个）
删除/修改/批量操作等敏感接口需额外验证二级操作密码。
{
"username": "reader_zhang",
"password": "Abc123!@#",
"confirmPassword": "Abc123!@#",
"nickname": "小张",
"preferenceTags": ["科幻", "悬疑"]
}
响应示例:
{
"code": 0,
"message": "注册成功",
"data": {
"userId": 10086,
"username": "reader_zhang",
"nickname": "小张",
"accessToken": "eyJhbGciOiJIUzI1NiIs...",
"refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
}
3.1.2 用户登录
POST /api/v1/auth/login
权限: 无需登录
请求体:
{
"username": "reader_zhang",
"password": "Abc123!@#"
}
响应示例:
{
"code": 0,
"message": "登录成功",
"data": {
"userId": 10086,
"username": "reader_zhang",
"nickname": "小张",
"accessToken": "eyJhbGciOiJIUzI1NiIs...",
"refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
}
3.1.3 刷新 Token
POST /api/v1/auth/refresh
权限: 无需登录（但需携带 refreshToken）
请求体:
{
"refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
响应:
{
"code": 0,
"data": {
"accessToken": "new_access_token",
"refreshToken": "new_refresh_token"
}
}
3.1.4 获取当前用户信息
GET /api/v1/users/me
权限: 登录用户
响应:
{
"code": 0,
"data": {
"userId": 10086,
"username": "reader_zhang",
"nickname": "小张",
"avatar": "https://...",
"preferenceTags": ["科幻", "悬疑"],
"createdAt": "2026-06-01T10:00:00Z"
}
}
3.1.5 更新用户偏好标签
PUT /api/v1/users/me/preferences
权限: 登录用户
请求体:
{
"preferenceTags": ["科幻", "历史", "哲学"]
}
响应: {"code":0,"message":"更新成功"}
3.1.6 管理员 - 用户管理（CRUD）
GET /api/v1/admin/users— 分页查询用户列表
POST /api/v1/admin/users— 新增用户（管理员可设置任意角色）
PUT /api/v1/admin/users/{userId}— 修改用户信息（含密码重置）
DELETE /api/v1/admin/users/{userId}— 删除用户
权限: 管理员
请求参数与响应遵循通用分页/资源格式。
3.2 图书管理模块（管理员）
3.2.1 单本图书录入
POST /api/v1/admin/books
权限: 管理员
请求体:
{
"title": "三体",
"author": "刘慈欣",
"isbn": "9787536692930",
"publisher": "重庆出版社",
"category": "科幻",
"tags": ["科幻", "宇宙", "文明"],
"summary": "文化大革命如火如荼进行的同时...",
"coverUrl": "https://...",
"publishDate": "2008-01-01"
}
响应:
{
"code": 0,
"data": {
"bookId": 201,
"title": "三体"
}
}
3.2.2 批量导入图书
POST /api/v1/admin/books/batch
权限: 管理员
Content-Type: multipart/form-data
参数: file— CSV 或 Excel 文件，字段与单本录入一致
响应:
{
"code": 0,
"data": {
"successCount": 98,
"failCount": 2,
"errors": [
{"row": 3, "reason": "ISBN已存在"},
{"row": 17, "reason": "作者字段为空"}
]
}
}
3.2.3 编辑图书信息
PUT /api/v1/admin/books/{bookId}
权限: 管理员
请求体: 同单本录入，支持部分字段更新
3.2.4 删除图书
DELETE /api/v1/admin/books/{bookId}
权限: 管理员
删除同时清理知识图谱中的关联关系。
3.3 图书检索与浏览
3.3.1 多维度搜索图书
GET /api/v1/books/search?keyword=三体&page=1&size=20
权限: 无需登录
查询参数:
响应: 分页列表，每项包含 bookId, title, author,      coverUrl, avgRating, matchType（书名/作者/ISBN）
3.3.2 按分类/标签筛选图书
GET /api/v1/books?category=科幻&tag=硬科幻&sortBy=hot&order=desc&page=1&size=20
权限: 无需登录
查询参数:
3.3.3 获取图书详情
GET /api/v1/books/{bookId}
权限: 无需登录
响应:
{
"code": 0,
"data": {
"bookId": 201,
"title": "三体",
"author": "刘慈欣",
"publisher": "重庆出版社",
"isbn": "9787536692930",
"category": "科幻",
"tags": ["科幻", "宇宙", "文明"],
"summary": "...",
"coverUrl": "https://...",
"publishDate": "2008-01-01",
"avgRating": 4.8,
"ratingCount": 10234,
"pages": 302,
"seriesInfo": { "seriesName": "三体系列", "seriesOrder": 1 },
"relatedBooks": [ { "bookId": 202, "title": "三体II·黑暗森林" } ]  // 图谱关联
}
}
3.4 图书试读
3.4.1 获取试读内容
GET /api/v1/books/{bookId}/preview
权限: 无需登录（未登录用户仅返回前3页，登录用户返回前10页或第一章）
查询参数:
响应:
{
"code": 0,
"data": {
"bookId": 201,
"format": "pdf",
"totalPreviewPages": 10,
"contentBase64": "/9j/4AAQSkZJRg..."  // 或返回文本内容（epub时）
}
}
3.5 知识图谱模块
3.5.1 触发图谱构建（管理员）
POST /api/v1/admin/kg/build
权限: 管理员
请求体（可选）:
{
"bookIds": [201, 203, 205],  // 留空则对所有未构建图书执行
"forceRebuild": false         // 是否覆盖已有图谱关系
}
响应:
{
"code": 0,
"data": {
"taskId": "kg_task_001",
"status": "processing",
"estimatedTimeSeconds": 45
}
}
3.5.2 图谱关系查询（Cypher）
POST /api/v1/kg/query
权限: 登录用户
请求体:
{
"cypher": "MATCH (b:Book {title:'三体'})-[r]->(n) RETURN b, r, n LIMIT 20"
}
响应:
{
"code": 0,
"data": {
"columns": ["b", "r", "n"],
"rows": [
[{"id":"book:201","labels":["Book"],"properties":{"title":"三体"}},
{"type":"written_by","properties":{}},
{"id":"author:1","labels":["Author"],"properties":{"name":"刘慈欣"}}]
]
}
}
3.5.3 图谱可视化数据
GET /api/v1/kg/graph/{entityId}?depth=2
权限: 无需登录
参数:
响应:
{
"nodes": [
{"id":"book:201","label":"三体","type":"book"},
{"id":"author:1","label":"刘慈欣","type":"author"},
{"id":"tag:scifi","label":"科幻","type":"tag"}
],
"edges": [
{"source":"author:1","target":"book:201","relation":"撰写"},
{"source":"book:201","target":"tag:scifi","relation":"属于"}
]
}
3.5.4 手动编辑图谱关系（管理员）
POST /api/v1/admin/kg/relations
权限: 管理员
请求体:
{
"action": "add",  // add / remove / update
"sourceEntityId": "book:201",
"targetEntityId": "author:1",
"relationType": "written_by"
}
3.6 个性化推荐模块
3.6.1 获取首页推荐
GET /api/v1/recommend/home
权限: 登录用户（未登录用户返回热门/新书兜底）
响应:
{
"code": 0,
"data": [
{
"recId": "rec_001",
"bookId": 301,
"title": "银河帝国：基地",
"author": "艾萨克·阿西莫夫",
"coverUrl": "...",
"matchScore": 93.5,
"reason": "因为你喜欢《三体》，同属科幻题材，且均为太空史诗风格",
"reasonPath": ["book:201", "tag:scifi", "book:301"]
},
...
]
}
未登录用户响应示例（无 reason）：
{
"code": 0,
"data": [
{ "bookId": 401, "title": "活着", "author": "余华", "coverUrl": "...", "matchScore": 0 }
]
}
3.6.2 获取相似读物
GET /api/v1/books/{bookId}/similar?page=1&size=10
权限: 无需登录（登录用户返回个性化相似，未登录返回全局相似）
响应: 推荐列表（同首页推荐格式）
3.6.3 获取延伸阅读
GET /api/v1/books/{bookId}/extended
权限: 登录用户
说明: 基于多跳路径推理（至少3跳），返回与当前图书间接关联的高质量图书
响应: 推荐列表
3.6.4 获取热门推荐
GET /api/v1/recommend/hot?days=30&limit=20
权限: 无需登录
响应: 图书列表（无 reason）
3.6.5 获取新书推荐
GET /api/v1/recommend/new?months=3&limit=20
权限: 无需登录
响应: 图书列表
3.6.6 推荐解释详情
GET /api/v1/recommend/{recId}/explain
权限: 登录用户
响应:
{
"code": 0,
"data": {
"recId": "rec_001",
"reasonText": "因为你喜欢《三体》，同属科幻题材，且均为太空史诗风格",
"pathNodes": [
{"id":"book:201","label":"三体","type":"book"},
{"id":"tag:scifi","label":"科幻","type":"tag"},
{"id":"book:301","label":"银河帝国：基地","type":"book"}
]
}
}
3.7 用户画像模块
3.7.1 获取我的画像
GET /api/v1/users/me/profile
权限: 登录用户
响应:
{
"code": 0,
"data": {
"tagVector": [
{"tag":"科幻","weight":0.75},
{"tag":"历史","weight":0.55},
{"tag":"哲学","weight":0.40}
],
"preferredAuthors": ["刘慈欣","阿西莫夫"],
"preferredCategories": ["科幻","历史"],
"lastUpdated": "2026-06-25T08:00:00Z"
}
}
3.7.2 手动触发画像更新
POST /api/v1/users/me/profile/refresh
权限: 登录用户
说明: 强制重新计算画像（正常情况系统每天自动更新一次）
3.8 书架管理模块
3.8.1 创建书架
POST /api/v1/shelves
权限: 登录用户
请求体:
{
"name": "想读",
"description": "今年计划阅读的书"
}
3.8.2 获取书架列表
GET /api/v1/shelves
权限: 登录用户
响应:
{
"code": 0,
"data": [
{"shelfId":1,"name":"想读","bookCount":5},
{"shelfId":2,"name":"在读","bookCount":2},
{"shelfId":3,"name":"已读","bookCount":12}
]
}
3.8.3 向书架添加图书
POST /api/v1/shelves/{shelfId}/books
请求体:
{
"bookId": 201,
"readingStatus": "want_to_read"  // want_to_read / reading / finished
}
3.8.4 移动图书到其他书架
PUT /api/v1/shelves/{shelfId}/books/{bookId}
请求体:
{
"targetShelfId": 2,
"readingStatus": "reading"
}
3.8.5 删除书架
DELETE /api/v1/shelves/{shelfId}
说明: 删除书架不会删除其中的图书，图书变为“未分类”状态。
3.9 阅读进度同步模块
3.9.1 同步阅读进度
PUT /api/v1/reading/progress
权限: 登录用户
请求体:
{
"bookId": 201,
"currentPage": 152,
"percentage": 48.5,
"device": "web_chrome"
}
3.9.2 获取阅读进度
GET /api/v1/reading/progress/{bookId}
权限: 登录用户
响应:
{
"code": 0,
"data": {
"bookId": 201,
"currentPage": 152,
"percentage": 48.5,
"updatedAt": "2026-06-25T20:30:00Z"
}
}
3.9.3 获取阅读历史
GET /api/v1/reading/history?page=1&size=20
权限: 登录用户
响应: 分页列表，按更新时间倒序，每项包含 bookId, title,      progress, lastReadAt。
3.10 书评社区模块
3.10.1 发布书评
POST /api/v1/books/{bookId}/reviews
权限: 登录用户
请求体:
{
"content": "这是一本改变世界观的好书！\n\n推荐理由: 情节紧凑，思想深刻。",
"markdown": true
}
响应:
{
"code": 0,
"data": {
"reviewId": 501,
"status": "pending_audit"  // 待审核
}
}
3.10.2 删除自己未审核书评
DELETE /api/v1/reviews/{reviewId}
权限: 登录用户（仅可删除自己未审核的书评）
3.10.3 获取图书书评列表
GET /api/v1/books/{bookId}/reviews?sortBy=likes&page=1&size=10
权限: 无需登录
查询参数:
3.10.4 点赞书评
POST /api/v1/reviews/{reviewId}/like
权限: 登录用户
说明: 再次调用取消点赞
3.10.5 回复书评
POST /api/v1/reviews/{reviewId}/reply
请求体:
{
"content": "同意！这本书我也很喜欢。"
}
3.11 书评审核模块（管理员）
3.11.1 获取待审核书评
GET /api/v1/admin/reviews/pending?page=1&size=20
权限: 管理员
3.11.2 审核书评
PUT /api/v1/admin/reviews/{reviewId}/audit
请求体:
{
"action": "approve",   // approve / reject / delete
"reason": "含有广告链接"  // 驳回或删除时必填
}
3.12 图书评分模块
3.12.1 提交评分
POST /api/v1/books/{bookId}/rating
权限: 登录用户
请求体:
{
"score": 5  // 1~5 整数
}
说明: 用户对同一本书只能评分一次，再次调用更新评分。
3.12.2 获取图书评分
GET /api/v1/books/{bookId}/rating
权限: 无需登录
响应:
{
"code": 0,
"data": {
"avgRating": 4.78,
"ratingCount": 2048,
"distribution": {
"1": 12,
"2": 28,
"3": 156,
"4": 520,
"5": 1332
}
}
}
3.13 购书链接模块
3.13.1 获取购书链接
GET /api/v1/books/{bookId}/purchase-links
权限: 无需登录
响应:
{
"code": 0,
"data": [
{"platform": "当当", "url": "https://www.dangdang.com/...", "price": 39.9},
{"platform": "京东", "url": "https://item.jd.com/...", "price": 38.5}
]
}
3.14 系统管理模块（管理员）
3.14.1 推荐算法权重配置
PUT /api/v1/admin/recommend/config
请求体:
{
"weights": {
"knowledgeGraph": 0.35,
"collaborativeFiltering": 0.35,
"hot": 0.15,
"new": 0.15
}
}
说明: 四个权重之和必须等于1。
3.14.2 获取统计数据
GET /api/v1/admin/stats?startDate=2026-06-01&endDate=2026-06-25
响应:
{
"code": 0,
"data": {
"totalUsers": 52300,
"totalBooks": 128000,
"totalReviews": 34000,
"dailyActiveUsers": 8200,
"recommendClickRate": 0.235,
"topCategories": [{"name":"科幻","count":1500},{"name":"文学","count":1200}]
}
}
4. 附录
4.1 公共错误响应示例
{
"code": 1002,
"message": "Token已过期，请重新登录",
"data": null
}
4.2 接口版本管理
当前版本 v1，路径前缀 /api/v1/
向后兼容的变更（如新增字段）不升级版本号
破坏性变更（如删除字段、修改语义）需升级版本号为 /api/v2/
4.3 速率限制
普通用户：每分钟 60 次请求
管理员：每分钟 120 次请求
超出限制返回 HTTP 429 Too Many Requests
点击图片可查看完整电子表格
三、图书浏览模块 BooksController
基础路径：/api/v1/books  |  GET 请求无需登录
点击图片可查看完整电子表格
四、图书分类模块 CategoriesController
基础路径：/api/v1/categories  |  无需登录
点击图片可查看完整电子表格
五、用户模块 UsersController
基础路径：/api/v1/users  |  需登录
点击图片可查看完整电子表格
六、评分模块 RatingController
基础路径：/api/v1/books/{bookId}/rating
点击图片可查看完整电子表格
七、书评模块 ReviewsController
基础路径：见各接口  |  发布/删除/点赞/回复需登录，查询无需登录
点击图片可查看完整电子表格
八、书架模块 ShelvesController
基础路径：/api/v1/shelves  |  需登录
点击图片可查看完整电子表格
九、阅读管理模块 ReadingController
基础路径：/api/v1/reading  |  需登录
点击图片可查看完整电子表格
十、推荐引擎模块 RecommendController
基础路径：/api/v1/recommend  |  GET 请求无需登录，登录用户落库推荐记录
点击图片可查看完整电子表格
十一、知识图谱模块 KgController
基础路径：/api/v1/kg
点击图片可查看完整电子表格
十二、购买记录模块 PurchaseRecordsController
基础路径：/api/v1/purchases  |  需登录
点击图片可查看完整电子表格
十三、用户行为历史 UserBehaviorsController
基础路径：/api/v1/users/me/behaviors  |  需登录
点击图片可查看完整电子表格
十四、推荐历史 RecommendationsController
基础路径：/api/v1/users/me/recommendations  |  需登录
点击图片可查看完整电子表格
十五、管理员-图书管理 AdminBooksController
基础路径：/api/v1/admin/books  |  角色：BOOK_ADMIN, ADMIN
点击图片可查看完整电子表格
十六、管理员-用户管理 AdminUsersController
基础路径：/api/v1/admin/users  |  角色：OPS_ADMIN, ADMIN
点击图片可查看完整电子表格
十七、管理员-书评审核 AdminReviewsController
基础路径：/api/v1/admin/reviews  |  角色：COMMUNITY_ADMIN, ADMIN
点击图片可查看完整电子表格
十八、管理员-知识图谱 AdminKgController
基础路径：/api/v1/admin/kg  |  角色：BOOK_ADMIN, ADMIN
点击图片可查看完整电子表格
十九、管理员-系统统计 AdminStatsController
基础路径：/api/v1/admin/stats  |  角色：OPS_ADMIN, ADMIN
点击图片可查看完整电子表格
二十、管理员-推荐配置 AdminRecommendController
基础路径：/api/v1/admin/recommend  |  角色：OPS_ADMIN, ADMIN
点击图片可查看完整电子表格
二十一、管理员-定时任务 AdminTasksController
基础路径：/api/v1/admin/tasks  |  角色：ADMIN
点击图片可查看完整电子表格
二十二、管理员-购书链接 AdminPurchaseLinksController
基础路径：/api/v1/admin/purchase-links  |  角色：BOOK_ADMIN, ADMIN
点击图片可查看完整电子表格
附录A：角色权限说明
系统采用基于角色的访问控制（RBAC），共定义以下角色：
USER：普通用户，可使用所有读者端功能
BOOK_ADMIN：图书管理员，管理图书资源、电子书、购书链接、知识图谱
OPS_ADMIN：运营管理员，管理系统统计、推荐配置、用户管理
COMMUNITY_ADMIN：社区管理员，管理书评审核、优质标记、违规清理
ADMIN：超级管理员，拥有所有权限
附录B：错误码说明
点击图片可查看完整电子表格
附录C：接口调试
Swagger UI：http://localhost:8080/swagger-ui.html
Knife4j 文档：http://localhost:8080/doc.html
Druid 监控：http://localhost:8080/druid/（admin/admin）
文档结束

|  |
| --- |


|  |
| --- |


| 路径规则 | 角色要求 |
| --- | --- |
| /api/v1/auth/** | 无需登录 |
| GET /api/v1/books/** | 无需登录 |
| GET /api/v1/categories/** | 无需登录 |
| GET /api/v1/kg/graph/**, /api/v1/kg/stats | 无需登录 |
| /api/v1/recommend/** | 无需登录 |
| /api/v1/banners | 无需登录 |
| /api/v1/admin/books/**, /api/v1/admin/purchase-links/**, /api/v1/admin/kg/** | BOOK_ADMIN 或 ADMIN |
| /api/v1/admin/stats/**, /api/v1/admin/recommend/**, /api/v1/admin/users/** | OPS_ADMIN 或 ADMIN |
| /api/v1/admin/reviews/** | COMMUNITY_ADMIN 或 ADMIN |
| /api/v1/admin/**（兜底） | ADMIN |
| 其余所有 /api/v1/** | 需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | POST | /register | Body: {username, password, nickname?} | 用户注册 | 无需登录 |
| 2 | POST | /login | Body: {username, password} | 用户登录，返回 JWT Token | 无需登录 |
| 3 | POST | /refresh | Body: {refreshToken} | 刷新 Token | 无需登录 |
| 4 | POST | /second-password | Body: {currentPassword, newPassword} | 设置/更新二级操作密码 | 需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | GET | /search | keyword?, page=1, size=20 | 多维度搜索（书名/作者/ISBN） | 无需登录 |
| 2 | GET | / | categoryId?, tag?, sortBy=createTime, order=desc, page=1, size=20 | 分类/标签筛选+排序 | 无需登录 |
| 3 | GET | /{bookId} | 路径: bookId | 图书详情（含标签、购书链接） | 无需登录 |
| 4 | GET | /{bookId}/preview | 路径: bookId | 试读（未登录限500字，登录可读全部） | 无需登录 |
| 5 | GET | /{bookId}/ebook | 路径: bookId | 电子书（未登录前3章，登录前10章） | 无需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | POST | / | Body: {score} | 提交评分 | 需登录 |
| 2 | GET | / | — | 获取评分统计 | 无需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | GET | / | — | 所有分类列表 | 无需登录 |
| 2 | GET | /{categoryId} | 路径: categoryId | 分类详情 | 无需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | GET | /banners | limit=6 | 轮播图（热门图书） | 无需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | GET | /home | limit=10 | 首页推荐（登录=个性化，未登录=热门/新书） | 无需登录 |
| 2 | GET | /hot | days=90, limit=10 | 热门图书 | 无需登录 |
| 3 | GET | /new | months=6, limit=10 | 新书推荐 | 无需登录 |
| 4 | GET | /{bookId}/similar | limit=10 | 相似读物（KG 1-2跳） | 无需登录 |
| 5 | GET | /{bookId}/extended | limit=10 | 延伸阅读（KG 3跳+） | 无需登录 |
| 6 | POST | /{recommendId}/click | — | 推荐点击回调 | 需登录 |
| 7 | GET | /{bookId}/explain | — | 推荐解释详情 | 无需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | GET | /me | — | 当前用户信息 | 需登录 |
| 2 | PUT | /me/preferences | Body: {preferenceTags: [...]} | 更新偏好标签 | 需登录 |
| 3 | GET | /me/profile | — | 用户画像 | 需登录 |
| 4 | POST | /me/profile/refresh | — | 刷新用户画像 | 需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | GET | / | clicked?, collected?, page=1, size=20 | 推荐历史查询 | 需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | GET | / | type? (view/search/rate/collect/review), page=1, size=20 | 行为历史 | 需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | PUT | /progress | Body: {bookId, currentPage, chapter, totalPages, readDuration} | 同步阅读进度 | 需登录 |
| 2 | GET | /progress/{bookId} | 路径: bookId | 获取阅读进度 | 需登录 |
| 3 | GET | /history | status? (reading/finished), page=1, size=20 | 阅读历史 | 需登录 |
| 4 | GET | /stats | — | 阅读统计（周/月时长、已读/在读） | 需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | POST | / | Query: name (必填), description? | 创建书架 | 需登录 |
| 2 | GET | / | — | 书架列表 | 需登录 |
| 3 | GET | /{shelfId} | 路径: shelfId | 书架详情+图书列表 | 需登录 |
| 4 | POST | /{shelfId}/books | Query: bookId (必填), readingStatus=0 | 添加图书到书架 | 需登录 |
| 5 | PUT | /{shelfId}/books/{bookId} | Query: readingStatus (必填) | 更新阅读状态(0-未读/1-在读/2-已读) | 需登录 |
| 6 | DELETE | /{shelfId}/books/{bookId} | — | 从书架移除 | 需登录 |
| 7 | DELETE | /{shelfId} | — | 删除书架 | 需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | POST | /api/v1/books/{bookId}/reviews | Body: {content, markdown?} | 发布书评 | 需登录 |
| 2 | GET | /api/v1/books/{bookId}/reviews | sortBy=likes, page=1, size=10 | 书评列表（已审核通过） | 需登录 |
| 3 | DELETE | /api/v1/reviews/{reviewId} | — | 删除自己的书评 | 需登录 |
| 4 | POST | /api/v1/reviews/{reviewId}/like | — | 点赞/取消（toggle） | 需登录 |
| 5 | POST | /api/v1/reviews/{reviewId}/reply | Body: {content} | 回复书评 | 需登录 |
| 6 | GET | /api/v1/reviews/{reviewId}/replies | — | 书评回复列表 | 需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | POST | / | Body: {bookId, platform, url, price?} | 记录购买意向 | 需登录 |
| 2 | GET | / | bookId?, platform?, page=1, size=20 | 购买记录列表 | 需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 1 | POST | /query | Body: {cypher, limit=100} | Cypher 只读查询 | 需登录 |
| 2 | GET | /graph/{entityType}/{entityId} | Query: depth=2 | 图谱可视化数据 | 无需登录 |
| 3 | GET | /stats | — | 图谱统计 | 无需登录 |


| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
| --- | --- | --- | --- | --- | --- |
| 1 | POST | / | Body: {title, author, isbn, publisher, ...} | 单本录入 | — |
| 2 | PUT | /{bookId} | Body: 部分字段 | 编辑图书 | — |


| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
| --- | --- | --- | --- | --- | --- |
| 1 | GET | / | page=1, size=10, keyword?, role?, status? | 用户列表 | — |
| 2 | POST | / | Body: {username, password, nickname, role} | 新增用户 | — |


| # | 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | /pending | page=1, size=20 | 待审核书评列表 |
| 2 | PUT | /{reviewId}/audit | Body: {action} (approve/reject/delete) | 审核书评 |
| 3 | PUT | /{reviewId}/featured | Body: {featured: true/false} | 标记优质书评 |
| 4 | GET | /featured | page=1, size=20 | 优质书评列表 |
| 5 | POST | /batch-delete | Body: {reviewIds: [1,2,3]} | 批量软删除 |
| 6 | GET | /search | keyword?, status?, page=1, size=20 | 搜索书评 |


| # | 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | GET | / | — | 系统统计总览 |
| 2 | GET | /user-growth | — | 用户增长趋势（近30天） |
| 3 | GET | /rating-distribution | — | 评分分布 |
| 4 | GET | /top-books | limit=10 | 热门图书 Top N |
| 5 | GET | /behavior-distribution | — | 行为类型分布 |


| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
| --- | --- | --- | --- | --- | --- |
| 1 | GET | /config | — | 获取推荐权重配置 | — |


| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
| --- | --- | --- | --- | --- | --- |


| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
| --- | --- | --- | --- | --- | --- |


| # | 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | /compute-similarity | — | 手动触发相似度矩阵计算 |
| 2 | POST | /sync-kg | — | 手动触发 KG 数据同步 |
| 3 | GET | /logs | limit=20 | 同步日志查询 |


| # | 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | /home | Body: {userId?, limit=10} | 混合推荐 |
| 2 | GET | /hot | days=90, limit=10 | 热门图书 |
| 3 | GET | /new | months=6, limit=10 | 新书推荐 |
| 4 | GET | /itemcf/{user_id} | limit=10 | ItemCF 协同过滤 |
| 5 | GET | /kg/{user_id} | limit=10 | 知识图谱推荐 |


| # | 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | /query | Body: {cypher, limit=100} | Cypher 查询 |
| 2 | GET | /stats | — | 图谱统计 |
| 3 | GET | /subgraph | bookId?, depth=2, limit=50 | 子图数据 |


| # | 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | POST | /compute-similarity | — | 相似度矩阵计算 |
| 2 | POST | /sync-kg | — | KG 数据同步 |


| 分类 | 数量 |
| --- | --- |


| 角色 | 可访问管理接口 |
| --- | --- |
| ADMIN | 全部（77个中所有） |


| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 唯一用户名，3~20字符，字母数字下划线 |
| password | string | 是 | 8~32位，至少包含大写字母、小写字母、数字、特殊符号中的三种 |
| confirmPassword | string | 是 | 必须与 password 一致 |
| nickname | string | 否 | 默认取 username |
| preferenceTags | array[string] | 否 | 最多5个标签，用于冷启动推荐 |


| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| keyword | string | 是 | 支持书名、作者、ISBN 模糊匹配 |
| page | int | 否 | 默认1 |
| size | int | 否 | 默认20，最大100 |


| 参数 | 类型 | 说明 |
| --- | --- | --- |
| category | string | 分类名称 |
| tag | string | 标签名称（可与category同时使用） |
| sortBy | string | hot（热度）/ rating（评分）/ date（出版时间） |
| order | string | asc / desc |


| 参数 | 类型 | 说明 |
| --- | --- | --- |
| format | string | pdf / epub（默认根据图书格式） |
| page | int | 指定起始页码（可选，用于分页加载） |


| 参数 | 类型 | 说明 |
| --- | --- | --- |
| entityId | string | 实体ID（如 book:201） |
| depth | int | 关联深度，默认2，最大3 |


| 参数 | 类型 | 说明 |
| --- | --- | --- |
| sortBy | string | likes（按点赞数降序）/   newest（按发布时间降序） |
| onlyHighlighted | boolean | 是否仅显示优质书评（管理员标记） |


| 二、认证模块 AuthController
基础路径：/api/v1/auth  |  无需登录 |
| --- |
