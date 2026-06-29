# 图书推荐系统 — API 接口文档

> **Spring Boot 服务** `http://localhost:8080` | **FastAPI 服务** `http://localhost:8000`
>
> **认证方式**: JWT Bearer Token（除标注"无需登录"外，所有接口需携带 `Authorization: Bearer <token>`）
>
> **接口文档页面**: Knife4j `http://localhost:8080/doc.html` | Swagger `http://localhost:8000/docs`

---

## 一、权限规则总览

| 路径规则 | 角色要求 |
|---|---|
| `/api/v1/auth/**` | 无需登录 |
| `GET /api/v1/books/**` | 无需登录 |
| `GET /api/v1/categories/**` | 无需登录 |
| `GET /api/v1/kg/graph/**`, `/api/v1/kg/stats` | 无需登录 |
| `/api/v1/recommend/**` | 无需登录 |
| `/api/v1/banners` | 无需登录 |
| `/api/v1/admin/books/**`, `/api/v1/admin/purchase-links/**`, `/api/v1/admin/kg/**` | BOOK_ADMIN 或 ADMIN |
| `/api/v1/admin/stats/**`, `/api/v1/admin/recommend/**`, `/api/v1/admin/users/**` | OPS_ADMIN 或 ADMIN |
| `/api/v1/admin/reviews/**` | COMMUNITY_ADMIN 或 ADMIN |
| `/api/v1/admin/**`（兜底） | ADMIN |
| 其余所有 `/api/v1/**` | 需登录 |

---

## 二、认证模块 `/api/v1/auth`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | POST | `/register` | Body: `{username, password, nickname?}` | 用户注册 | 无需登录 |
| 2 | POST | `/login` | Body: `{username, password}` | 用户登录，返回 JWT Token | 无需登录 |
| 3 | POST | `/refresh` | Body: `{refreshToken}` | 刷新 Token | 无需登录 |
| 4 | POST | `/second-password` | Body: `{currentPassword, newPassword}` | 设置/更新二级操作密码 | 需登录 |

---

## 三、图书浏览模块 `/api/v1/books`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | GET | `/search` | `keyword?`, `page=1`, `size=20` | 多维度搜索（书名/作者/ISBN） | 无需登录 |
| 2 | GET | `/` | `categoryId?`, `tag?`, `sortBy=createTime`, `order=desc`, `page=1`, `size=20` | 分类/标签筛选+排序 | 无需登录 |
| 3 | GET | `/{bookId}` | 路径: bookId | 图书详情（含标签、购书链接） | 无需登录 |
| 4 | GET | `/{bookId}/preview` | 路径: bookId | 试读（未登录限500字，登录可读全部） | 无需登录 |
| 5 | GET | `/{bookId}/ebook` | 路径: bookId | 电子书（未登录前3章，登录前10章） | 无需登录 |

---

## 四、图书评分 `/api/v1/books/{bookId}/rating`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | POST | `/` | Body: `{score}` | 提交评分 | 需登录 |
| 2 | GET | `/` | — | 获取评分统计 | 无需登录 |

---

## 五、分类模块 `/api/v1/categories`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | GET | `/` | — | 所有分类列表 | 无需登录 |
| 2 | GET | `/{categoryId}` | 路径: categoryId | 分类详情 | 无需登录 |

---

## 六、首页聚合 `/api/v1`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | GET | `/banners` | `limit=6` | 轮播图（热门图书） | 无需登录 |

---

## 七、推荐引擎 `/api/v1/recommend`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | GET | `/home` | `limit=10` | 首页推荐（登录=个性化，未登录=热门/新书） | 无需登录 |
| 2 | GET | `/hot` | `days=90`, `limit=10` | 热门图书 | 无需登录 |
| 3 | GET | `/new` | `months=6`, `limit=10` | 新书推荐 | 无需登录 |
| 4 | GET | `/{bookId}/similar` | `limit=10` | 相似读物（KG 1-2跳） | 无需登录 |
| 5 | GET | `/{bookId}/extended` | `limit=10` | 延伸阅读（KG 3跳+） | 无需登录 |
| 6 | POST | `/{recommendId}/click` | — | 推荐点击回调 | 需登录 |
| 7 | GET | `/{bookId}/explain` | — | 推荐解释详情 | 无需登录 |

---

## 八、用户模块 `/api/v1/users`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | GET | `/me` | — | 当前用户信息 | 需登录 |
| 2 | PUT | `/me/preferences` | Body: `{preferenceTags: [...]}` | 更新偏好标签 | 需登录 |
| 3 | GET | `/me/profile` | — | 用户画像 | 需登录 |
| 4 | POST | `/me/profile/refresh` | — | 刷新用户画像 | 需登录 |

---

## 九、推荐历史 `/api/v1/users/me/recommendations`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | GET | `/` | `clicked?`, `collected?`, `page=1`, `size=20` | 推荐历史查询 | 需登录 |

---

## 十、行为历史 `/api/v1/users/me/behaviors`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | GET | `/` | `type?` (view/search/rate/collect/review), `page=1`, `size=20` | 行为历史 | 需登录 |

