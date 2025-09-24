<template>
  <div class="header-container">
    <nav class="main-nav" :class="{ 'main-nav--hidden': !isHeaderVisible }">
      <div class="logo-wrap">
        <!-- Вертикальный переключатель темы -->
        <div class="theme-toggle-vert" @click.stop>
          <button class="ttv-btn" :class="{active: computedTheme === 'light' && themeMode !== 'auto'}"
                  @click="setTheme('light')" title="День">☀
          </button>
          <button class="ttv-btn" :class="{active: computedTheme === 'dark' && themeMode !== 'auto'}"
                  @click="setTheme('dark')" title="Ночь">🌙
          </button>
        </div>

        <button class="qr-btn" @click.stop="openQr" aria-label="Показать QR код" type="button">
          <!-- data URL to avoid network fetch and keep predictable sizing -->
          <img class="qr-img" :src="qrDataUrl" alt="QR code" width="28" height="28" />
        </button>
        <router-link to="/" class="logo" @click.stop> TenderBotStore</router-link>
      </div>

      <div class="burger" @click="toggleMenu" :class="{ 'is-active': isMenuOpen }">
        <span></span>
        <span></span>
        <span></span>
      </div>

      <div class="nav-links" :class="{ 'is-active': isMenuOpen }" @click="closeMenu">
        <router-link v-if="route.name !== 'Home'" to="/">Главная</router-link>
        <template v-if="authStore.isAuthenticated">
          <!-- Скрыли ссылки Профиль/Редактировать/Выйти: они доступны в меню аватара -->
          <router-link
            v-if="isAdminOrOwner"
            to="/staff"
            class="nav-link btn-primary"
          >Управление персоналом</router-link>
          <button class="nav-link btn-primary" type="button" @click.stop="openMiniCart">
            Корзина ({{ cartStore.items.length }})
          </button>
          <router-link v-if="isAdminOrOwner" to="/admin/archive">Корзина (архив)</router-link>
          <router-link v-if="isAdminOrOwner" to="/admin">Админ</router-link>
        </template>
        <template v-else>
          <button @click="openLogin" class="nav-link btn-primary">Войти</button>
          <button class="nav-link btn-primary" type="button" @click.stop="openMiniCart">
            Корзина ({{ cartStore.items.length }})
          </button>
          <a href="#" @click.prevent="openRegister">Регистрация</a>
        </template>
      </div>

      <!-- User avatar on the right side -->
      <div v-if="authStore.isAuthenticated" class="user-chip-wrap" @mouseenter="chipHover = true"
           @mouseleave="chipHover = false">
        <button :title="authStore.user?.username || 'Профиль'" class="user-chip" type="button" @click.stop="goProfile">
          <img v-if="authStore.user?.avatarUrl" :src="authStore.user.avatarUrl" alt="avatar" class="user-chip__img"
               height="28" width="28"/>
          <img v-else :src="userIcon" alt="user" class="user-chip__img user-chip__img--placeholder" height="28"
               width="28"/>
        </button>
        <transition name="fade-scale">
          <div v-if="chipHover" class="user-menu" @mouseenter="chipHover = true" @mouseleave="chipHover = false">
            <button class="user-menu__item" type="button" @click="goProfile">Профиль</button>
            <button class="user-menu__item user-menu__item--danger" type="button" @click="handleLogout">Выйти</button>
          </div>
        </transition>
      </div>
    </nav>
    <!-- Spacer to offset fixed header height -->
    <div class="header-spacer" aria-hidden="true"></div>
    <div v-if="isMenuOpen" class="nav-overlay" @click="closeMenu"></div>
    
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
  </div>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useAuthStore} from '../store/auth';
import {useCartStore} from '../store/cart';

import qrInline from '../assets/qr-code.svg?raw';
import userIcon from '../assets/user.svg';

const props = defineProps({
  isModalVisible: {
    type: Boolean,
    required: true
  }
});

const emit = defineEmits(['open-login-modal', 'open-register-modal', 'open-mini-cart']);
const route = useRoute();
const authStore = useAuthStore();
const cartStore = useCartStore();
const router = useRouter();
const qrInlineRef = ref(qrInline);
const qrDataUrl = computed(() =>
  'data:image/svg+xml;utf8,' + encodeURIComponent(qrInlineRef.value || '')
);

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
const isMenuOpen = ref(false);
const showQr = ref(false);
const chipHover = ref(false);
const isProfilePage = computed(() => route.name === 'Profile');
const isHeaderVisible = ref(true);
let lastScrollPosition = 0;

const isAdminOrOwner = computed(() => {
  if (!authStore.user || !authStore.user.roles) {
    return false;
  }
  return authStore.user.roles.includes('ADMIN') || authStore.user.roles.includes('OWNER');
});

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
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll);
  if (media && media.removeEventListener) {
    media.removeEventListener('change', () => {
    });
  }
  window.removeEventListener('keydown', onKeydown);
});

watch(themeMode, (v) => {
  localStorage.setItem(THEME_KEY, v);
  applyTheme();
});

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
  background: #2c2c2c;
  padding: 0.75rem 1.5rem;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: transform 0.3s ease-in-out;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  min-height: 60px; /* стабильная высота шапки */
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

/* Logo container with QR icon */
.logo-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem; /* небольшой отступ между иконкой и названием */
  position: relative;
  z-index: 2;
}

/* User compact avatar next to logo */
.user-chip-wrap {
  position: relative;
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
  margin-left: auto; /* сдвигаем блок ссылок вправо */
  display: flex;
  align-items: center; /* вертикальное выравнивание */
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

.nav-link:hover {
  opacity: 0.8;
}

.nav-links a {
  color: #fff;
  text-decoration: none;
  transition: opacity 0.3s ease;
}

.nav-links a:hover {
  opacity: 0.8;
}

.burger {
  display: none;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  padding: 8px;
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
  .nav-links {
    position: fixed;
    top: 0;
    right: -100%;
    width: 80%;
    height: 100vh;
    background: #2c2c2c;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: 2rem;
    padding: 2rem;
    transition: right 0.3s ease;
  }

  .nav-links.is-active {
    right: 0;
  }

  .burger {
    display: flex;
  }

  /* Анимация бургера в крестик */
  .burger.is-active span:nth-child(1) {
    transform: rotate(45deg) translate(5px, 5px);
  }
  .burger.is-active span:nth-child(2) {
    opacity: 0;
  }
  .burger.is-active span:nth-child(3) {
    transform: rotate(-45deg) translate(7px, -6px);
  }
}
</style>
