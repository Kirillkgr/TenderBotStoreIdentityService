<template>
  <div class="api-test">
    <h3>API Connection Test</h3>
    
    <div class="mb-3">
      <h4>Environment Variables:</h4>
      <pre>VITE_API_BASE_URL: {{ env.VITE_API_BASE_URL }}</pre>
    </div>
    
    <div class="mb-3">
      <button class="btn btn-primary me-2" @click="testGetBrands">Test GET /auth/v1/brands</button>
      <button class="btn btn-secondary" @click="testAuth">Test Authentication</button>
    </div>
    
    <div v-if="loading" class="alert alert-info">
      <div class="spinner-border spinner-border-sm me-2" role="status"></div>
      Loading...
    </div>
    
    <div v-if="error" class="alert alert-danger">
      <h5>Error:</h5>
      <pre>{{ error }}</pre>
      <div v-if="errorDetails">
        <h6>Details:</h6>
        <pre>{{ errorDetails }}</pre>
      </div>
    </div>
    
    <div v-if="responseData" class="alert alert-success">
      <h5>Response:</h5>
      <pre>{{ responseData }}</pre>
    </div>
    
    <div class="request-history mt-4">
      <h5>Request History:</h5>
      <ul class="list-group">
        <li v-for="(req, index) in requestHistory" :key="index" class="list-group-item">
          <strong>{{ req.method }} {{ req.url }}</strong>
          <div v-if="req.error" class="text-danger">Error: {{ req.error }}</div>
          <div v-else class="text-success">Success ({{ req.status }})</div>
        </li>
      </ul>
    </div>

    <div class="mt-4">
      <h5>Context Cookie (ctx)</h5>
      <div class="mb-2 d-flex gap-2 align-items-end">
        <div>
          <label class="form-label">masterId</label>
          <input v-model.number="ctxMasterId" class="form-control" placeholder="e.g. 1" type="number"/>
        </div>
        <div>
          <label class="form-label">brandId</label>
          <input v-model.number="ctxBrandId" class="form-control" placeholder="optional" type="number"/>
        </div>
        <div>
          <label class="form-label">pickupPointId</label>
          <input v-model.number="ctxPickupId" class="form-control" placeholder="optional" type="number"/>
        </div>
      </div>
      <div class="mb-3">
        <button class="btn btn-secondary me-2" @click="onSetCtx">POST /auth/v1/context</button>
        <button class="btn btn-secondary" @click="onClearCtx">DELETE /auth/v1/context</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref} from 'vue';
import axios from 'axios';
import {useAuthStore} from '@/store/auth';
import {useToast} from 'vue-toastification';

const env = import.meta.env;
const toast = useToast();
const authStore = useAuthStore();

const loading = ref(false);
const error = ref(null);
const errorDetails = ref(null);
const responseData = ref(null);
const requestHistory = ref([]);
const ctxMasterId = ref();
const ctxBrandId = ref();
const ctxPickupId = ref();

const addToHistory = (method, url, status, errorMsg = null) => {
  requestHistory.value.unshift({
    method,
    url,
    status: errorMsg ? 'Error' : status,
    error: errorMsg,
    timestamp: new Date().toISOString()
  });};

const testGetBrands = async () => {
  loading.value = true;
  error.value = null;
  errorDetails.value = null;
  responseData.value = null;
  
  try {
    console.log('Testing GET /auth/v1/brands...');
    const response = await axios.get(`${import.meta.env.VITE_API_BASE_URL}/auth/v1/brands`, {
      withCredentials: true,
      headers: {
        'Authorization': `Bearer ${authStore.accessToken}`
      }
    });
    
    console.log('Response:', response);
    responseData.value = response.data;
    addToHistory('GET', '/auth/v1/brands', response.status);
    toast.success('Successfully fetched brands');
  } catch (err) {
    console.error('Error fetching brands:', err);
    error.value = err.message;
    errorDetails.value = {
      status: err.response?.status,
      statusText: err.response?.statusText,
      data: err.response?.data,
      config: {
        url: err.config?.url,
        method: err.config?.method,
        headers: err.config?.headers
      }
    };
    addToHistory('GET', '/auth/v1/brands', err.response?.status, err.message);
    toast.error('Failed to fetch brands');
  } finally {
    loading.value = false;
  }
};

const testAuth = async () => {
  loading.value = true;
  error.value = null;
  errorDetails.value = null;
  responseData.value = null;
  
  try {
    console.log('Testing authentication...');
    const response = await axios.get(`${import.meta.env.VITE_API_BASE_URL}/auth/v1/me`, {
      withCredentials: true,
      headers: {
        'Authorization': `Bearer ${authStore.accessToken}`
      }
    });
    
    console.log('Auth response:', response);
    responseData.value = response.data;
    addToHistory('GET', '/auth/v1/me', response.status);
    toast.success('Authentication successful');
  } catch (err) {
    console.error('Auth error:', err);
    error.value = err.message;
    errorDetails.value = {
      status: err.response?.status,
      statusText: err.response?.statusText,
      data: err.response?.data,
      config: {
        url: err.config?.url,
        method: err.config?.method,
        headers: err.config?.headers
      }
    };
    addToHistory('GET', '/auth/v1/me', err.response?.status, err.message);
    toast.error('Authentication failed');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.api-test {
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin: 20px 0;
}

pre {
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}

.alert {
  margin-top: 15px;
}

.request-history {
  max-height: 300px;
  overflow-y: auto;
}
</style>
