import {useAuthStore} from '@/store/auth';

function applyVisibility(el, allowed, options) {
    const mode = options?.mode || 'hide'; // 'hide' | 'disable'
    const tooltip = options?.tooltip || 'Недостаточно прав';

    if (allowed) {
        // restore
        el.style.display = el.__vcan_orig_display ?? '';
        el.style.pointerEvents = el.__vcan_orig_pointer ?? '';
        el.style.opacity = el.__vcan_orig_opacity ?? '';
        el.removeAttribute('aria-disabled');
        if (el.__vcan_title_saved) {
            el.setAttribute('title', el.__vcan_title_saved);
        }
        return;
    }

    if (mode === 'disable') {
        if (el.__vcan_orig_pointer == null) el.__vcan_orig_pointer = el.style.pointerEvents;
        if (el.__vcan_orig_opacity == null) el.__vcan_orig_opacity = el.style.opacity;
        el.style.pointerEvents = 'none';
        el.style.opacity = '0.5';
        el.setAttribute('aria-disabled', 'true');
        if (!el.__vcan_title_saved) el.__vcan_title_saved = el.getAttribute('title') || '';
        el.setAttribute('title', tooltip);
    } else {
        // hide
        if (el.__vcan_orig_display == null) el.__vcan_orig_display = el.style.display;
        el.style.display = 'none';
    }
}

function evaluate(bindingValue) {
    // bindingValue can be Array<string> or { any?: string[], all?: string[], mode?, tooltip? }
    let any = [];
    let all = [];
    let mode;
    let tooltip;
    if (Array.isArray(bindingValue)) {
        any = bindingValue;
    } else if (bindingValue && typeof bindingValue === 'object') {
        any = Array.isArray(bindingValue.any) ? bindingValue.any : [];
        all = Array.isArray(bindingValue.all) ? bindingValue.all : [];
        mode = bindingValue.mode;
        tooltip = bindingValue.tooltip;
    }
    return {any, all, mode, tooltip};
}

function isAllowed(roles, any, all) {
    const r = Array.isArray(roles) ? roles : [];
    if ((all && all.length) && !all.every(x => r.includes(x))) return false;
    if ((any && any.length) && !any.some(x => r.includes(x))) return false;
    // if neither any nor all provided, allow
    return (any?.length || all?.length) ? true : true;
}

export default {
    mounted(el, binding) {
        const auth = useAuthStore();
        const {any, all, mode, tooltip} = evaluate(binding.value);
        applyVisibility(el, isAllowed(auth.roles, any, all), {mode, tooltip});
        // Watch roles changes (Pinia store is reactive; minimal polling via subscription)
        el.__vcan_unsub = auth.$subscribe(() => {
            applyVisibility(el, isAllowed(auth.roles, any, all), {mode, tooltip});
        });
    },
    updated(el, binding) {
        const auth = useAuthStore();
        const {any, all, mode, tooltip} = evaluate(binding.value);
        applyVisibility(el, isAllowed(auth.roles, any, all), {mode, tooltip});
    },
    unmounted(el) {
        if (el.__vcan_unsub) {
            try {
                el.__vcan_unsub();
            } catch (_) {
            }
            el.__vcan_unsub = null;
        }
    }
};
