// Shared date/time utilities
// Treat server UTC timestamps or ISO without timezone as UTC and render in user's local timezone

export function parseServerDate(val) {
    if (!val) return null;
    try {
        const s = typeof val === 'string' ? val : String(val);
        // Detect ISO like 2025-09-24T09:30:00 optionally with fractional seconds
        const looksIso = /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(s);
        const hasTZ = /[zZ]|[+-]\d{2}:\d{2}$/.test(s);
        const date = looksIso && !hasTZ ? new Date(s + 'Z') : new Date(s);
        if (Number.isNaN(date.getTime())) return null;
        return date;
    } catch {
        return null;
    }
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
