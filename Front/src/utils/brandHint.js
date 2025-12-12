// Utility to parse brand hint from subdomain safely (client-side only)
// - Supports local dev like brand-xxx.localhost and prod like brand-xxx.tbspro.ru
// - Lowercases result
// - Returns empty string if no subdomain brand is present
// - Does not perform any network operations
export function getBrandHint(hostname = window.location.hostname) {
    try {
        // Remove port if present and lowercase
        const host = String(hostname || '').split(':')[0].toLowerCase();

        if (!host) return '';

        // Handle localhost pattern: brand-xxx.localhost or brand.localhost
        const localSuffix = '.localhost';
        if (host === 'localhost') return '';
        const idxLocal = host.indexOf(localSuffix);
        if (idxLocal > 0) {
            const sub = host.slice(0, idxLocal);
            return (sub === 'www' || sub === '') ? '' : sub;
        }

        // Prod/root domain from Vite env or fallback
        const envRoot = (import.meta?.env?.VITE_MAIN_DOMAIN || '').toString().trim().toLowerCase();
        const root = envRoot || 'tbspro.ru';

        // If host equals root (no subdomain), no hint
        if (host === root) return '';

        // If the host ends with the root domain, extract the first label
        if (host.endsWith('.' + root)) {
            const labels = host.split('.');
            // e.g., brand.tbspro.ru -> ['brand','kirillkgr','ru']
            if (labels.length >= 3) {
                const sub = labels[0];
                if (sub === 'www' || sub === '') return '';
                return sub;
            }
        }

        // Not matching expected domains
        return '';
    } catch (_) {
        return '';
    }
}

// Normalize any brand string to a label suitable for subdomain matching
// - lowercase
// - keep letters/digits from any script and hyphen (Unicode aware)
// - collapse multiple dashes
// - trim leading/trailing dashes
export function toSlug(input) {
    const base = String(input || '');
    return base
        .toLowerCase()
        .replace(/[^\p{L}\p{Nd}-]+/gu, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$|/g, '');
}
