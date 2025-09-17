<template>
  <div class="header-container">
    <nav class="main-nav" :class="{ 'main-nav--hidden': !isHeaderVisible }">
      <router-link to="/" class="logo">TenderBotStore</router-link>

      <div class="burger" @click="toggleMenu" :class="{ 'is-active': isMenuOpen }">
        <span></span>
        <span></span>
        <span></span>
      </div>

      <div class="nav-links" :class="{ 'is-active': isMenuOpen }" @click="closeMenu">
        <router-link v-if="route.name !== 'Home'" to="/">Главная</router-link>
        <template v-if="authStore.isAuthenticated">
          <router-link v-if="route.name !== 'Profile'" to="/profile">Профиль</router-link>
          <button
            v-if="isProfilePage && !isModalVisible"
            @click="$emit('open-edit-profile-modal')"
            class="nav-link btn-primary"
          >Редактировать профиль</button>
          <router-link to="/cart">Корзина ({{ cartStore.items.length }})</router-link>
          <router-link v-if="isAdminOrOwner" to="/admin">Админ</router-link>
          <a @click="handleLogout" href="#">Выйти ({{ authStore.user?.username }})</a>
        </template>
        <template v-else>
          <button @click="openLogin" class="nav-link btn-primary">Войти</button>
          <a href="#" @click.prevent="openRegister">Регистрация</a>
        </template>
      </div>
    </nav>
    <div v-if="isMenuOpen" class="nav-overlay" @click="closeMenu"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '../store/auth';
import { useCartStore } from '../store/cart';
import { useRouter } from 'vue-router';

const props = defineProps({
  isModalVisible: {
    type: Boolean,
    required: true
  }
});

const emit = defineEmits(['open-login-modal', 'open-register-modal', 'open-edit-profile-modal']);
const route = useRoute();
const authStore = useAuthStore();
const cartStore = useCartStore();
const router = useRouter();
const isMenuOpen = ref(false);
const isProfilePage = computed(() => route.name === 'Profile');
const isHeaderVisible = ref(true);
let lastScrollPosition = 0;

const isAdminOrOwner = computed(() => {
  if (!authStore.user || !authStore.user.roles) {
    return false;
  }
  return authStore.user.roles.includes('ADMIN') || authStore.user.roles.includes('OWNER');
});

function handleLogout() {
  authStore.logout();
  isMenuOpen.value = false;
  router.push('/login');
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

function handleScroll() {
  const currentScrollPosition = window.pageYOffset || document.documentElement.scrollTop;
  if (currentScrollPosition < 0) {
    return;
  }
  // Показываем шапку, если скроллим вверх или находимся в самом верху
  isHeaderVisible.value = currentScrollPosition < lastScrollPosition || currentScrollPosition < 10;
  lastScrollPosition = currentScrollPosition;
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll);
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll);
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
  padding: 1rem 2rem;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: transform 0.3s ease-in-out;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

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
  display: flex;
  gap: 2rem;
  align-items: center;
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
