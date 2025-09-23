<template>
  <div class="env-test">
    <h3>Environment Variables</h3>
    <div class="mb-3">
      <button class="btn btn-sm btn-primary" @click="loadEnv">Reload Environment</button>
    </div>
    
    <div v-if="loading" class="alert alert-info">
      <div class="spinner-border spinner-border-sm me-2" role="status"></div>
      Loading environment variables...
    </div>
    
    <div v-if="error" class="alert alert-danger">
      <h5>Error loading environment:</h5>
      <pre>{{ error }}</pre>
    </div>
    
    <div class="table-responsive">
      <table class="table table-bordered table-striped">
        <thead>
          <tr>
            <th>Variable</th>
            <th>Value</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(value, key) in envVars" :key="key">
            <td><code>{{ key }}</code></td>
            <td>
              <span v-if="key.toLowerCase().includes('key') || key.toLowerCase().includes('secret') || key.toLowerCase().includes('password')">
                ********
              </span>
              <span v-else>
                {{ value }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue';

const envVars = ref({});
const loading = ref(true);
const error = ref(null);

const loadEnv = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    // Get all environment variables that start with VITE_
    const env = import.meta.env;
    const viteVars = {};
    
    for (const key in env) {
      if (key.startsWith('VITE_')) {
        viteVars[key] = env[key];
      }
    }
    
    // Also get some important build info
    viteVars['NODE_ENV'] = import.meta.env.MODE;
    viteVars['BASE_URL'] = import.meta.env.BASE_URL;
    viteVars['DEV'] = import.meta.env.DEV;
    viteVars['PROD'] = import.meta.env.PROD;
    
    envVars.value = viteVars;
  } catch (err) {
    console.error('Error loading environment:', err);
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

// Load environment variables on component mount
onMounted(loadEnv);
</script>

<style scoped>
.env-test {
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin: 20px 0;
  background-color: #f8f9fa;
}

.table {
  background-color: white;
}

code {
  background-color: #f1f1f1;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: monospace;
}
</style>
