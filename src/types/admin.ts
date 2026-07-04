/**
 * 管理后台首页大盘统计数据
 */
export interface AdminStats {
  /** 平台总用户数量 */
  totalUsers: number
  /** 平台录入总图书数量 */
  totalBooks: number
  /** 用户发布总书评条数 */
  totalReviews: number
  /** 当日活跃用户数 */
  dailyActiveUsers: number
  /** 推荐模块点击率 0~1小数 */
  recommendClickRate: number
  /** 热门图书分类排行数组 */
  topCategories: string[]
}