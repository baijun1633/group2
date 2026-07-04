/** 用户基础资料 */
export interface UserProfileForm {
  avatar: string
  nickname: string
  bio: string
  gender: 'male' | 'female' | 'secret'
  location: string
}

/** 隐私配置开关 */
export interface PrivacyConfig {
  publicShelf: boolean
  publicReviews: boolean
  searchable: boolean
}

/** 消息通知配置开关 */
export interface NotifyConfig {
  reply: boolean
  like: boolean
  system: boolean
}

/** 用户完整资料返回 */
export interface UserFullProfile {
  profile: UserProfileForm
  privacy: PrivacyConfig
  notify: NotifyConfig
  phone: string
  email: string
}

/** 修改密码表单 */
export interface PasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
/** 用户主页阅读统计 */
export interface UserReadStats {
  readBooks: number
  readingHours: number
  collections: number
  reviews: number
}

/** 用户阅读偏好标签（分类+作者） */
export interface UserPrefTags {
  categories: string[]
  authors: string[]
}

/** 个人主页完整返回数据 */
export interface UserHomeRes {
  profile: UserProfileForm
  stats: UserReadStats
  preferences: UserPrefTags
}

/** 编辑资料弹窗表单 */
export type ProfileEditForm = Pick<UserProfileForm, 'nickname' | 'bio' | 'location'>