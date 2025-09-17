<template>
  <div class="auth-test">
    <h3>Authentication Status</h3>
    
    <div class="mb-3">
      <button class="btn btn-sm btn-primary me-2" @click="checkAuth">Check Auth Status</button>
      <button class="btn btn-sm btn-secondary" @click="checkLocalStorage">Check Local Storage</button>
    </div>
    
    <div v-if="loading" class="alert alert-info">
      <div class="spinner-border spinner-border-sm me-2" role="status"></div>
      Loading...
    </div>
    
    <div v-if="error" class="alert alert-danger">
      <h5>Error:</h5>
      <pre>{{ error }}</pre>
    </div>
    
    <div v-if="authStatus" class="alert" :class="authStatus.isAuthenticated ? 'alert-success' : 'alert-warning'">
      <h5>Authentication Status:</h5>
      <pre>{{ authStatus }}</pre>
    </div>
    
    <div v-if="localStorageData" class="alert alert-secondary">
      <h5>Local Storage:</h5>
      <pre>{{ localStorageData }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { useAuthStore } from '@/store/auth';
import { useToast } from 'vue-toastification';

const toast = useToast();
const authStore = useAuthStore();

const loading = ref(false);
const error = ref(null);
const authStatus = ref(null);
const localStorageData = ref(null);

const checkAuth = async () => {
  loading.value = true;
  error.value = null;
  authStatus.value = null;
  
  try {
    // Check auth store
    const storeData = {
      isAuthenticated: authStore.isAuthenticated,
      user: authStore.user,
      accessToken: authStore.accessToken ? '***' + authStore.accessToken.slice(-10) : null,
      refreshToken: authStore.refreshToken ? '***' + authStore.refreshToken.slice(-10) : null
    };
    
    // Check API auth status
    let apiStatus = { status: 'Not checked' };
    try {
      const response = await axios.get(`${import.meta.env.VITE_API_BASE_URL}/auth/v1/me`, {
        withCredentials: true,
        headers: {
          'Authorization': `Bearer ${authStore.accessToken}`
        }
      });
      apiStatus = {
        status: 'Authenticated',
        user: response.data
      };
    } catch (apiError) {
      apiStatus = {
        status: 'Not authenticated',
        error: apiError.response?.statusText || apiError.message
      };
    }
    
    authStatus.value = {
      store: storeData,
      api: apiStatus,
      isAuthenticated: storeData.isAuthenticated && apiStatus.status === 'Authenticated'
    };
    
    toast.success('Auth status checked');
  } catch (err) {
    console.error('Auth check error:', err);
    error.value = err.message;
    toast.error('Failed to check auth status');
  } finally {
    loading.value = false;
  }
};

const checkLocalStorage = () => {
  try {
    const data = {};
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      let value = localStorage.getItem(key);
      
      // Mask sensitive data
      if (key.toLowerCase().includes('token') || key.toLowerCase().includes('auth')) {
        value = value ? '***' + value.slice(-10) : null;
      }
      
      data[key] = value;
    }
    
    localStorageData.value = data;
    toast.success('Local storage checked');
  } catch (err) {
    console.error('Local storage error:', err);
    error.value = err.message;
    toast.error('Failed to check local storage');
  }
};
</script>

<style scoped>
.auth-test {
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin: 20px 0;
  background-color: #f8f9fa;
}

pre {
  background-color: #f1f1f1;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 300px;
  overflow-y: auto;
}

.alert {
  margin-top: 15px;
}
</style>
