# Checklist

## 第一期验收标准

- [ ] 项目启动无编译错误和依赖冲突
- [ ] `POST /api/v1/auth/register` 注册成功返回用户信息和Token
- [ ] `POST /api/v1/auth/register` 用户名重复返回 code=1003
- [ ] `POST /api/v1/auth/login` 登录成功返回 accessToken 和 refreshToken
- [ ] `POST /api/v1/auth/login` 密码错误返回 code=1004
- [ ] `POST /api/v1/auth/refresh` 使用 refreshToken 获取新Token对
- [ ] `GET /api/v1/users/me` 携带有效Token返回当前用户信息
- [ ] `GET /api/v1/users/me` 无Token或无效Token返回 code=1001
- [ ] `PUT /api/v1/users/me/preferences` 更新偏好标签成功
- [ ] 所有响应格式统一为 `{"code": 0, "message": "...", "data": {...}}`
- [ ] 全局异常处理器捕获业务异常和未知异常
- [ ] JWT accessToken 有效期2小时，refreshToken 有效期7天

## 第二期验收标准

- [ ] `GET /api/v1/books/search?keyword=三体` 返回匹配图书列表（含matchType字段）
- [ ] `GET /api/v1/books?category=科幻&tag=硬科幻&sortBy=hot` 返回筛选排序结果
- [ ] `GET /api/v1/books/{bookId}` 返回图书详情（含avgRating、ratingCount）
- [ ] `POST /api/v1/books/{bookId}/rating` 提交评分成功，avg_rating 更新正确
- [ ] `GET /api/v1/books/{bookId}/rating` 返回平均分和评分分布
- [ ] `GET /api/v1/books/{bookId}/purchase-links` 返回购书链接列表
- [ ] `POST /api/v1/admin/books` 管理员可新增图书
- [ ] `PUT /api/v1/admin/books/{bookId}` 管理员可编辑图书
- [ ] `DELETE /api/v1/admin/books/{bookId}` 管理员可删除图书
- [ ] 搜索接口支持分页（page/size参数生效）

## 第三期验收标准

- [ ] `POST /api/v1/shelves` 创建书架成功
- [ ] `GET /api/v1/shelves` 返回用户书架列表（含bookCount）
- [ ] `POST /api/v1/shelves/{shelfId}/books` 添加图书到书架成功
- [ ] `PUT /api/v1/shelves/{shelfId}/books/{bookId}` 移动图书到其他书架成功
- [ ] `DELETE /api/v1/shelves/{shelfId}` 删除书架成功（图书变为未分类）
- [ ] `PUT /api/v1/reading/progress` 同步阅读进度成功
- [ ] `GET /api/v1/reading/progress/{bookId}` 返回最新进度
- [ ] `GET /api/v1/reading/history` 返回分页阅读历史
- [ ] `GET /api/v1/users/me/profile` 返回用户画像（tagVector、preferredAuthors、preferredCategories）
- [ ] `PUT /api/v1/users/me/preferences` 更新偏好标签后画像同步更新

## 第四期验收标准

- [ ] `POST /api/v1/books/{bookId}/reviews` 发布书评成功，状态为 pending_audit
- [ ] `GET /api/v1/books/{bookId}/reviews` 返回分页书评列表（支持sortBy排序）
- [ ] `DELETE /api/v1/reviews/{reviewId}` 用户可删除自己未审核书评
- [ ] `POST /api/v1/reviews/{reviewId}/like` 点赞成功，再次调用取消点赞
- [ ] `POST /api/v1/reviews/{reviewId}/reply` 回复书评成功
- [ ] `GET /api/v1/admin/reviews/pending` 管理员获取待审核书评
- [ ] `PUT /api/v1/admin/reviews/{reviewId}/audit` 管理员审核（通过/驳回/删除）
- [ ] 书评列表返回 likesCount 和回复数

## 第五期验收标准

- [ ] Neo4j 连接配置正确，项目启动时可连接
- [ ] `POST /api/v1/admin/kg/build` 触发图谱构建，返回 taskId
- [ ] 图谱构建完成后 Neo4j 中存在正确的节点和关系
- [ ] `POST /api/v1/kg/query` Cypher查询返回正确结果
- [ ] `GET /api/v1/kg/graph/{entityId}?depth=2` 返回 nodes 和 edges 数据
- [ ] `POST /api/v1/admin/kg/relations` 管理员可添加/删除/修改图谱关系
- [ ] 可视化数据格式正确，前端可直接渲染（D3.js/vis.js兼容）

## 第六期验收标准

- [ ] `GET /api/v1/recommend/home` 登录用户返回个性化推荐（含matchScore和reason）
- [ ] `GET /api/v1/recommend/home` 未登录用户返回热门/新书兜底列表
- [ ] 推荐结果包含 reason（推荐理由文本）和 reasonPath（推理路径节点列表）
- [ ] `GET /api/v1/books/{bookId}/similar` 返回相似读物
- [ ] `GET /api/v1/books/{bookId}/extended` 返回延伸阅读（基于多跳推理）
- [ ] `GET /api/v1/recommend/hot?days=30` 返回热门推荐
- [ ] `GET /api/v1/recommend/new?months=3` 返回新书推荐
- [ ] `GET /api/v1/recommend/{recId}/explain` 返回推荐解释详情（含pathNodes）
- [ ] `PUT /api/v1/admin/recommend/config` 配置权重，权重之和必须为1
- [ ] ItemCF 算法可基于用户评分数据计算图书相似度
- [ ] 图谱推理推荐可生成至少3跳的推理路径

## 第七期验收标准

- [ ] `GET /api/v1/admin/users` 管理员分页查询用户列表
- [ ] `POST /api/v1/admin/users` 管理员新增用户（可设置角色）
- [ ] `PUT /api/v1/admin/users/{userId}` 管理员修改用户信息
- [ ] `DELETE /api/v1/admin/users/{userId}` 管理员删除用户
- [ ] `POST /api/v1/admin/books/batch` CSV批量导入返回成功/失败统计
- [ ] `GET /api/v1/admin/stats` 返回总用户、总图书、总书评、日活、推荐点击率等
- [ ] `GET /api/v1/admin/stats` 返回热门分类排行
- [ ] `PUT /api/v1/admin/recommend/config` 推荐权重配置持久化生效
- [ ] `GET /api/v1/books/{bookId}/preview` 返回试读内容
- [ ] 未登录用户试读限前3页，登录用户限前10页或第一章
- [ ] 管理员接口非管理员用户返回403
