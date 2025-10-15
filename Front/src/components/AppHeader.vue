<template>
    <nav class="main-nav" :class="{ 'main-nav--hidden': !isHeaderVisible }">
      <div class="logo-wrap">
        <!-- Burger moved to the left side -->
        <div :class="{ 'is-active': false }" class="burger" title="Меню" @click="ui.toggleSidebar">
          <span></span>
          <span></span>
          <span></span>
        </div>

        <!-- Single theme toggle button: Auto → Dark → Light → Auto -->
        <div class="theme-toggle-vert" @click.stop>
          <button :title="`Тема: ${themeMode}`" class="ttv-btn" @click="cycleTheme">{{ themeIcon }}</button>
        </div>

        <!-- QR quick button -->
        <button class="qr-btn" @click.stop="openQr" aria-label="Показать QR код" type="button">
          <img class="qr-img" :src="qrDataUrl" alt="QR code" width="28" height="28" />
        </button>

        <router-link to="/" class="logo" @click.stop> TenderBotStore</router-link>
        <span v-if="brandChip" :title="brandChipTitle" class="brand-chip">{{ brandChip }}</span>
      </div>

      <div class="nav-links">
        <button v-if="authStore.isAuthenticated" class="nav-link" type="button" @click="openContextModal">Контексты
        </button>
        <button :aria-label="`Корзина, товаров: ${cartCountDisplay}, сумма: ${cartTotalDisplay}`" class="cart-btn" type="button"
                @click="openMiniCart">
          <span class="cart-ico" v-html="cartSvg"></span>
          <span v-if="cartCount > 0" :class="{ pulse: badgePulse }" :title="`В корзине: ${cartCountDisplay}`"
                class="cart-badge">{{ cartCountDisplay }}</span>
        </button>
      </div>

      <!-- User avatar/menu (visible for guests as well) -->
      <div class="user-chip-wrap" @mouseenter="chipHover = true" @mouseleave="chipHover = false">
        <button
            :title="authStore.isAuthenticated ? (authStore.user?.username || 'Профиль') : 'Войти или зарегистрироваться'"
            class="user-chip" type="button"
            @click.stop="authStore.isAuthenticated ? goProfile() : (chipHover = !chipHover)">
          <img v-if="authStore.user?.avatarUrl" :src="authStore.user.avatarUrl" alt="avatar" class="user-chip__img"
               height="28" width="28"/>
          <img v-else :src="userIcon" alt="user" class="user-chip__img user-chip__img--placeholder" height="28"
               width="28"/>
          <span v-if="authStore.isAuthenticated && (nStore.hasAnyUnread || nStore.hasClientNavDot)"
                :title="`Есть новые события`" class="unread-dot"></span>
        </button>
        <transition name="fade-scale">
          <div v-if="chipHover" class="user-menu" @mouseenter="chipHover = true" @mouseleave="chipHover = false">
            <template v-if="authStore.isAuthenticated">
              <button class="user-menu__item" type="button" @click="goProfile">Профиль</button>
              <button class="user-menu__item user-menu__item--danger" type="button" @click="handleLogout">Выйти</button>
            </template>
            <template v-else>
              <button class="user-menu__item" type="button" @click="openLogin">Войти</button>
              <button class="user-menu__item" type="button" @click="openRegister">Зарегистрироваться</button>
            </template>
          </div>
        </transition>
      </div>
    </nav>
    <!-- Modal with enlarged QR for easy scanning (teleported to body) -->
    <teleport to="body">
      <transition name="fade-scale">
        <div v-if="showQr" class="qr-overlay" @click="showQr = false" aria-modal="true" role="dialog">
          <div class="qr-card" @click.stop>
            <div class="qr-full qr-inline" v-html="qrInlineRef"></div>
            <button class="qr-close" @click="showQr = false" aria-label="Закрыть" type="button">×</button>
            <p class="qr-hint">Наведите камеру, чтобы открыть сайт</p>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- Context select modal -->
    <ContextSelectModal :visible="showContext" @close="showContext=false"/>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useAuthStore} from '../store/auth';
import {useNotificationsStore} from '../store/notifications';
import {useCartStore} from '../store/cart';
import {getBrandHint} from '../utils/brandHint';
import ContextSelectModal from './modals/ContextSelectModal.vue';
import {useUiStore} from '../store/ui';

