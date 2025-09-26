import {createApp} from 'vue';
import {createPinia} from 'pinia';

import Toast from 'vue-toastification';
import 'vue-toastification/dist/index.css';
import 'vue-datepicker-next/index.css';

import App from './App.vue';
import router from './router';
import {useAuthStore} from './store/auth';
import {getBrandHint} from './utils/brandHint';

import './style.css';
import './theme.css';
import './theme/light.css';
import './theme/dark.css';
import './brand/default.css';
import './admin.css';

// Set initial theme class to avoid flashes (sync with AppHeader THEME_KEY)
try {
    const THEME_KEY = 'admin_theme_mode'; // 'auto' | 'light' | 'dark'
    const saved = localStorage.getItem(THEME_KEY);
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    const initial = (saved === 'light' || saved === 'dark') ? saved : (prefersDark ? 'dark' : 'light');
    const html = document.documentElement;
    html.classList.remove('theme-light', 'theme-dark');
    html.classList.add(initial === 'dark' ? 'theme-dark' : 'theme-light');
} catch (_) {}

// Parse brand hint from subdomain once, store locally, and set brand class early
try {
    const hint = getBrandHint(); // already lowercased
    const html = document.documentElement;
    // Clean previous brand-* classes if any
    html.classList.forEach(cls => {
        if (cls.startsWith('brand-')) html.classList.remove(cls);
    });
    if (hint) {
        localStorage.setItem('brand_hint', hint);
        html.classList.add(`brand-${hint}`);
    } else {
        localStorage.removeItem('brand_hint');
        html.classList.add('brand-default');
    }
} catch (_) {
}

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

app.use(Toast, {
    // Опции уведомлений: компактно и слева внизу
    transition: "Vue-Toastification__bounce",
    position: "top-left",
    timeout: 2500,
    hideProgressBar: true,
    closeButton: "button",
    icon: false,
    maxToasts: 5,
    newestOnTop: true
});

// Восстанавливаем сессию и возвращаем пользователя на последнюю страницу
const auth = useAuthStore();
auth.hydrateFromStorage(); // подхватить роли/профиль сразу, без запроса
let shouldRestore = true;
try {
    const skip = localStorage.getItem('skip_refresh_once') === '1';
    if (skip) {
        localStorage.removeItem('skip_refresh_once');
        shouldRestore = false; // после logout не делаем refresh на первой загрузке
    }
} catch (_) {}

if (shouldRestore) {
    try {
        await auth.restoreSession();
    } catch (_) {}
}

const lastPath = localStorage.getItem('last_path');
if (auth.isAuthenticated && lastPath && lastPath !== router.currentRoute.value.fullPath) {
    await router.replace(lastPath).catch(() => {});
}

app.mount('#app');
// Brand class is applied above during initial boot to avoid flashes