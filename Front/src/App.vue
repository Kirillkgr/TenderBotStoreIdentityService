<template>
  <div id="app" :class="appClass">
    <Sidebar/>
    <AppHeader
      :is-modal-visible="isModalVisible"
      @open-login-modal="openModal('LoginView')"
      @open-register-modal="openModal('RegisterView')"
      @open-mini-cart="showMiniCart = true"
    />
    <main class="main-content">
<!--      <h1 v-if="pageTitle" class="page-title">{{ pageTitle }}</h1>-->
      <router-view />
    </main>



    <Modal :is-modal-visible="isModalVisible" @close="closeModal">
      <template #header>
        <h3>{{ modalTitle }}</h3>
      </template>
      <template #content>
        <component :is="modalContent" @success="handleSuccess" />
      </template>
    </Modal>

    <MiniCartModal v-model="showMiniCart" @open-login="openModal('LoginView')" @open-checkout="openCheckout"/>
  </div>
</template>

<script setup>
import {computed, ref, shallowRef, watch, onMounted, onBeforeUnmount} from 'vue';
import {useRoute} from 'vue-router';
import AppHeader from './components/AppHeader.vue';
import Sidebar from './components/Sidebar.vue';
import Modal from './components/Modal.vue';
import LoginView from './views/LoginView.vue';
import RegisterView from './views/RegisterView.vue';
import ProfileEditModal from './components/modals/ProfileEditModal.vue';
import CheckoutModal from './components/modals/CheckoutModal.vue';
import MiniCartModal from './components/modals/MiniCartModal.vue';
import {getNotificationsClient} from './services/notifications';
import {useAuthStore} from './store/auth';
import {useUiStore} from './store/ui';

const route = useRoute();
const ui = useUiStore();
const authStore = useAuthStore();

const components = {
  LoginView,
  RegisterView,
  ProfileEditModal
  , CheckoutModal
};

const isModalVisible = ref(false);
const showMiniCart = ref(false);

const appClass = computed(() => {
  // Apply left padding only when desktop AND sidebar is open
  if (!ui.isDesktop || !ui.sidebarOpen) return '';
  return ui.sidebarCollapsed ? 'layout-rail' : 'layout-docked';
});

const pageTitle = computed(() => {
  if (isModalVisible.value && modalContent.value === ProfileEditModal) {
    return '';
  }
  return route.meta.title || '';
});
const modalContent = shallowRef(null);
const modalTitle = ref('');

function openModal(componentName) {
  console.log('App: openModal called with', componentName);
  console.log('App: modalContent before:', modalContent.value);
  
  modalContent.value = components[componentName];
  if (componentName === 'LoginView') {
    modalTitle.value = 'Вход в аккаунт';
  } else if (componentName === 'RegisterView') {
    modalTitle.value = 'Создание аккаунта';
  } else if (componentName === 'ProfileEditModal') {
    modalTitle.value = 'Редактирование профиля';
  } else {
    modalTitle.value = '';
  }
  isModalVisible.value = true;
  
  console.log('App: modalContent after:', modalContent.value);
  console.log('App: isModalVisible:', isModalVisible.value);
}

function closeModal() {
  isModalVisible.value = false;
  modalContent.value = null;
}

function handleSuccess() {
  closeModal();
}

function openCheckout() {
  showMiniCart.value = false;
  openModal('CheckoutModal');
}
watch(
  () => route.name,
  () => {
    document.title = `TenderBotStore - ${route.meta.title || 'Главная'}`;
  },
  { immediate: true }
);

// Автозапуск long-poll уведомлений для авторизованных пользователей
// Запускаем при isAuthenticated; перехватчики сами обработают refresh 401
watch(
    () => authStore.isAuthenticated,
    (isAuth) => {
      const client = getNotificationsClient();
      if (!isAuth) {
        client.stop();
        return;
      }
      client.start();
    },
    {immediate: true}
);

