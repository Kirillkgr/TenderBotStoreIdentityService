// Utility to parse brand hint from subdomain safely (client-side only)
// - Lowercases result
// - Returns empty string if no subdomain brand is present
// - Does not perform any network operations
export function getBrandHint(hostname = window.location.hostname) {
    try {
        // Remove port if present
        const host = hostname.split(':')[0].toLowerCase();
        // Expected root domain
        const root = 'kirillkgr.ru';

        // If host equals root (no subdomain), no hint
        if (host === root) return '';

        // If the host ends with the root domain, extract the first label
        if (host.endsWith('.' + root)) {
            const labels = host.split('.');
            // e.g., brand.kirillkgr.ru -> ['brand','kirillkgr','ru']
            if (labels.length >= 3) {
                const sub = labels[0];
                // Ignore generic subdomains if needed (e.g., 'www')
                if (sub === 'www' || sub === '') return '';
                return sub;
            }
        }

        // If not matching expected domain, return empty hint
        return '';
    } catch (_) {
        return '';
    }
}