---

## 十一、阅读管理 `/api/v1/reading`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | PUT | `/progress` | Body: `{bookId, currentPage, chapter, totalPages, readDuration}` | 同步阅读进度 | 需登录 |
| 2 | GET | `/progress/{bookId}` | 路径: bookId | 获取阅读进度 | 需登录 |
| 3 | GET | `/history` | `status?` (reading/finished), `page=1`, `size=20` | 阅读历史 | 需登录 |
| 4 | GET | `/stats` | — | 阅读统计（周/月时长、已读/在读） | 需登录 |

---

## 十二、书架管理 `/api/v1/shelves`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | POST | `/` | Query: `name` (必填), `description?` | 创建书架 | 需登录 |
| 2 | GET | `/` | — | 书架列表 | 需登录 |
| 3 | GET | `/{shelfId}` | 路径: shelfId | 书架详情+图书列表 | 需登录 |
| 4 | POST | `/{shelfId}/books` | Query: `bookId` (必填), `readingStatus=0` | 添加图书到书架 | 需登录 |
| 5 | PUT | `/{shelfId}/books/{bookId}` | Query: `readingStatus` (必填) | 更新阅读状态(0-未读/1-在读/2-已读) | 需登录 |
| 6 | DELETE | `/{shelfId}/books/{bookId}` | — | 从书架移除 | 需登录 |
| 7 | DELETE | `/{shelfId}` | — | 删除书架 | 需登录 |

---

## 十三、书评模块

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | POST | `/api/v1/books/{bookId}/reviews` | Body: `{content, markdown?}` | 发布书评 | 需登录 |
| 2 | GET | `/api/v1/books/{bookId}/reviews` | `sortBy=likes`, `page=1`, `size=10` | 书评列表（已审核通过） | 需登录 |
| 3 | DELETE | `/api/v1/reviews/{reviewId}` | — | 删除自己的书评 | 需登录 |
| 4 | POST | `/api/v1/reviews/{reviewId}/like` | — | 点赞/取消（toggle） | 需登录 |
| 5 | POST | `/api/v1/reviews/{reviewId}/reply` | Body: `{content}` | 回复书评 | 需登录 |
| 6 | GET | `/api/v1/reviews/{reviewId}/replies` | — | 书评回复列表 | 需登录 |

---

## 十四、购买记录 `/api/v1/purchases`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | POST | `/` | Body: `{bookId, platform, url, price?}` | 记录购买意向 | 需登录 |
| 2 | GET | `/` | `bookId?`, `platform?`, `page=1`, `size=20` | 购买记录列表 | 需登录 |

---

## 十五、知识图谱 `/api/v1/kg`

| # | 方法 | 路径 | 参数 | 说明 | 权限 |
|---|---|---|---|---|---|
| 1 | POST | `/query` | Body: `{cypher, limit=100}` | Cypher 只读查询 | 需登录 |
| 2 | GET | `/graph/{entityType}/{entityId}` | Query: `depth=2` | 图谱可视化数据 | 无需登录 |
| 3 | GET | `/stats` | — | 图谱统计 | 无需登录 |

---

## 十六、管理员 — 图书管理 `/api/v1/admin/books`

**权限**: BOOK_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
|---|---|---|---|---|---|
| 1 | POST | `/` | Body: `{title, author, isbn, publisher, ...}` | 单本录入 | — |
| 2 | PUT | `/{bookId}` | Body: 部分字段 | 编辑图书 | — |
| 3 | DELETE | `/{bookId}` | — | 删除图书 | **需要** |
| 4 | POST | `/batch` | Form: file (CSV) | 批量导入图书 | **需要** |
| 5 | PUT | `/{bookId}/preview` | Body: `{previewContent}` | 更新试读内容 | — |
| 6 | POST | `/{bookId}/ebook` | Form: file (EPUB/PDF) | 上传电子书 | — |

---

## 十七、管理员 — 用户管理 `/api/v1/admin/users`

**权限**: OPS_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
|---|---|---|---|---|---|
| 1 | GET | `/` | `page=1`, `size=10`, `keyword?`, `role?`, `status?` | 用户列表 | — |
| 2 | POST | `/` | Body: `{username, password, nickname, role}` | 新增用户 | — |
| 3 | PUT | `/{userId}` | Body: `{nickname, email, phone, role, status}` | 修改用户 | **需要** |
| 4 | DELETE | `/{userId}` | — | 删除用户 | **需要** |
| 5 | GET | `/{userId}/reading/stats` | — | 查看用户阅读统计 | — |
| 6 | GET | `/{userId}/reading/history` | `status?`, `page=1`, `size=20` | 查看用户阅读历史 | — |

---

## 十八、管理员 — 书评审核 `/api/v1/admin/reviews`