// Один раз пытаемся восстановить сессию при загрузке приложения,
// чтобы accessToken появился до старта long-poll
onMounted(async () => {
  try {
    if (authStore.isAuthenticated && !authStore.accessToken) {
      await authStore.restoreSession();
    }
  } catch (_) {
  }
  // Sidebar behavior: open on desktop, close on mobile
  const handleResize = () => {
    const desktop = window.innerWidth >= 1200;
    ui.setDesktop(desktop);
    if (desktop) {
      if (authStore.isAuthenticated) {
        ui.openSidebar();
        ui.setCollapsed(false); // по умолчанию развернуто на десктопе
      } else {
        ui.closeSidebar(); // гость: сайдбар закрыт до авторизации
      }
    } else {
      ui.closeSidebar();
    }
  };
  handleResize();
  window.addEventListener('resize', handleResize);
  // store remover
  cleanupFns.push(() => window.removeEventListener('resize', handleResize));

  // Open Login modal from anywhere via custom event
  const openLoginHandler = () => openModal('LoginView');
  window.addEventListener('open-login-modal', openLoginHandler);
  cleanupFns.push(() => window.removeEventListener('open-login-modal', openLoginHandler));
  // Auto-open sidebar after successful login on desktop; close on logout
  const unsubAuth = authStore.$subscribe((_mutation, state) => {
    try {
      const isAuth = state.isAuthenticated;
      if (ui.isDesktop) {
        if (isAuth) ui.openSidebar(); else ui.closeSidebar();
      } else {
        ui.closeSidebar();
      }
    } catch (_) {
    }
  });
  cleanupFns.push(() => {
    try {
      unsubAuth();
    } catch (_) {
    }
  });
});

const cleanupFns = [];
onBeforeUnmount(() => {
  cleanupFns.forEach(fn => {
    try {
      fn();
    } catch (_) {
    }
  });
});
</script>

<style>
body {
  margin: 0;
  font-family: Avenir, Helvetica, Arial, sans-serif;
  background-color: var(--bg);
  color: #ecf0f1;
}

#app {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  position: relative;
  height: 100vh;
}

.main-content {
  padding-top: 80px;
  text-align: center;
}
.page-title {
  margin-bottom: 1.5rem;
  font-weight: 400;
}



/* Стили для ссылок в футере или других местах, если они будут */
nav a {
  font-weight: bold;
  color: #ecf0f1; /* Светлый текст для ссылок */
}

nav a.router-link-exact-active {
  color: #4AAE9B; /* Акцентный цвет */
}

/* Global theme variables */
/* Defaults (fallback) */
:root {
  --bg: #242424;
  --text: #eaeaea;
  --muted: #cfd8dc;
  --active-bg: rgba(255, 255, 255, .08);
  --sidebar-bg: #1f1f1f;
  --sidebar-border: rgba(255, 255, 255, .08);
  --sidebar-width: 320px;
  --sidebar-rail: 64px;
}

/* Dark theme */
html.theme-dark {
  --bg: #242424;
  --text: #eaeaea;
  --muted: #cfd8dc;
  --active-bg: rgba(255, 255, 255, .08);
  --sidebar-bg: #1f1f1f;
  --sidebar-border: rgba(255, 255, 255, .08);
}

/* Light theme */
html.theme-light {
  --bg: #f7f8fa;
  --text: #1f2937;
  --muted: #6b7280;
  --active-bg: rgba(0, 0, 0, .06);
  --sidebar-bg: #ffffff;
  --sidebar-border: rgba(0, 0, 0, .08);
}

/* Layout adjustments for left sidebar */
#app.layout-docked .main-content {
  padding-left: calc(var(--sidebar-width) + 16px);
}

#app.layout-rail .main-content {
  padding-left: calc(var(--sidebar-rail) + 16px);
}

@media (max-width: 1199px) {
  #app .main-content {
    padding-left: 0;
  }
}
</style>
