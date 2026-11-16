export type EntityId = number | string

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface PageParams {
  page?: number
  size?: number
  keyword?: string
  [key: string]: unknown
}

export interface Role {
  id: EntityId
  name?: string
  code?: string
  roleName?: string
  roleCode?: string
  description?: string
  userCount?: number
  [key: string]: unknown
}

export interface User {
  id: EntityId
  username: string
  nickname?: string
  email?: string
  avatarUrl?: string
  enabled?: boolean
  status?: string
  role?: string
  roleCode?: string
  roles?: Array<string | Role>
  createdAt?: string
  [key: string]: unknown
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  nickname: string
  email: string
}

export interface LoginResult {
  token: string
  user: User
}

export interface Genre {
  id: EntityId
  name: string
  description?: string
  animeCount?: number
  [key: string]: unknown
}

export interface Studio {
  id: EntityId
  name: string
  logoUrl?: string
  description?: string
  country?: string
  foundedYear?: number
  animeCount?: number
  [key: string]: unknown
}

export interface Staff {
  id: EntityId
  name: string
  avatarUrl?: string
  gender?: string
  birthDate?: string
  position?: string
  biography?: string
  [key: string]: unknown
}

export interface Platform {
  id: EntityId
  name: string
  logoUrl?: string
  websiteUrl?: string
  description?: string
  animeCount?: number
  [key: string]: unknown
}

export interface Anime {
  id: EntityId
  title: string
  originalTitle?: string
  coverUrl?: string
  bannerUrl?: string
  description?: string
  synopsis?: string
  releaseYear?: number
  status?: string
  averageRating?: number
  rating?: number
  score?: number
  favoriteCount?: number
  ratingCount?: number
  episodeCount?: number
  favorited?: boolean
  studioId?: EntityId
  studioName?: string
  studio?: Studio
  genres?: Genre[]
  genreNames?: string[]
  tags?: string[]
  createdAt?: string
  [key: string]: unknown
}

export interface Episode {
  id: EntityId
  animeId?: EntityId
  title: string
  episodeNumber?: number
  durationSeconds?: number
  videoUrl?: string
  description?: string
  releaseDate?: string
  [key: string]: unknown
}

export interface Review {
  id: EntityId
  animeId?: EntityId
  animeTitle?: string
  userId?: EntityId
  username?: string
  nickname?: string
  rating: number
  content: string
  hidden?: boolean
  createdAt?: string
  updatedAt?: string
  [key: string]: unknown
}

export interface FavoriteItem {
  id?: EntityId
  animeId?: EntityId
  anime?: Anime
  createdAt?: string
  [key: string]: unknown
}

export interface WatchHistoryItem {
  id?: EntityId
  animeId?: EntityId
  animeTitle?: string
  coverUrl?: string
  episodeId?: EntityId
  episodeTitle?: string
  episodeNumber?: number
  progressSeconds?: number
  durationSeconds?: number
  watchedAt?: string
  anime?: Anime
  episode?: Episode
  [key: string]: unknown
}

export interface OperationLog {
  id: EntityId
  username?: string
  action?: string
  module?: string
  method?: string
  path?: string
  ip?: string
  success?: boolean
  createdAt?: string
  [key: string]: unknown
}

export interface StatisticsOverview {
  animeCount?: number
  studioCount?: number
  userCount?: number
  reviewCount?: number
  favoriteCount?: number
  episodeCount?: number
  [key: string]: unknown
}

export interface TrendPoint {
  date?: string
  label?: string
  count?: number
  value?: number
  [key: string]: unknown
}

export type AdminRow = Record<string, unknown>

export interface AdminColumn {
  key: string
  label: string
  width?: number | string
  minWidth?: number | string
  type?: 'text' | 'image' | 'tag' | 'date' | 'score' | 'boolean' | 'link'
  optionMap?: Record<string, string>
  formatter?: (row: AdminRow) => string
}

export interface SelectOption {
  label: string
  value: string | number | boolean
}

export interface AdminFormField {
  key: string
  label: string
  type?: 'text' | 'textarea' | 'number' | 'select' | 'switch' | 'date' | 'password'
  placeholder?: string
  required?: boolean
  min?: number
  max?: number
  step?: number
  rows?: number
  options?: SelectOption[]
  defaultValue?: unknown
}

export interface CrudConfig {
  title: string
  eyebrow: string
  description: string
  resource: string
  searchPlaceholder: string
  rowKey?: string
  columns: AdminColumn[]
  fields: AdminFormField[]
  createLabel: string
  extraQuery?: Record<string, unknown>
  readonly?: boolean
}

export interface LabViewResult {
  records?: AdminRow[]
  total?: number
  [key: string]: unknown
}

export interface TriggerResult {
  beforeCount?: number
  afterCount?: number
  before?: number
  after?: number
  favoriteCountBefore?: number
  favoriteCountAfter?: number
  [key: string]: unknown
}
