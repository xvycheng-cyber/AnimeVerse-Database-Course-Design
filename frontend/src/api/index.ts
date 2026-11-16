import { request } from './http'
import type {
  AdminRow,
  Anime,
  Episode,
  FavoriteItem,
  Genre,
  LoginPayload,
  LoginResult,
  OperationLog,
  PageParams,
  PageResult,
  Platform,
  RegisterPayload,
  Review,
  Role,
  Staff,
  StatisticsOverview,
  Studio,
  TrendPoint,
  TriggerResult,
  User,
  WatchHistoryItem
} from '@/types'

export const authApi = {
  login: (payload: LoginPayload) =>
    request<LoginResult>({ method: 'POST', url: '/auth/login', data: payload }),
  register: (payload: RegisterPayload) =>
    request<LoginResult>({ method: 'POST', url: '/auth/register', data: payload }),
  me: () => request<User>({ method: 'GET', url: '/auth/me' })
}

export const animeApi = {
  list: (params: PageParams) =>
    request<PageResult<Anime>>({ method: 'GET', url: '/anime', params }),
  detail: (id: string | number) =>
    request<Anime>({ method: 'GET', url: `/anime/${id}` }),
  ranking: (limit = 10) =>
    request<Anime[]>({ method: 'GET', url: '/anime/ranking', params: { limit } }),
  episodes: (id: string | number) =>
    request<Episode[]>({ method: 'GET', url: `/anime/${id}/episodes` }),
  reviews: (id: string | number, params: PageParams) =>
    request<PageResult<Review>>({
      method: 'GET',
      url: `/anime/${id}/reviews`,
      params
    }),
  createReview: (id: string | number, payload: { rating: number; content: string }) =>
    request<Review>({
      method: 'POST',
      url: `/anime/${id}/reviews`,
      data: payload
    })
}

export const catalogApi = {
  studios: (params: PageParams) =>
    request<PageResult<Studio>>({ method: 'GET', url: '/studios', params }),
  studioDetail: (id: string | number) =>
    request<Studio>({ method: 'GET', url: `/studios/${id}` }),
  staff: (params: PageParams) =>
    request<PageResult<Staff>>({ method: 'GET', url: '/staff', params }),
  genres: () => request<Genre[]>({ method: 'GET', url: '/genres' }),
  platforms: () => request<Platform[]>({ method: 'GET', url: '/platforms' })
}

export const userApi = {
  favorites: () =>
    request<FavoriteItem[] | PageResult<FavoriteItem>>({
      method: 'GET',
      url: '/user/favorites'
    }),
  addFavorite: (animeId: string | number) =>
    request<void>({ method: 'POST', url: `/user/favorites/${animeId}` }),
  removeFavorite: (animeId: string | number) =>
    request<void>({ method: 'DELETE', url: `/user/favorites/${animeId}` }),
  watchHistory: () =>
    request<WatchHistoryItem[] | PageResult<WatchHistoryItem>>({
      method: 'GET',
      url: '/user/watch-history'
    }),
  saveWatchHistory: (payload: { episodeId: string | number; progressSeconds: number }) =>
    request<void>({ method: 'POST', url: '/user/watch-history', data: payload }),
  reviews: () =>
    request<Review[] | PageResult<Review>>({ method: 'GET', url: '/user/reviews' }),
  updateReview: (id: string | number, payload: { rating: number; content: string }) =>
    request<Review>({
      method: 'PUT',
      url: `/user/reviews/${id}`,
      data: payload
    }),
  deleteReview: (id: string | number) =>
    request<void>({ method: 'DELETE', url: `/user/reviews/${id}` }),
  profile: () => request<User>({ method: 'GET', url: '/user/profile' }),
  updateProfile: (payload: { nickname: string; email: string; avatarUrl?: string }) =>
    request<User>({ method: 'PUT', url: '/user/profile', data: payload })
}

export const adminApi = {
  list: (resource: string, params: PageParams) =>
    request<PageResult<AdminRow> | AdminRow[]>({
      method: 'GET',
      url: resource,
      params
    }),
  create: (resource: string, payload: AdminRow) =>
    request<AdminRow>({ method: 'POST', url: resource, data: payload }),
  update: (resource: string, id: string | number, payload: AdminRow) =>
    request<AdminRow>({ method: 'PUT', url: `${resource}/${id}`, data: payload }),
  remove: (resource: string, id: string | number) =>
    request<void>({ method: 'DELETE', url: `${resource}/${id}` }),
  overview: () =>
    request<StatisticsOverview>({
      method: 'GET',
      url: '/admin/statistics/overview'
    }),
  trend: (days = 30) =>
    request<TrendPoint[]>({
      method: 'GET',
      url: '/admin/statistics/trend',
      params: { days }
    }),
  userStatus: (id: string | number, enabled: boolean) =>
    request<void>({
      method: 'PUT',
      url: `/admin/users/${id}/status`,
      data: { enabled }
    }),
  userRoles: (id: string | number, roleIds: Array<string | number>) =>
    request<void>({
      method: 'PUT',
      url: `/admin/users/${id}/roles`,
      data: { roleIds }
    }),
  roles: () => request<Role[]>({ method: 'GET', url: '/admin/roles' }),
  reviewVisibility: (id: string | number, hidden: boolean) =>
    request<void>({
      method: 'PUT',
      url: `/admin/reviews/${id}/visibility`,
      data: { hidden }
    }),
  logs: (params: PageParams) =>
    request<PageResult<OperationLog>>({
      method: 'GET',
      url: '/admin/logs',
      params
    })
}

export const labApi = {
  view: (viewName: string) =>
    request<AdminRow[] | PageResult<AdminRow>>({
      method: 'GET',
      url: `/lab/views/${viewName}`
    }),
  triggerFavorite: (animeId: string | number, enable: boolean) =>
    request<TriggerResult>({
      method: 'POST',
      url: '/lab/trigger/favorite',
      data: { animeId, enable }
    }),
  addAnime: (payload: AdminRow) =>
    request<AdminRow>({
      method: 'POST',
      url: '/lab/procedure/add-anime',
      data: payload
    }),
  studioStatistics: (studioId: string | number) =>
    request<AdminRow>({
      method: 'GET',
      url: `/lab/procedure/studio-statistics/${studioId}`
    }),
  animeRanking: (limit = 10) =>
    request<AdminRow[]>({
      method: 'GET',
      url: '/lab/procedure/anime-ranking',
      params: { limit }
    }),
  cursorReport: () =>
    request<AdminRow[]>({ method: 'GET', url: '/lab/cursor/studio-report' }),
  transaction: (mode: 'commit' | 'rollback' | 'exception') =>
    request<AdminRow>({
      method: 'POST',
      url: '/lab/transaction/demo',
      data: { mode },
      suppressErrorMessage: true
    })
}