import qrInline from '../assets/qr-code.svg?raw';
import userIcon from '../assets/user.svg';
import cartSvg from '../assets/cart.svg?raw';

const props = defineProps({
  isModalVisible: {
    type: Boolean,
    required: true
  }
});

const emit = defineEmits(['open-login-modal', 'open-register-modal', 'open-mini-cart']);
const route = useRoute();
const authStore = useAuthStore();
const nStore = useNotificationsStore();
const cartStore = useCartStore();
const router = useRouter();
const qrInlineRef = ref(qrInline);
const ui = useUiStore();
const qrDataUrl = computed(() =>
  'data:image/svg+xml;utf8,' + encodeURIComponent(qrInlineRef.value || '')
);
const cartSvgRef = ref(cartSvg);
const badgePulse = ref(false);

function openContextModal() {
  showContext.value = true;
}

async function onSelectMembership(e) {
  const val = e?.target?.value;
  if (!val) return;
  const m = (authStore.memberships || []).find(x => String(x.membershipId || x.id) === String(val));
  if (!m) return;
  await authStore.selectMembership(m);
}

// Бренд-класс на <html>: brand--{brandId|brandName}
const prevBrandClass = ref('');

function toBrandClass() {
  const mId = authStore.membershipId;
  const m = (authStore.memberships || []).find(x => x.membershipId === mId);
  const key = m?.brandId || m?.brandName;
  if (!key) return '';
  const norm = String(key).toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9_-]/g, '');
  return norm ? `brand--${norm}` : '';
}

function applyBrandClass() {
  try {
    const html = document.documentElement;
    if (prevBrandClass.value) html.classList.remove(prevBrandClass.value);
    const cls = toBrandClass();
    if (cls) {
      html.classList.add(cls);
      prevBrandClass.value = cls;
      try {
        localStorage.setItem('current_brand_class', cls);
      } catch (_) {
      }
    } else {
      prevBrandClass.value = '';
      try {
        localStorage.removeItem('current_brand_class');
      } catch (_) {
      }
    }
  } catch (_) {
  }
}

// Чип контекста: бренд из выбранного membership или подсказка из субдомена
const brandChip = computed(() => {
  try {
    const selectedBrand = authStore.brandId ? (authStore.memberships || []).find(m => m.brandId === authStore.brandId)?.brandName : null;
    if (selectedBrand) return selectedBrand;
    const hint = getBrandHint();
    return hint || '';
  } catch (_) {
    return '';
  }
});
const brandChipTitle = computed(() => {
  const mId = authStore.membershipId;
  const m = (authStore.memberships || []).find(x => x.membershipId === mId);
  const master = m?.masterName || m?.masterId || '';
  const brand = m?.brandName || m?.brandId || '';
  const loc = m?.locationName || m?.locationId || '';
  const parts = [master, brand, loc].filter(Boolean);
  return parts.length ? `Контекст: ${parts.join(' / ')}` : 'Контекст не выбран';
});

// Селектор контекста: список опций и выбранный id
const membershipOptions = computed(() => {
  const list = authStore.memberships || [];
  return list.map(m => ({
    value: String(m.membershipId ?? m.id),
    label: [m.masterName || m.masterId, m.brandName || m.brandId, m.locationName || m.locationId]
        .filter(Boolean)
        .join(' / ')
  }));
});
const selectedMembershipId = computed(() => authStore.membershipId ? String(authStore.membershipId) : '');

// Тема: общий ключ с админкой
const THEME_KEY = 'admin_theme_mode'; // 'auto' | 'light' | 'dark'
const themeMode = ref('auto'); // авто до первого вмешательства пользователя
const media = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

const computedTheme = computed(() => {
  if (themeMode.value === 'light') return 'light';
  if (themeMode.value === 'dark') return 'dark';
  const systemDark = media ? media.matches : false;
  return systemDark ? 'dark' : 'light';
});

function applyTheme() {
  const html = document.documentElement;
  html.classList.remove('theme-light', 'theme-dark');
  html.classList.add(computedTheme.value === 'dark' ? 'theme-dark' : 'theme-light');
}

function setTheme(mode) {
  // Пользователь явно выбирает режим: сохраняем
  themeMode.value = mode; // 'light' | 'dark'
}

// Cycle order: auto -> dark -> light -> auto
function cycleTheme() {
  const next = themeMode.value === 'auto' ? 'dark' : themeMode.value === 'dark' ? 'light' : 'auto';
  setTheme(next);
}