**权限**: COMMUNITY_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/pending` | `page=1`, `size=20` | 待审核书评列表 |
| 2 | PUT | `/{reviewId}/audit` | Body: `{action}` (approve/reject/delete) | 审核书评 |
| 3 | PUT | `/{reviewId}/featured` | Body: `{featured: true/false}` | 标记优质书评 |
| 4 | GET | `/featured` | `page=1`, `size=20` | 优质书评列表 |
| 5 | POST | `/batch-delete` | Body: `{reviewIds: [1,2,3]}` | 批量软删除 |
| 6 | GET | `/search` | `keyword?`, `status?`, `page=1`, `size=20` | 搜索书评 |

---

## 十九、管理员 — 系统统计 `/api/v1/admin/stats`

**权限**: OPS_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/` | — | 系统统计总览 |
| 2 | GET | `/user-growth` | — | 用户增长趋势（近30天） |
| 3 | GET | `/rating-distribution` | — | 评分分布 |
| 4 | GET | `/top-books` | `limit=10` | 热门图书 Top N |
| 5 | GET | `/behavior-distribution` | — | 行为类型分布 |

---

## 二十、管理员 — 推荐配置 `/api/v1/admin/recommend`

**权限**: OPS_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
|---|---|---|---|---|---|
| 1 | GET | `/config` | — | 获取推荐权重配置 | — |
| 2 | PUT | `/config` | Body: `{kg_weight, itemcf_weight, hot_weight, new_weight}` | 更新权重 | **需要** |

---

## 二十一、管理员 — 购书链接 `/api/v1/admin/purchase-links`

**权限**: BOOK_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
|---|---|---|---|---|---|
| 1 | POST | `/batch` | Form: file (CSV) | 批量导入购书链接 | **需要** |

---

## 二十二、管理员 — 知识图谱 `/api/v1/admin/kg`

**权限**: BOOK_ADMIN 或 ADMIN

| # | 方法 | 路径 | 参数 | 说明 | 二级密码 |
|---|---|---|---|---|---|
| 1 | POST | `/build` | Body: `{forceRebuild?}` | 触发图谱构建 | **需要** |
| 2 | GET | `/build/status/{taskId}` | — | 构建任务状态 | — |
| 3 | POST | `/relations` | Body: `{sourceType, sourceId, targetType, targetId, relationType, action}` | 编辑关系 | **需要** |
| 4 | POST | `/relations/batch` | Form: file (CSV) | 批量导入关系 | **需要** |
| 5 | POST | `/cypher` | Body: `{cypher, params?, limit?}` | Cypher 调试 | **需要** |
| 6 | POST | `/entities/{type}` | Body: `{name}` | 新增实体 | **需要** |
| 7 | GET | `/entities/{type}` | `page=1`, `size=20` | 分页查询实体 | — |
| 8 | GET | `/entities/{type}/{name}` | — | 实体详情 | — |
| 9 | PUT | `/entities/{type}/{name}` | Body: `{newName}` | 重命名实体 | **需要** |
| 10 | DELETE | `/entities/{type}/{name}` | — | 删除实体 | **需要** |

---

## 二十三、管理员 — 定时任务 `/api/v1/admin/tasks`

**权限**: ADMIN

| # | 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|---|
| 1 | POST | `/compute-similarity` | — | 手动触发相似度矩阵计算 |
| 2 | POST | `/sync-kg` | — | 手动触发 KG 数据同步 |
| 3 | GET | `/logs` | `limit=20` | 同步日志查询 |

---

## 二十四、Python FastAPI 服务（端口 8000）

### 推荐服务 `/api/v1/recommend`

| # | 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|---|
| 1 | POST | `/home` | Body: `{userId?, limit=10}` | 混合推荐 |
| 2 | GET | `/hot` | `days=90`, `limit=10` | 热门图书 |
| 3 | GET | `/new` | `months=6`, `limit=10` | 新书推荐 |
| 4 | GET | `/itemcf/{user_id}` | `limit=10` | ItemCF 协同过滤 |
| 5 | GET | `/kg/{user_id}` | `limit=10` | 知识图谱推荐 |

### 知识图谱 `/api/v1/kg`

| # | 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|---|
| 1 | POST | `/query` | Body: `{cypher, limit=100}` | Cypher 查询 |
| 2 | GET | `/stats` | — | 图谱统计 |
| 3 | GET | `/subgraph` | `bookId?`, `depth=2`, `limit=50` | 子图数据 |

### 离线任务 `/api/v1/tasks`

| # | 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|---|
| 1 | POST | `/compute-similarity` | — | 相似度矩阵计算 |
| 2 | POST | `/sync-kg` | — | KG 数据同步 |

---

## 二十五、统计汇总

| 分类 | 数量 |
|---|---|
| Spring Boot 接口 | **67 个** |
| FastAPI 接口 | **10 个** |
| **总计** | **77 个** |

### 按角色统计

| 角色 | 可访问管理接口 |
|---|---|
| ADMIN | 全部（77个中所有） |
| BOOK_ADMIN | 图书管理(6) + 购书链接(1) + KG管理(10) = **17** |
| OPS_ADMIN | 统计(5) + 推荐配置(2) + 用户管理(6) = **13** |
| COMMUNITY_ADMIN | 书评审核(6) = **6** |

### 二级密码验证接口（共 13 个）

删除/修改/批量操作等敏感接口需额外验证二级操作密码。
