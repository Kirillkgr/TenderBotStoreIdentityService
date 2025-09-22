<template>
  <div id="app">
    <AppHeader
      :is-modal-visible="isModalVisible"
      @open-login-modal="openModal('LoginView')"
      @open-register-modal="openModal('RegisterView')"
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
  </div>
</template>

<script setup>
import {computed, ref, shallowRef, watch} from 'vue';
import {useRoute} from 'vue-router';
import AppHeader from './components/AppHeader.vue';
import Modal from './components/Modal.vue';
import LoginView from './views/LoginView.vue';
import RegisterView from './views/RegisterView.vue';
import ProfileEditModal from './components/modals/ProfileEditModal.vue';

const route = useRoute();

const components = {
  LoginView,
  RegisterView,
  ProfileEditModal
};

const isModalVisible = ref(false);

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
watch(
  () => route.name,
  () => {
    document.title = `TenderBotStore - ${route.meta.title || 'Главная'}`;
  },
  { immediate: true }
);
</script>

<style>
body {
  margin: 0;
  font-family: Avenir, Helvetica, Arial, sans-serif;
  background-color: #242424;
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


</style>