const themeIcon = computed(() => themeMode.value === 'auto' ? 'A' : (computedTheme.value === 'dark' ? '🌙' : '☀️'));
const isMenuOpen = ref(false);
const showQr = ref(false);
const showContext = ref(false);
const chipHover = ref(false);
const isProfilePage = computed(() => route.name === 'Profile');
const isHeaderVisible = ref(true);
let lastScrollPosition = 0;

const isAdminOrOwner = computed(() => {
  const roles = Array.isArray(authStore.roles) ? authStore.roles : [];
  return roles.includes('ADMIN') || roles.includes('OWNER');
});

function hasRoleInMemberships(role) {
  try {
    const list = Array.isArray(authStore.memberships) ? authStore.memberships : [];
    const norm = (r) => String(r || '').toUpperCase().replace(/^ROLE_/, '');
    return list.some(m => {
      const r = norm(m.role || m.membershipRole || m.brandRole || m.locationRole);
      return r === norm(role);
    });
  } catch {
    return false;
  }
}

const isCook = computed(() => {
  const roles = Array.isArray(authStore.roles) ? authStore.roles : [];
  return roles.includes('COOK') || hasRoleInMemberships('COOK');
});

const isCashier = computed(() => {
  const roles = Array.isArray(authStore.roles) ? authStore.roles : [];
  return roles.includes('CASHIER') || hasRoleInMemberships('CASHIER');
});

const canSeeKitchen = computed(() => isCook.value || isAdminOrOwner.value);
const canSeeCashier = computed(() => isCashier.value || isAdminOrOwner.value);
const canSeeOrders = computed(() => isCashier.value || isAdminOrOwner.value);

// На будущее: инициалы, если захотим показать поверх иконки
const userInitials = computed(() => {
  const name = authStore.user?.username || authStore.user?.firstName || '';
  if (!name) return '';
  const parts = String(name).trim().split(/\s+/);
  const first = parts[0]?.[0] || '';
  const last = parts.length > 1 ? parts[parts.length - 1]?.[0] || '' : '';
  return (first + last).toUpperCase();
});

function handleLogout() {
  authStore.logout();
  isMenuOpen.value = false;
  router.push('/login');
}

function goProfile() {
  router.push('/profile');
}

function toggleMenu() {
  isMenuOpen.value = !isMenuOpen.value;
}

function closeMenu() {
  isMenuOpen.value = false;
}

function openLogin() {
  console.log('AppHeader: emit open-login-modal');
  emit('open-login-modal');
  isMenuOpen.value = false; // Close menu on mobile after clicking
}

function openRegister() {
  emit('open-register-modal');
  isMenuOpen.value = false; // Close menu on mobile after clicking
}

function openMiniCart() {
  emit('open-mini-cart');
  isMenuOpen.value = false;
}

function openQr() {
  // Небольшая защита от случайного двойного клика/пропагации
  console.log('openQr click');
  // Закрываем мобильное меню, если открыто (иначе overlay может перекрывать клики)
  if (isMenuOpen.value) isMenuOpen.value = false;
  showQr.value = true;
}

// Переключает membership под нужную роль (если требуется) и выполняет переход
async function ensureRoleAndGo(requiredRole, path, anyOf = null) {
  const target = Array.isArray(anyOf) && anyOf.length ? anyOf : [requiredRole];
  const hasAny = (rs) => {
    const roles = Array.isArray(authStore.roles) ? authStore.roles : [];
    return rs.some(r => roles.includes(r));
  };
  try {
    if (!hasAny(target)) {
      const list = Array.isArray(authStore.memberships) ? authStore.memberships : [];
      const norm = (r) => String(r || '').toUpperCase().replace(/^ROLE_/, '');
      const m = list.find(x => {
        const r = norm(x.role || x.membershipRole || x.brandRole || x.locationRole);
        return target.some(tr => r === norm(tr));
      });
      if (m) {
        await authStore.selectMembership(m);
        // дождёмся обновления ролей (до 1.5с)
        const ok = await new Promise((resolve) => {
          if (hasAny(target)) return resolve(true);
          const start = Date.now();
          const unsub = authStore.$subscribe(() => {
            if (hasAny(target)) {
              try {
                unsub();
              } catch (_) {
              }
              resolve(true);
            } else if (Date.now() - start > 1500) {
              try {
                unsub();
              } catch (_) {
              }
              resolve(false);
            }
          });
          // подстраховка на случай, если $subscribe не сработает
          setTimeout(() => {
            try {
              unsub();
            } catch (_) {
            }
            resolve(hasAny(target));
          }, 1600);
        });
        // даже если не дождались — пробуем навигацию, гвард может пропустить при обновлённых ролях
      }
    }
    await router.push(path);
  } catch (_) {
    await router.push(path);
  } finally {
    isMenuOpen.value = false;
  }
}

