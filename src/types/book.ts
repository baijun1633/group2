export interface BookItem {
  id: number
  bookId: number
  title: string
  author: string
  cover: string
  coverUrl: string
  coverImage: string
  category: string
  summary: string
  avgRating: number
  ratingCount: number
  status: string
  words: number
  publishDate: string
  tags: string[]
}

export interface ShelfItem {
  id: string
  name: string
  count: number
}

export interface CollectItem {
  id: number
  title: string
  author: string
  cover: string
  category: string
  favTime: string
}

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatar: string
  bio: string
  joinDate: string
  location: string
}

export interface SearchBookItem {
  bookId: number
  title: string
  author: string
  publisher: string
  isbn: string
  coverUrl: string
  coverImage: string
  avgRating: number
  ratingCount: number
  viewCount: number
  collectCount: number
  tags: string[]
  category?: string
  summary?: string
}

export interface BookSearchData {
  records: SearchBookItem[]
  total: number
  page: number
  size: number
}

export interface BookSearchRes {
  code: number
  message: string
  data: BookSearchData
  answer?: string
  books?: SearchBookItem[]
}

export interface HotSearchWord {
  word: string
  count: number
}

export interface BookChapter {
  id: number
  title: string
  updateTime: string
  wordCount: number
}

export interface BookDetail extends BookItem {
  chapters: BookChapter[]
}

export interface BookListRes {
  list: BookItem[]
  total: number
  page: number
  pageSize: number
}

export interface RecommendBook {
  bookId: number
  title: string
  author: string
  coverUrl: string
  coverImage: string
}