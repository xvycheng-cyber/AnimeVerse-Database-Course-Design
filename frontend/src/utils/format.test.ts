import { describe, expect, it } from 'vitest'
import { formatCount, formatSeconds, ratingValue, statusLabel, toRecords } from './format'

describe('format utilities', () => {
  it('formats episode progress without hiding hours', () => {
    expect(formatSeconds(0)).toBe('0:00')
    expect(formatSeconds(125)).toBe('2:05')
    expect(formatSeconds(3661)).toBe('1:01:01')
  })

  it('formats large counts for compact cards', () => {
    expect(formatCount(999)).toBe('999')
    expect(formatCount(12500)).toBe('1.3万')
    expect(formatCount(120000)).toBe('12万')
  })

  it('accepts both array and paginated responses', () => {
    expect(toRecords([1, 2])).toEqual([1, 2])
    expect(toRecords({ records: [3] })).toEqual([3])
    expect(toRecords(null)).toEqual([])
  })

  it('normalizes backend rating aliases and status values', () => {
    expect(ratingValue({ rating: 9.1 })).toBe(9.1)
    expect(ratingValue({ averageRating: 8.2 })).toBe(8.2)
    expect(ratingValue({ score: 7.4 })).toBe(7.4)
    expect(statusLabel('COMPLETED')).toBe('已完结')
    expect(statusLabel('AIRING')).toBe('连载中')
  })
})