function handleScroll() {
  const currentScrollPosition = window.pageYOffset || document.documentElement.scrollTop;
  if (currentScrollPosition < 0) {
    return;
  }
  // Показываем шапку, если скроллим вверх или находимся в самом верху
  isHeaderVisible.value = currentScrollPosition < lastScrollPosition || currentScrollPosition < 10;
  lastScrollPosition = currentScrollPosition;
}

function onKeydown(e) {
  if (e.key === 'Escape') {
    showQr.value = false;
  }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll);
  // Автопоказ модалки выбора контекста после логина (инициирует store/auth)
  window.addEventListener('open-context-modal', openContextModal);
  // init theme
  const saved = localStorage.getItem(THEME_KEY);
  if (saved === 'light' || saved === 'dark') themeMode.value = saved; // иначе остаёмся в auto
  if (media && media.addEventListener) {
    media.addEventListener('change', () => {
      if (themeMode.value === 'auto') applyTheme();
    });
  }
  applyTheme();
  window.addEventListener('keydown', onKeydown);

  // Применим бренд-класс с учётом сохранённого значения, затем актуализируем по store
  try {
    const savedBrandCls = localStorage.getItem('current_brand_class');
    if (savedBrandCls) {
      document.documentElement.classList.add(savedBrandCls);
      prevBrandClass.value = savedBrandCls;
    }
  } catch (_) {
  }
  applyBrandClass();

  // Инициализируем корзину при загрузке
  try {
    cartStore.fetchCart();
  } catch (_) {
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll);
  if (media && media.removeEventListener) {
    media.removeEventListener('change', () => {
    });
  }
  window.removeEventListener('keydown', onKeydown);
  window.removeEventListener('open-context-modal', openContextModal);
});

watch(themeMode, (v) => {
  localStorage.setItem(THEME_KEY, v);
  applyTheme();
});

// Следим за сменой контекста и применяем бренд-класс
watch(() => [authStore.membershipId, authStore.brandId], () => {
  applyBrandClass();
});

// Следим за авторизацией и обновляем корзину
watch(() => authStore.isAuthenticated, (v) => {
  if (v) {
    try {
      cartStore.fetchCart();
    } catch (_) {
    }
  }
});

const cartCount = computed(() => {
  const items = Array.isArray(cartStore.items) ? cartStore.items : [];
  try {
    return items.reduce((acc, it) => acc + (Number(it.quantity ?? it.qty ?? 1) || 0), 0) || 0;
  } catch {
    return items.length;
  }
});

const cartCountDisplay = computed(() => (cartCount.value > 99 ? '99+' : String(cartCount.value)));

watch(cartCount, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    badgePulse.value = true;
    setTimeout(() => {
      badgePulse.value = false;
    }, 160);
  }
});

function formatMoney(n) {
  try {
    return new Intl.NumberFormat('ru-RU').format(Math.max(0, Number(n) || 0));
  } catch {
    return String(n || 0);
  }
}

const cartTotalDisplay = computed(() => `${formatMoney(cartStore.total)} ₽`);
const cartTotalFull = computed(() => `${formatMoney(cartStore.total)} рублей`);
const cartHasTotal = computed(() => Number(cartStore.total || 0) > 0);

// Lock body scroll when modal is open
watch(showQr, (open) => {
  if (open) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
});
</script>

<style scoped>
.btn-primary {
  background-color: #3498db;
  border-color: #3498db;
  color: #fff !important;
}

.btn-primary:hover {
  background-color: #2980b9;
}

/* Focus styling for all nav links and buttons */
.nav-links a:focus-visible,
.nav-links button:focus-visible {
  background-color: #3498db;
  outline: none;
  border-radius: 4px;
  color: #fff !important;
}

/* Avatar chip and tiny unread dot */
.user-chip-wrap {
  position: relative;
}

