<template>
  <div class="tags-view">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-12">
          <div class="card">
            <div class="card-header">
              <h2 class="mb-0">Управление тегами</h2>
            </div>
            <div class="card-body">
              <div v-if="loadingBrand" class="text-center py-4">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">Загрузка...</span>
                </div>
                <p class="mt-2">Загрузка информации о бренде...</p>
              </div>
              
              <div v-else-if="!currentBrand" class="alert alert-warning">
                Бренд не найден или у вас нет прав на управление тегами.
              </div>
              
              <div v-else>
                <div class="brand-info mb-4">
                  <h3>{{ currentBrand.name }}</h3>
                  <p class="text-muted">Управление тегами для бренда</p>
                </div>
                
                <tag-manager :brand-id="currentBrand.id" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {computed, ref} from 'vue';
import {useRoute} from 'vue-router';
import {useAuthStore} from '@/store/auth';
import TagManager from '@/components/tags/TagManager.vue';

export default {
  name: 'TagsView',
  components: {
    TagManager
  },
  setup() {
    const route = useRoute();
    const authStore = useAuthStore();
    
    // Получаем brandId из параметров маршрута
    const brandId = computed(() => parseInt(route.params.brandId));
    
    // Создаем моковый объект бренда на основе ID
    const currentBrand = computed(() => ({
      id: brandId.value,
      name: `Бренд #${brandId.value}`,
      description: 'Информация о бренде'
    }));
    
    // Проверка прав доступа
    const checkPermissions = () => {
      // В реальном приложении здесь должна быть проверка прав пользователя
      return authStore.isAuthenticated;
    };
    
    return {
      currentBrand,
      loadingBrand: ref(false), // Упрощаем, так как загрузка не требуется
      checkPermissions
    };
  }
};
</script>

<style scoped>
.tags-view {
  padding: 20px 0;
  min-height: calc(100vh - 120px);
}

.card {
  border: none;
  border-radius: 10px;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.05);
  margin-bottom: 30px;
}

.card-header {
  background-color: #f8f9fa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding: 20px 25px;
  border-top-left-radius: 10px !important;
  border-top-right-radius: 10px !important;
}

.card-header h2 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 600;
  color: #2c3e50;
}

.card-body {
  padding: 25px;
}

.brand-info h3 {
  color: #2c3e50;
  margin-bottom: 5px;
}

.brand-info p {
  margin-bottom: 0;
  font-size: 0.9rem;
}

.alert {
  border-radius: 8px;
  margin-bottom: 0;
}

.spinner-border {
  width: 2rem;
  height: 2rem;
  border-width: 0.2em;
}

/* Адаптивность */
@media (max-width: 768px) {
  .card-header h2 {
    font-size: 1.5rem;
  }
  
  .card-body {
    padding: 20px 15px;
  }
}
</style>
