import {describe, expect, it} from 'vitest'
import {formatLocalDateTime, formatUtcToLocal, parseServerDate} from '@/utils/datetime.js'

/**
 * These tests verify that server UTC timestamps are rendered in the client
 * timezone correctly. We explicitly pass timeZone to make tests deterministic
 * regardless of runner machine settings.
 */

describe('datetime utils: UTC -> local formatting', () => {
    const ISO_Z = '2025-10-09T10:00:00Z' // explicit UTC
    const ISO_NOZ = '2025-10-09T10:00:00' // implicit UTC in our client parsing

    it('parseServerDate handles ISO with and without Z as UTC', () => {
        const dZ = parseServerDate(ISO_Z)
        const dNoZ = parseServerDate(ISO_NOZ)
        expect(dZ).toBeInstanceOf(Date)
        expect(dNoZ).toBeInstanceOf(Date)
        // both represent the same instant in time
        expect(dZ.getTime()).toBe(dNoZ.getTime())
    })

    it('formats to UTC (no shift) when timeZone is UTC', () => {
        const s = formatUtcToLocal(ISO_Z, 'en-GB', {timeZone: 'UTC', hour12: false})
        // Expect 10:00 in any date prefix
        expect(s).toMatch(/10:00$/)
    })

    it('applies +02:00 shift (Europe/Kaliningrad sample)', () => {
        const s = formatUtcToLocal(ISO_Z, 'en-GB', {timeZone: 'Europe/Kaliningrad', hour12: false})
        // 10:00Z -> 12:00 local
        expect(s).toMatch(/12:00$/)
    })

    it('formatLocalDateTime defaults to environment TZ but still returns a string', () => {
        const s = formatLocalDateTime(ISO_Z)
        expect(typeof s).toBe('string')
        expect(s.length).toBeGreaterThan(0)
    })
})