.user-chip {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.06);
  padding: 0;
  cursor: pointer;
}

.user-chip__img {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.user-chip__img--placeholder {
  background: #2f3640;
}

.unread-dot {
  position: absolute;
  bottom: 2px;
  left: 2px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #e53935;
  box-shadow: 0 0 0 2px rgba(36, 36, 36, 0.9);
}

.nav-link-wrap {
  position: relative;
  display: inline-block;
  margin-left: 8px;
}

.nav-link-wrap > a {
  padding-right: 14px;
}

.nav-dot {
  position: absolute;
  top: -4px;
  right: -8px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #e53935;
  box-shadow: 0 0 0 2px rgba(36, 36, 36, 0.9);
}

</style>

<!-- Global styles for teleported QR modal (scoped styles don't apply to teleport) -->
<style>
.qr-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  cursor: zoom-out;
}

.qr-card {
  position: relative;
  background: #1f1f1f;
  border-radius: 12px;
  padding: 0.5rem 0.5rem -0.75rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.06);
  max-width: 76vw;
  max-height: 83vh;
  overflow: auto;
}

.qr-full {
  display: block;
  width: min(80vw, 25rem);
  height: auto;
  margin: 0 auto;
}

.qr-close {
  position: absolute;
  top: 6px;
  right: 8px;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 1.25rem;
  cursor: pointer;
}

.qr-hint {
  color: #bdbdbd;
  font-size: 0.85rem;
  text-align: center;
  margin: 0.5rem 0 0.25rem;
}

</style>

<style scoped>
.header-container {
  position: relative;
}

.nav-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999; /* Должен быть ниже чем меню, но выше остального контента */
}

.main-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  --header-height: 60px;
  background: #2c2c2c;
  padding: 0.75rem 1.5rem;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: transform 0.3s ease-in-out;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  min-height: var(--header-height); /* стабильная высота шапки */
}

/* Header-specific tweaks for inline QR icon (small size like before) */
.qr-btn {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
  overflow: hidden; /* не позволяем SVG вылезать за пределы */
}
.qr-small svg { width: 100% !important; height: 100% !important; display: block; }
.qr-img { width: 28px; height: 28px; display: block; }
.qr-img { object-fit: contain; }

.brand-chip {
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  color: #f0f0f0;
  font-size: 0.85rem;
  line-height: 1.6;
  border: 1px solid rgba(255, 255, 255, 0.2);
  text-transform: lowercase;
}

/* Logo container with QR icon */
.logo-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem; /* небольшой отступ между иконкой и названием */
  position: relative;
  z-index: 1; /* ниже, чем overlay-элементы справа */
}

/* User compact avatar next to logo */
.user-chip-wrap {
  position: relative;
  z-index: 2; /* выше, чем логотип */
  align-self: center; /* вертикально по центру навбара */
  margin-left: 12px; /* небольшой отступ от ссылок справа */
}

.user-chip {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid rgba(180, 180, 180, 0.45); /* серая граница для лучшей видимости */
  background: rgba(255, 255, 255, 0.12); /* немного светлее фон */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease;
}

.user-chip:hover {
  transform: scale(1.06);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.28);
  background: rgba(255, 255, 255, 0.16);
}

.user-chip__img {
  width: 28px;
  height: 28px;
  object-fit: cover;
  display: block;
}


.user-menu {
  position: absolute;
  top: 36px;
  right: 0;
  min-width: 200px;
  background: #333333; /* чуть светлее для контраста */
  border: 1px solid rgba(180, 180, 180, 0.35);
  border-radius: 10px;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.30);
  padding: 6px;
  z-index: 1001;
}

.user-menu__item {
  width: 100%;
  text-align: left;
  background: transparent;
  color: #eaeaea;
  border: none;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background .15s ease, color .15s ease;
}

.user-menu__item:hover {
  background: rgba(255, 255, 255, 0.10);
}

.user-menu__item--danger {
  color: #ff6b6b;
}



/* Вертикальный переключатель темы */
.theme-toggle-vert {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-right: 8px;
  align-items: center;
}

.ttv-btn {
  width: 24px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 11px;
  line-height: 1;
}

.ttv-btn.active {
  background: #4a6cf7;
  border-color: #4a6cf7;
}

.ttv-btn:hover {
  background: rgba(255, 255, 255, 0.16);
}

