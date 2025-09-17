import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useBrandStore = defineStore('brand', () => {
  // State
  const currentBrand = ref(null);
  const loading = ref(false);
  const error = ref(null);

  // Mock function to get brand by ID
  // In a real app, this would be an API call
  const getBrandById = async (brandId) => {
    loading.value = true;
    error.value = null;
    
    try {
      // This is a mock implementation
      // Replace with actual API call in production
      return new Promise((resolve) => {
        setTimeout(() => {
          const mockBrand = {
            id: parseInt(brandId),
            name: `Бренд #${brandId}`,
            description: 'Описание бренда',
            logo: null,
            createdAt: new Date().toISOString()
          };
          currentBrand.value = mockBrand;
          resolve(mockBrand);
        }, 300);
      });
    } catch (err) {
      console.error('Ошибка при загрузке бренда:', err);
      error.value = 'Не удалось загрузить информацию о бренде';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  // Reset store state
  const reset = () => {
    currentBrand.value = null;
    loading.value = false;
    error.value = null;
  };

  return {
    // State
    currentBrand,
    loading,
    error,
    
    // Getters
    isBrandLoaded: computed(() => !!currentBrand.value),
    
    // Actions
    getBrandById,
    reset
  };
});
