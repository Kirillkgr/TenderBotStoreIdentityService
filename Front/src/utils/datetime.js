// Shared date/time utilities
// Server returns UTC timestamps (e.g., "2025-10-09T10:00:00Z" or ISO without TZ)
// We convert them to browser-local time for display.

export function parseServerDate(val) {
    if (!val) return null;
    try {
        let s = typeof val === 'string' ? val : String(val);
        // Normalize fractional seconds to max 3 digits (milliseconds) if present
        // e.g. 2025-10-09T12:58:52.986029783 -> 2025-10-09T12:58:52.986
        if (/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+/.test(s)) {
            s = s.replace(/(\.\d{3})\d+/, '$1');
        }
        // Detect ISO like 2025-09-24T09:30:00 optionally with fractional seconds
        const looksIso = /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(s);
        const hasTZ = /[zZ]|[+-]\d{2}:\d{2}$/.test(s);
        const iso = looksIso && !hasTZ ? s + 'Z' : s;
        const date = new Date(iso);
        if (Number.isNaN(date.getTime())) return null;
        return date;
    } catch {
        return null;
    }
}

// Returns IANA time zone of browser (e.g., "Europe/Kaliningrad")
export function getLocalTimeZone() {
    try {
        return Intl.DateTimeFormat().resolvedOptions().timeZone;
    } catch {
        return undefined; // fallback to default locale handling
    }
}

// Convert server UTC timestamp (string) to a Date object in local time context
// Usage: const localDate = toLocalDate('2025-10-09T10:00:00Z');
export function toLocalDate(val) {
    return parseServerDate(val);
}

export function formatLocalDateTime(val, opts) {
    const d = parseServerDate(val);
    if (!d) return '';
    return new Intl.DateTimeFormat(undefined, {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit',
        ...(opts || {})
    }).format(d);
}

// Explicit timeZone formatting example:
// formatUtcToLocal('2025-10-09T10:00:00Z', 'ru-RU', { timeZone: 'Europe/Kaliningrad' })
export function formatUtcToLocal(val, locale = undefined, options = {}) {
    const d = parseServerDate(val);
    if (!d) return '';
    const tz = options.timeZone || getLocalTimeZone();
    return new Intl.DateTimeFormat(locale, {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        timeZone: tz, ...options
    }).format(d);
}

export function timeAgo(val, now = new Date()) {
    const d = parseServerDate(val);
    if (!d) return '';
    const diffMs = now - d;
    const diffSec = Math.floor(diffMs / 1000);
    if (diffSec < 0) return 'только что';
    if (diffSec < 60) return 'только что';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin} мин назад`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `${diffH} ч назад`;
    const diffD = Math.floor(diffH / 24);
    if (diffD === 1) return 'вчера';
    if (diffD < 7) return `${diffD} дн. назад`;
    // older — show localized date
    return formatLocalDateTime(d);
}

// Short RU date like DD.MM.YYYY
export function formatDateShortRU(val) {
    const d = parseServerDate(val);
    if (!d) return '';
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    return `${dd}.${mm}.${yyyy}`;
}

// Short time-ago label: "1 ч" / "15 мин" / "2 дн" / "только что"
export function timeAgoShort(val, now = new Date()) {
    const d = parseServerDate(val);
    if (!d) return '';
    const diffMs = now - d;
    const sec = Math.floor(diffMs / 1000);
    const min = Math.floor(sec / 60);
    const hr = Math.floor(min / 60);
    const day = Math.floor(hr / 24);
    if (day >= 365) return `${Math.floor(day / 365)} г`;
    if (day > 0) return `${day} дн`;
    if (hr > 0) return `${hr} ч`;
    if (min > 0) return `${min} мин`;
    return 'только что';
}