.qr-btn {
  padding: 0;
  margin: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  position: relative;
  z-index: 2; /* на случай перекрытий соседними элементами */
  pointer-events: auto;
}

/* .logo-img больше не используется для QR */

.main-nav--hidden {
  transform: translateY(-100%);
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: #fff;
  text-decoration: none;
  transition: opacity 0.3s ease;
}

.nav-links {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.nav-link {
  color: #fff;
  text-decoration: none;
  transition: opacity 0.3s ease;
  padding: 0.5rem;
  border-radius: 4px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
}

/* Cart button aligned with avatar size and styling */
.cart-btn {
  position: relative;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: transparent;
  border: none;
}

.cart-ico {
  display: inline-flex;
  width: 22px;
  height: 22px;
  color: var(--text);
  opacity: .85;
  filter: drop-shadow(0 0 0.5px rgba(0, 0, 0, .5));
}

.cart-ico :deep(svg) {
  width: 100%;
  height: 100%;
  display: block;
  fill: currentColor;
}

.cart-ico :deep(path) {
  stroke: currentColor;
  stroke-width: .6;
}

/* Badge and compact total label */
.cart-badge {
  position: absolute;
  left: -6px;
  bottom: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  z-index: 2;
}

/* DNS-like orange badge, always readable */
.cart-badge {
  background: #ff9800;
  color: #fff;
  border-color: rgba(0, 0, 0, .06);
}

.cart-btn:hover .cart-badge {
  filter: brightness(1.05);
}

.cart-total {
  position: absolute;
  top: -14px; /* place above icon without changing layout height */
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  font-size: 12px;
  font-weight: 700;
  color: var(--text);
  text-shadow: 0 1px 2px rgba(0, 0, 0, .35);
  pointer-events: none;
}

.cart-btn:hover .cart-ico {
  opacity: 1;
}

.nav-link:hover {
  opacity: 0.8;
}

.nav-links a {
  color: #fff;
  text-decoration: none;
  transition: opacity 0.3s ease;
}

/* Mobile adaptations: iPhone SE (<=360px) */
@media (max-width: 360px) {
  .main-nav {
    padding: 6px 8px;
    --header-height: 52px;
  }

  .logo {
    font-size: 1.25rem;
  }

  .logo {
    max-width: 45vw;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .brand-chip {
    display: none;
  }

  .nav-links {
    gap: 8px;
    position: absolute;
    right: 44px;
    top: 50%;
    transform: translateY(-50%);
    z-index: 5;
    pointer-events: auto;
  }

  .user-chip-wrap {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    z-index: 6;
    pointer-events: auto;
  }

  .nav-link {
    display: none;
  }

  /* hide verbose 'Контексты' on very small screens */
  .cart-total {
    top: -12px;
    font-size: 10px;
  }

  .header-spacer {
    height: 52px;
  }
}

/* Ultra small: <=320px */
@media (max-width: 320px) {
  .main-nav {
    padding: 4px 6px;
    --header-height: 50px;
  }

  .logo {
    max-width: 43vw;
  }

  .header-spacer {
    height: 50px;
  }

  .cart-total {
    top: -11px;
    font-size: 9.5px;
    max-width: 72px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .header-spacer {
    height: 50px;
  }
}

/* Legacy tiny: <=300px */
@media (max-width: 300px) {
  .main-nav {
    padding: 3px 5px;
    --header-height: 48px;
  }

  .logo {
    font-size: 1.05rem;
  }

  .logo {
    max-width: 40vw;
  }

  .cart-total {
    top: -10px;
    font-size: 9px;
    max-width: 64px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .cart-total {
    display: none;
  }

  /* ultra-compact: прячем сумму, чтобы не мешала кликам */
  .nav-links {
    right: 42px;
  }

  .user-chip-wrap {
    right: 6px;
  }

  .header-spacer {
    height: 48px;
  }
}

.burger {
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  padding: 8px;
  margin-left: 12px;
}

.burger span {
  width: 25px;
  height: 2px;
  background-color: #fff;
  transition: 0.3s ease;
}

.burger.is-active span:nth-child(1) {
  transform: translateY(8px) rotate(45deg);
}

.burger.is-active span:nth-child(2) {
  opacity: 0;
}

.burger.is-active span:nth-child(3) {
  transform: translateY(-8px) rotate(-45deg);
}

@media (max-width: 768px) {
  .burger {
    display: flex;
  }
}
</style>
