import fallbackCover from '@/assets/cover-fallback.svg'

export const animeCoverPool = [
  // Verified fallback artwork; backend-provided cover URLs remain the primary source.
  'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx16498-C6FPmWm59CyP.jpg'
]

export function animeCover(
  url?: string,
  seed: string | number = 0
): string {
  if (url) return url
  const index = Math.abs(String(seed).split('').reduce((sum, char) => sum + char.charCodeAt(0), 0))
  return animeCoverPool[index % animeCoverPool.length] || fallbackCover
}

export { fallbackCover }
