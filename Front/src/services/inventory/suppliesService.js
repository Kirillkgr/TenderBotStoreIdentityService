import apiClient from '../api';
import {useAuthStore} from '../../store/auth';

export const listSupplies = (params = {}) => apiClient.get('/auth/v1/inventory/supplies', {params});
// Backend expects POST /search with request params (not body)
export const searchSupplies = (params = {}) => apiClient.post('/auth/v1/inventory/supplies/search', null, {params});
export const getSupply = (id) => apiClient.get(`/auth/v1/inventory/supplies/${id}`);
export const createSupply = (payload) => apiClient.post('/auth/v1/inventory/supplies', payload);
export const updateSupply = (id, payload) => apiClient.put(`/auth/v1/inventory/supplies/${id}`, payload);
export const postSupply = (id) => apiClient.post(`/auth/v1/inventory/supplies/${id}/post`);
export const cancelSupply = (id) => apiClient.post(`/auth/v1/inventory/supplies/${id}/cancel`);

// Smart fetch: tries alternative endpoints often used by backends
export async function fetchSuppliesSmart(params = {}) {
  // 1) Primary: POST /search (часто так сделано на Spring-API)
  try {
    return await searchSupplies(params);
  } catch (e1) {
    const s1 = e1?.response?.status;
    // 2) If method not allowed/404 — пробуем GET-варианты
    if (s1 === 404 || s1 === 405 || s1 === 501) {
      try {
        return await listSupplies(params);
      } catch (_) {
      }
      try {
        return await apiClient.get('/auth/v1/inventory/supplies/list', {params});
      } catch (_) {
      }
      try {
        return await apiClient.get('/auth/v1/admin/inventory/supplies', {params});
      } catch (_) {
      }
    }
    // 3) If 401 — пробуем refresh и повторяем оба варианта (POST/GET)
    if (s1 === 401) {
      try {
        await apiClient.post('/auth/v1/refresh');
        try {
          return await searchSupplies(params);
        } catch (_) {
        }
        try {
          return await listSupplies(params);
        } catch (_) {
        }
      } catch (_) {
      }
    }
    // 4) Ничего не сработало — выбрасываем исходную ошибку
    throw e1;
  }
}
