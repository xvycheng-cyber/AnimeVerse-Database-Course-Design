export function formatDate(value?: string | null): string {
  if (!value) return '-'
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatSeconds(value?: number | null): string {
  if (!value || value < 0) return '0:00'
  const total = Math.floor(value)
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  const clock = `${minutes}:${String(seconds).padStart(2, '0')}`
  return hours ? `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}` : clock
}

export function formatCount(value?: number | null): string {
  const count = Number(value || 0)
  if (count >= 10000) return `${(count / 10000).toFixed(count >= 100000 ? 0 : 1)}万`
  return String(count)
}

export function toRecords<T>(value: T[] | { records?: T[] } | null | undefined): T[] {
  if (!value) return []
  if (Array.isArray(value)) return value
  return Array.isArray(value.records) ? value.records : []
}

export function humanizeKey(key: string): string {
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (char) => char.toUpperCase())
}

export function prettyJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}

export function statusLabel(status?: string): string {
  const labels: Record<string, string> = {
    ONGOING: '连载中',
    AIRING: '连载中',
    FINISHED: '已完结',
    COMPLETED: '已完结',
    UPCOMING: '即将播出',
    PAUSED: '暂停更新'
  }
  return status ? labels[status.toUpperCase()] || status : '未知'
}

export function ratingValue(anime: {
  score?: unknown
  rating?: unknown
  averageRating?: unknown
}): number {
  const value = anime.score ?? anime.rating ?? anime.averageRating ?? 0
  const rating = Number(value)
  return Number.isFinite(rating) ? rating : 0
}
