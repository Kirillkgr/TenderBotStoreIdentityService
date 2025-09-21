<template>
  <!-- Модальные окна -->
  <CreateBrandModal
      v-if="showCreateBrandModal"
      @close="showCreateBrandModal = false"
      @saved="handleBrandCreated"
  />

  <CreateGroupModal
      v-if="showCreateGroupModal"
      :brands="brands"
      :brandId="selectedBrand"
      :parentId="selectedTag ? selectedTag.id : currentParentId"
      @close="showCreateGroupModal = false"
      @saved="handleTagCreated"
  />

  <EditGroupModal
      v-if="showEditGroupModal && selectedTag"
      :brands="brands"
      :tag="selectedTag"
      :brandId="selectedBrand"
      @close="() => { showEditGroupModal = false; selectedTag = null; }"
      @saved="handleTagCreated"
  />

  <CreateProductModal
      v-if="showCreateProductModal"
      :brands="brands"
      :selectedBrand="selectedBrand"
      @close="showCreateProductModal = false"
      @saved="handleProductCreated"
  />

  <ProductPreviewModal
      v-if="showProductPreview"
      :product="previewProduct"
      width="720px"
      height="480px"
      @close="showProductPreview = false"
      @edit="openEditFromPreview"
  />

  <!-- Модалка редактирования товара (админ) -->
  <EditProductModal
      v-if="showEditProductModal && editProduct"
      :model-value="showEditProductModal"
      @update:modelValue="(v) => showEditProductModal = v"
      :product="editProduct"
      :brands="brands"
      :groupOptions="groupOptions"
      :tagOptions="tags"
      :theme="computedTheme"
      @close="showEditProductModal = false"
      @save="onProductSave"
      @delete="onProductDelete"
  />

  <div class="admin-panel admin-scope">
    <h1>Панель администратора</h1>

    <!-- Переключатель темы: Авто / День / Ночь -->
    <div class="theme-toggle">
      <span class="tt-label">Тема:</span>
      <div class="tt-group">
        <button class="tt-btn" :class="{ active: themeMode === 'auto' }" @click="setTheme('auto')">Авто</button>
        <button class="tt-btn" :class="{ active: themeMode === 'light' }" @click="setTheme('light')">День</button>
        <button class="tt-btn" :class="{ active: themeMode === 'dark' }" @click="setTheme('dark')">Ночь</button>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Загрузка...</span>
      </div>
      <p>Загрузка данных...</p>
    </div>

    <div v-if="brandsError" class="alert alert-danger">
      {{ brandsError }}
    </div>

    <div class="actions-container">
      <button
          class="admin-action-btn"
          @click="showCreateBrandModal = true"
          :disabled="loading"
      >
        + Создать бренд
      </button>
      <button
          class="admin-action-btn"
          @click="showCreateGroupModal = true"
          :disabled="!selectedBrand || loading"
      >
        + Создать тег
      </button>
      <button
          class="admin-action-btn secondary"
          @click="showCreateProductModal = true"
          :disabled="loading"
      >
        + Создать товар
      </button>
    </div>

    <!-- Debug Info -->
    <div v-if="false" class="debug-info p-3 mb-3 bg-light border rounded">
      <h5>Debug Information</h5>
      <p><strong>Loading:</strong> {{ loading }}, <strong>Brands Count:</strong> {{ brands.length }}</p>
      <p><strong>Selected Brand:</strong> {{ selectedBrand }}</p>
      <p><strong>Tags Count:</strong> {{ tags.length }}, <strong>Tag Loading:</strong> {{ tagLoading }}</p>
      <button @click="console.log('Brands:', brands)" class="btn btn-sm btn-secondary me-2">Log Brands</button>
      <button @click="console.log('Tags:', tags)" class="btn btn-sm btn-secondary">Log Tags</button>
    </div>

    <div v-if="!loading && !brandsError" class="brand-selector-container">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <h2 class="brand-selector-title mb-0">Бренды ({{ brands.length }})</h2>
        <button
            class="btn btn-sm btn-outline-primary"
            @click="fetchBrands"
            :disabled="loading"
            title="Обновить список брендов"
        >
          <i class="bi bi-arrow-repeat"></i>
        </button>
      </div>

      <div v-if="brands.length === 0" class="alert alert-info">
        Нет доступных брендов. Создайте новый бренд, чтобы начать работу.
      </div>

      <div v-else class="brand-list-scroll">
        <button
            v-for="brand in brands"
            :key="brand.id"
            :class="[
            'brand-chip',
            { 
              'active': selectedBrand === brand.id,
              'owner': brand.role === 'OWNER' || brand.role === 'ROLE_OWNER'
            }
          ]"
            @click="selectBrand(brand.id)"
            :title="brand.description || brand.name"
        >
          <span class="brand-name">{{ brand.name }}</span>
          <span v-if="brand.role === 'OWNER' || brand.role === 'ROLE_OWNER'"
                class="role-badge ms-2">
            <i class="bi bi-shield-lock"></i>
          </span>
        </button>
      </div>

      <!-- Текущий путь навигации по тегам (кликабельные крошки) -->
      <div v-if="selectedBrand" class="current-path mt-2 d-flex align-items-center flex-wrap gap-1">
        <i class="bi bi-folder2-open me-2 text-muted"></i>
        <template v-for="(item, index) in breadcrumbs" :key="'hpath-' + index">
          <button
              class="path-chip"
              :class="{ active: index === breadcrumbs.length - 1 }"
              @click="navigateToBreadcrumb(item, index)"
              :title="index === 0 ? 'Перейти к корню' : 'Перейти к ' + item.name"
          >
            {{ index === 0 ? 'Корень' : item.name }}
          </button>
          <span v-if="index < breadcrumbs.length - 1" class="path-sep">/</span>
        </template>
      </div>
    </div>

    <div v-if="selectedBrand" class="tag-manager-container mt-4">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="mb-0">
          Управление тегами
          <span v-if="!tagLoading" class="badge bg-secondary ms-2">
            {{ tags.length }} {{ formatWord(tags.length, ['тег', 'тега', 'тегов']) }}
          </span>
        </h2>

        <div class="d-flex gap-2">
          <button
              class="btn btn-sm btn-outline-secondary"
              @click="fetchTags(selectedBrand, currentParentId)"
              :disabled="tagLoading"
              title="Обновить теги"
          >
            <i class="bi bi-arrow-repeat"></i>
          </button>
          <button
              class="btn btn-sm btn-primary"
              @click="addChildTag()"
              :disabled="tagLoading"
          >
            <i class="bi bi-plus-circle me-1"></i> Создать тег
          </button>
        </div>
      </div>

      <!-- Убрали дублирующиеся хлебные крошки внутри белого блока -->

      <!-- Состояние загрузки -->
      <div v-if="tagLoading" class="text-center py-4">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Загрузка...</span>
        </div>
        <p class="mt-2">Загрузка тегов...</p>
      </div>

      <!-- Ошибка загрузки -->
      <div v-else-if="tagError" class="alert alert-danger">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        {{ tagError }}
        <button
            type="button"
            class="btn-close float-end"
            @click="tagError = null"
            aria-label="Close"
        ></button>
      </div>

      <!-- Список тегов -->
      <div v-else class="tag-manager-content">
        <div v-if="tags.length === 0" class="alert alert-info">
          <i class="bi bi-info-circle-fill me-2"></i>
          {{ currentParentId === 0 ? 'Создайте первый тег для этого бренда' : 'В этой папке пока нет тегов' }}
        </div>

        <div v-else class="list-group">
          <div
              v-for="tag in tags"
              :key="tag.id"
              class="list-group-item list-group-item-action d-flex justify-content-between align-items-center tag-row"
              :class="{ active: selectedTag?.id === tag.id }"
              @click="selectTagRow(tag)"
              @dblclick.stop="navigateToTag(tag.id)"
              :title="tag.hasChildren ? 'Двойной клик: открыть. Один клик: выбрать для вложенного' : 'Один клик: выбрать для вложенного'"
          >
            <div class="d-flex align-items-center">
              <i :class="['me-2', tag.icon || 'bi-tag']"></i>
              <span class="tag-title">
                {{ tag.name }}
                <button
                  class="btn btn-sm btn-outline-secondary ms-2"
                  @click.stop="editTag(tag)"
                  title="Изменить тег"
                >
                  Изменить
                </button>
              </span>

              <span v-if="tag.childrenCount > 0" class="badge bg-secondary rounded-pill ms-2">
                {{ tag.childrenCount }}
              </span>
            </div>

            <div class="btn-group btn-group-sm">
              <button
                  v-if="tag.hasChildren || tag.childrenCount > 0"
                  class="btn btn-outline-primary"
                  @click.stop="navigateToTag(tag.id)"
                  :title="'Открыть ' + (tag.childrenCount || '') + ' подкатегорий'"
              >
                <i class="bi bi-folder2-open"></i>
              </button>

              <button
                  class="btn btn-outline-success"
                  @click.stop="addChildTag(tag)"
                  title="Создать дочерний тег"
              >
                <i class="bi bi-plus"></i>
              </button>

              <button
                  class="btn btn-outline-secondary"
                  @click.stop="editTag(tag)"
                  title="Редактировать"
              >
                <i class="bi bi-pencil"></i>
              </button>

              <button
                  class="btn btn-outline-danger"
                  @click.stop="confirmDeleteTag(tag)"
                  :disabled="tag.childrenCount > 0"
                  :title="tag.childrenCount > 0 ? 'Удалите сначала подкатегории' : 'Удалить'"
              >
                <i class="bi bi-trash"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Товары текущего уровня -->
      <div class="products-section mt-4">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <h5 class="mb-0">Товары в этом разделе</h5>
          <button class="btn btn-sm btn-outline-secondary" :disabled="productsLoading" @click="loadProductsForCurrentLevel()" title="Обновить товары">
            <i class="bi bi-arrow-repeat"></i>
          </button>
        </div>
        <div v-if="productsLoading" class="text-muted">Загрузка товаров...</div>
        <div v-else-if="products.length === 0" class="text-muted">Товаров нет</div>
        <div v-else class="product-grid-admin">
          <div v-for="p in products" :key="p.id" class="product-card-admin" @click.stop.prevent="openPreview(p)">
            <div class="pc-header">
              <span class="pc-title" :title="p.name">{{ p.name }}</span>
              <div class="d-flex align-items-center gap-2">
                <span class="badge" :class="p.visible ? 'bg-success' : 'bg-secondary'">{{ p.visible ? 'Видим' : 'Скрыт' }}</span>
                <button class="btn btn-sm btn-outline-secondary" @click.stop="openEdit(p)" title="Редактировать">
                  <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-outline-primary pc-cart-btn" @click.stop="addToCartStub(p)" title="Добавить в корзину">
                  <i class="bi bi-cart-plus"></i>
                </button>
              </div>
            </div>
            <div class="pc-price">
              <template v-if="p.promoPrice && p.promoPrice < p.price">
                <span class="old">{{ formatPrice(p.price) }}</span>
                <span class="new">{{ formatPrice(p.promoPrice) }}</span>
                <span class="promo-badge">Промо</span>
              </template>
              <template v-else>
                <span class="new">{{ formatPrice(p.price) }}</span>
              </template>
            </div>
            <div class="pc-desc">
              <span>
                {{ isExpanded(p.id) ? (p.description || '—') : truncate(p.description || '—', 127) }}
              </span>
              <button v-if="(p.description || '').length > 127" class="btn-link-more" @click.stop="toggleExpanded(p.id)">
                {{ isExpanded(p.id) ? 'Скрыть' : '...' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Действия с тегами -->
      <div class="tag-actions mt-3">
        <button
            class="btn btn-sm btn-primary"
            @click="addChildTag()"
            :disabled="tagLoading"
        >
          + Создать корневой тег
        </button>
      </div>


    </div>

    <!-- Модальное окно подтверждения удаления -->
    <div v-if="showDeleteConfirm" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">Подтверждение удаления</h5>
          <button type="button" class="btn-close" @click="showDeleteConfirm = false"></button>
        </div>
        <div class="modal-body">
          <p>Вы уверены, что хотите удалить тег "{{ tagToDelete?.name }}"?</p>
          <p v-if="tagToDelete?.childrenCount > 0" class="text-warning">
            Внимание! Этот тег содержит дочерние теги. Все дочерние теги также будут удалены.
          </p>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="showDeleteConfirm = false">
            Отмена
          </button>
          <button
              type="button"
              class="btn btn-danger"
              @click="deleteTag"
              :disabled="deleting"
          >
            <span v-if="deleting" class="spinner-border spinner-border-sm me-1"></span>
            Удалить
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useToast } from 'vue-toastification';
import { useTagStore } from '@/store/tag';
import { useProductStore } from '@/store/product';
import { getBrands, createBrand } from '../services/brandService';
import CreateBrandModal from '../components/modals/CreateBrandModal.vue';
import CreateGroupModal from '../components/modals/CreateGroupModal.vue';
import EditGroupModal from '../components/modals/EditGroupModal.vue';
import CreateProductModal from '../components/modals/CreateProductModal.vue';
import ProductPreviewModal from '../components/modals/ProductPreviewModal.vue';
import EditProductModal from '../components/modals/EditProductModal.vue';

// Refs
const brands = ref([]);
const selectedBrand = ref(null);
const tags = ref([]);
const tagLoading = ref(false);
const loading = ref(false);
const error = ref(null);
const selectedTag = ref(null);
const tagToDelete = ref(null);
const showDeleteConfirm = ref(false);
const showEditGroupModal = ref(false);
const deleting = ref(false);
const currentParentId = ref(0);
const currentTagPath = ref([]);
const tagError = ref(null);
// Простой кэш для тегов (ключ: `${brandId}_${tagId}`)
const tagCache = new Map();

// UI state
const showCreateBrandModal = ref(false);
const showCreateGroupModal = ref(false);
const showCreateProductModal = ref(false);
const showProductPreview = ref(false);
const previewProduct = ref(null);
const showEditProductModal = ref(false);
const editProduct = ref(null);

// Store
const tagStore = useTagStore();
const productStore = useProductStore();
const toast = useToast();

// Helper function for Russian pluralization
const formatWord = (count, words) => {
  const cases = [2, 0, 1, 1, 1, 2];
  return words[
      count % 100 > 4 && count % 100 < 20
          ? 2
          : cases[Math.min(count % 10, 5)]
      ];
};

// Раскрытие длинного описания
const expandedProducts = ref(new Set());
const isExpanded = (id) => expandedProducts.value.has(id);
const toggleExpanded = (id) => {
  if (expandedProducts.value.has(id)) expandedProducts.value.delete(id);
  else expandedProducts.value.add(id);
};
const truncate = (text, len = 127) => {
  if (!text) return '';
  if (text.length <= len) return text;
  return text.slice(0, len) + '…';
};

// Опции групп для модалки (текущий уровень + корень)
const groupOptions = computed(() => {
  // Текущие теги (tags) — список на активном уровне. Отображаем их как кандидаты
  return (tags.value || []).map(t => ({ id: t.id, label: t.name }));
});

// ===== Темы: авто / светлая / тёмная =====
const THEME_KEY = 'admin_theme_mode'; // 'auto' | 'light' | 'dark'
const themeMode = ref('auto');
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
  themeMode.value = mode;
}

onMounted(() => {
  const saved = localStorage.getItem(THEME_KEY);
  if (saved === 'light' || saved === 'dark' || saved === 'auto') themeMode.value = saved;
  // слушаем смену системной темы, если режим авто
  if (media && media.addEventListener) {
    media.addEventListener('change', () => { if (themeMode.value === 'auto') applyTheme(); });
  }
  applyTheme();
});

watch(themeMode, (v) => {
  localStorage.setItem(THEME_KEY, v);
  applyTheme();
});

// Товары текущего уровня
const products = computed(() => productStore.products || []);
const productsLoading = computed(() => productStore.loading);

const loadProductsForCurrentLevel = async () => {
  if (!selectedBrand.value && selectedBrand.value !== 0) return;
  try {
    await productStore.fetchByBrandAndGroup(Number(selectedBrand.value), Number(currentParentId.value || 0), true);
  } catch (e) {
    console.error('Не удалось загрузить товары:', e);
    toast.error(e?.message || 'Не удалось загрузить товары');
  }
};

// Удаление (архивирование) товара из модалки
const onProductDelete = async () => {
  try {
    if (!editProduct.value?.id) return;
    const id = editProduct.value.id;
    await productStore.delete(id);
    toast.success('Товар перемещён в архив');
    showEditProductModal.value = false;
    editProduct.value = null;
    await loadProductsForCurrentLevel();
  } catch (e) {
    console.error('Ошибка при удалении товара:', e);
    toast.error(e?.message || 'Не удалось удалить товар');
  }
};

function openPreview(p) {
  previewProduct.value = p;
  showProductPreview.value = true;
}

async function openEdit(p) {
  try {
    const id = p?.id;
    if (!id) return;
    const full = await productStore.getById(id);
    editProduct.value = Array.isArray(full?.data) ? full.data : (full?.data ?? full);
    showEditProductModal.value = true;
  } catch (e) {
    console.error('Не удалось открыть редактирование товара:', e);
    toast.error(e?.message || 'Не удалось открыть редактирование');
  }
}

async function openEditFromPreview() {
  try {
    if (!previewProduct.value) return;
    const full = await productStore.getById(previewProduct.value.id);
    editProduct.value = Array.isArray(full?.data) ? full.data : (full?.data ?? full);
    showProductPreview.value = false;
    showEditProductModal.value = true;
  } catch (e) {
    console.error('Не удалось открыть редактирование товара:', e);
    toast.error(e?.message || 'Не удалось открыть редактирование');
  }
}

function addToCartStub(p) {
  toast.info('Корзина будет реализована позже');
}

function formatPrice(val) {
  if (val === null || val === undefined || val === '') return '';
  const num = Number(val);
  if (Number.isNaN(num)) return String(val);
  return num.toFixed(2);
}

// Выбор тега одной строкой списка
const selectTagRow = (tag) => {
  selectedTag.value = tag;
};

// Computed
const selectedBrandName = computed(() => {
  if (!selectedBrand.value) return '';
  const brand = brands.value.find(b => b.id === selectedBrand.value);
  return brand ? brand.name : '';
});

// Пробрасываем brandsError в шаблон, используя существующее состояние error
const brandsError = computed(() => error.value);

// Получение хлебных крошек для текущего пути тегов
const breadcrumbs = computed(() => {
  const result = [{ id: 0, name: 'Корень', isRoot: true }];
  if (currentTagPath.value && currentTagPath.value.length > 0) {
    return [...result, ...currentTagPath.value];
  }
  return result;
});

// Полный путь для отображения под списком брендов: /Brand/Tag1/Tag2/
const currentFullPath = computed(() => {
  const parts = [];
  if (selectedBrandName.value) parts.push(selectedBrandName.value);
  if (currentTagPath.value && currentTagPath.value.length > 0) {
    parts.push(...currentTagPath.value.map(t => t.name));
  }
  return parts.length ? `/${parts.join('/')}/` : '';
});

// Methods
const fetchBrands = async () => {
  console.log('Starting to fetch brands...');
  loading.value = true;
  error.value = null;
  try {
    const response = await getBrands();
    console.log('Brands API response:', response);

    // Handle different response formats
    if (Array.isArray(response)) {
      brands.value = response;
    } else if (response && Array.isArray(response.data)) {
      brands.value = response.data;
    } else if (response && response.data && Array.isArray(response.data.data)) {
      brands.value = response.data.data;
    } else {
      console.warn('Unexpected brands response format:', response);
      brands.value = [];
    }

    console.log('Processed brands:', brands.value);
    return brands.value;
  } catch (err) {
    const errorMsg = err.response?.data?.message || err.message || 'Неизвестная ошибка';
    console.error('Ошибка при загрузке брендов:', err);
    console.error('Детали ошибки:', {
      status: err.response?.status,
      statusText: err.response?.statusText,
      data: err.response?.data
    });
    error.value = `Не удалось загрузить бренды: ${errorMsg}`;
    toast.error(`Ошибка при загрузке брендов: ${errorMsg}`);
    throw err; // Пробрасываем ошибку дальше
  } finally {
    loading.value = false;
  }
};

const fetchTags = async (brandId, parentId = null) => {
  console.log('Fetching tags for:', { brandId, parentId });

  if (!brandId) {
    console.log('No brand ID provided, clearing tags');
    tags.value = [];
    return;
  }

  tagLoading.value = true;
  tagError.value = null;

  try {
    console.log(`Calling tagStore.fetchTagsByBrand(${brandId}, ${parentId || 0})`);

    // Load tags for the specified parent
    const response = await tagStore.fetchTagsByBrand(brandId, parentId || 0);
    console.log('Tags API response:', response);

    // Handle different response formats
    let loadedTags;
    if (Array.isArray(response)) {
      loadedTags = response;
    } else if (response && Array.isArray(response.data)) {
      loadedTags = response.data;
    } else if (response && response.data && Array.isArray(response.data.data)) {
      loadedTags = response.data.data;
    } else {
      console.warn('Unexpected tags response format:', response);
      loadedTags = [];
    }

    console.log('Processing tags:', loadedTags);

    // Process the loaded tags
    const processedTags = loadedTags.map(tag => {
      const hasChildren = (tag.childrenCount && tag.childrenCount > 0) ||
          (tag.children && tag.children.length > 0) ||
          false;

      return {
        id: tag.id || tag.tagId,
        name: tag.name || tag.tagName || 'Без названия',
        // ВАЖНО: не затираем фактический parentId текущим уровнем.
        // Берём parentId из самого тега, если есть; иначе используем контекстный уровень.
        parentId: (tag.parentId ?? (tag.parent?.id)) ?? (parentId ?? 0),
        brandId: tag.brandId || brandId,
        childrenCount: tag.childrenCount || (tag.children ? tag.children.length : 0),
        hasChildren: hasChildren,
        icon: hasChildren ? 'bi-folder' : 'bi-tag',
        ...tag // Spread remaining properties
      };
    });

    // Update the tags list
    tags.value = processedTags;
    console.log('Processed tags:', processedTags);

    // Cache the loaded tags
    processedTags.forEach(tag => {
      const cacheKey = `${brandId}_${tag.id}`;
      console.log('Caching tag:', cacheKey, tag);
      tagCache.set(cacheKey, tag);
    });

    // Update current parent ID
    currentParentId.value = parentId || 0;

    // Update breadcrumbs if we have a parent
    if (parentId && parentId !== 0) {
      console.log('Updating breadcrumb path for parentId:', parentId);
      await updateBreadcrumbPath(brandId, parentId);
    } else {
      console.log('Resetting to root level');
      currentTagPath.value = [];
    }

    console.log('Tags loaded successfully:', {
      brandId,
      parentId: parentId || 0,
      count: processedTags.length,
      tags: processedTags
    });

  } catch (err) {
    console.error('Error loading tags:', {
      error: err,
      response: err.response,
      status: err.response?.status,
      data: err.response?.data
    });

    const errorMessage = err.response?.data?.message || err.message || 'Не удалось загрузить теги. Пожалуйста, попробуйте снова.';
    tagError.value = errorMessage;
    toast.error(errorMessage);

    // Clear tags on error
    tags.value = [];
  } finally {
    tagLoading.value = false;
  }
};

// Update breadcrumb path based on current tag
const updateBreadcrumbPath = async (brandId, parentId) => {
  if (!parentId) {
    currentTagPath.value = [];
    return;
  }

  const path = [];
  let currentId = parentId;

  // Build the path by traversing up the hierarchy
  while (currentId) {
    const cacheKey = `${brandId}_${currentId}`;
    let tag = tagCache.get(cacheKey);

    // If tag not in cache, fetch it
    if (!tag) {
      try {
        const result = await tagStore.fetchTagsByBrand(brandId, 0, currentId);
        if (result && result.length > 0) {
          tag = result[0];
          tagCache.set(cacheKey, tag);
        } else {
          break; // Stop if we can't find the tag
        }
      } catch (err) {
        console.error('Ошибка при загрузке тега для навигации:', err);
        break;
      }
    }

    // Add to beginning of path
    path.unshift({
      id: tag.id,
      name: tag.name
    });

    // Move to parent
    currentId = tag.parentId || 0;
  }

  currentTagPath.value = path;
};

// Navigate to a specific tag
const navigateToTag = async (tagId = null) => {
  if (!selectedBrand.value) return;
  
  // If no tagId provided, go to root
  if (!tagId) {
    await fetchTags(selectedBrand.value, 0);
    await loadProductsForCurrentLevel();
    return;
  }
  
  // Otherwise, navigate to the specified tag
  selectedTag.value = null; // сбрасываем выделение при переходе
  await fetchTags(selectedBrand.value, tagId);
  await loadProductsForCurrentLevel();
};

// Переход по кликабельным крошкам под списком брендов
const navigateToBreadcrumb = async (item, index) => {
  try {
    if (!selectedBrand.value) return;
    tagError.value = null;
    selectedTag.value = null;
    const parentId = index === 0 ? 0 : (item?.id ?? 0);
    await fetchTags(selectedBrand.value, parentId);
    await loadProductsForCurrentLevel();
  } catch (e) {
    console.error('Ошибка navigateToBreadcrumb:', e);
    toast.error(e.message || 'Не удалось перейти по пути');
  }
};

// Combined onMounted hook
onMounted(async () => {
  console.log('Компонент AdminView смонтирован');
  loading.value = true;
  try {
    console.log('Загрузка брендов...');
    const loadedBrands = await fetchBrands();
    console.log('Бренды успешно загружены:', loadedBrands);

    // If we have brands, select the first one by default
    if (loadedBrands && loadedBrands.length > 0) {
      await selectBrand(loadedBrands[0].id);
    }
  } catch (error) {
    console.error('Ошибка при инициализации:', error);
    const errorMsg = error.response?.data?.message || error.message || 'Неизвестная ошибка';
    toast.error(`Не удалось загрузить данные: ${errorMsg}`);
  } finally {
    loading.value = false;
  }
});

const handleCreateBrand = async (formData) => {
  try {
    loading.value = true;
    console.log('Создание бренда с данными:', formData);

    // Ensure brandName is provided
    const brandName = (formData.brandName || '').trim();
    if (!brandName || brandName.length < 2) {
      throw new Error('Название бренда должно содержать минимум 2 символа');
    }

    // Create the brand
    const newBrand = await createBrand({ name: brandName });
    console.log('Бренд успешно создан:', newBrand);

    // Refresh the brands list
    await fetchBrands();

    // Close the modal
    showCreateBrandModal.value = false;

    toast.success(`Бренд "${brandName}" успешно создан!`);
    return newBrand;
  } catch (error) {
    console.error('Ошибка при создании бренда:', error);
    const errorMsg = error.response?.data?.message || error.message || 'Не удалось создать бренд';
    toast.error(`Ошибка: ${errorMsg}`);
    throw error; // Re-throw to keep the modal open
  } finally {
    loading.value = false;
  }
};

const handleCreateGroup = async (groupData) => {
  try {
    await createGroup(groupData);
    toast.success(`Группа "${groupData.groupName}" успешно создана!`);
    showCreateGroupModal.value = false;
    if (selectedBrand.value) {
      await onBrandSelect();
    }
  } catch (err) {
    console.error('Ошибка при создании группы:', err);
    toast.error('Не удалось создать группу.');
  }
};

const handleCreateProduct = async (productData) => {
  try {
    await createProduct(productData);
    toast.success(`Продукт "${productData.name}" успешно создан!`);
    showCreateProductModal.value = false;
  } catch (err) {
    console.error('Ошибка при создании продукта:', err);
    toast.error('Не удалось создать продукт.');
  }
};

// Сохранение из модалки редактирования
const onProductSave = async (payload) => {
  try {
    if (!editProduct.value?.id) return;
    const id = editProduct.value.id;

    // Смена бренда при необходимости
    if (payload.brandId && payload.brandId !== editProduct.value.brandId) {
      await productStore.changeBrand(id, Number(payload.brandId));
    }

    // Перемещение между группами при необходимости
    const newGroup = payload.groupTagId ?? 0;
    const oldGroup = editProduct.value.groupTagId ?? 0;
    if (newGroup !== oldGroup) {
      await productStore.move(id, Number(newGroup));
    }

    // Обновление полей товара
    await productStore.update(id, {
      name: payload.name,
      description: payload.description,
      price: payload.price,
      promoPrice: payload.promoPrice,
      visible: payload.visible ?? true,
    });

    toast.success('Товар обновлён');
    showEditProductModal.value = false;
    editProduct.value = null;
    await loadProductsForCurrentLevel();
  } catch (e) {
    console.error('Ошибка при сохранении товара:', e);
    toast.error(e?.message || 'Не удалось сохранить изменения');
  }
};

const selectBrand = async (brandId) => {
  const nextId = Number(brandId);
  const prevId = selectedBrand.value != null ? Number(selectedBrand.value) : null;
  console.log('Selecting brand:', { nextId, prevId });

  loading.value = true;
  tagLoading.value = true;
  tagError.value = null;

  try {
    // Очистка прошлого состояния
    tags.value = [];
    currentTagPath.value = [];
    currentParentId.value = 0;

    // Устанавливаем выбранный бренд
    selectedBrand.value = nextId;

    if (nextId) {
      console.log('Loading tags for brand:', nextId);
      await fetchTags(nextId, 0); // корневой уровень
      updateBreadcrumbPath(nextId, 0);
      await loadProductsForCurrentLevel();
    } else {
      console.log('No brand selected, clearing tags');
      tags.value = [];
    }
  } catch (error) {
    console.error('Ошибка при выборе бренда:', error);
    const errorMsg = error.response?.data?.message || error.message || 'Не удалось загрузить теги';
    tagError.value = errorMsg;
    toast.error(`Ошибка: ${errorMsg}`);
    throw error;
  } finally {
    loading.value = false;
    tagLoading.value = false;
  }
};

// Tag management methods
const addChildTag = async (parentTag) => {
  selectedTag.value = parentTag || null;
  showCreateGroupModal.value = true;

  // If we're adding a child to a specific tag, navigate to that tag
  if (parentTag) {
    await navigateToTag(parentTag.id);
  }
};

const editTag = (tag) => {
  selectedTag.value = tag;
  showEditGroupModal.value = true;
};

const confirmDeleteTag = (tag) => {
  tagToDelete.value = tag;
  showDeleteConfirm.value = true;
};

const deleteTag = async () => {
  if (!tagToDelete.value) return;

  deleting.value = true;

  try {
    await tagStore.deleteTag(tagToDelete.value.id);
    toast.success('Тег успешно удален');
    if (selectedBrand.value) {
      await fetchTags(selectedBrand.value);
    }
  } catch (error) {
    console.error('Ошибка при удалении тега:', error);
    toast.error('Не удалось удалить тег');
  } finally {
    deleting.value = false;
    showDeleteConfirm.value = false;
    tagToDelete.value = null;
  }
};

// Отладочный лог при изменении брендов
watch(brands, (newVal) => {
  console.log('Бренды обновлены:', newVal);
}, { deep: true });

// Управляем загрузкой тегов исключительно через selectBrand(),
// чтобы избежать дублирующих запросов и гонок состояний.
watch(selectedBrand, (newBrandId, oldBrandId) => {
  console.log('selectedBrand changed:', { from: oldBrandId, to: newBrandId });
});

const handleBrandCreated = async (formData) => {
  try {
    loading.value = true;
    const brandName = (formData.brandName || '').trim();

    if (!brandName || brandName.length < 2) {
      throw new Error('Название бренда должно содержать минимум 2 символа');
    }

    console.log('Creating brand with name:', brandName);
    const response = await createBrand(brandName);

    // Обновляем список брендов
    await fetchBrands();

    // Закрываем модальное окно
    showCreateBrandModal.value = false;

    toast.success('Бренд успешно создан');
    return response;
  } catch (error) {
    console.error('Ошибка при создании бренда:', error);
    const errorMsg = error.response?.data?.message || error.message || 'Не удалось создать бренд';
    toast.error(`Ошибка: ${errorMsg}`);
    throw error; // Пробрасываем ошибку, чтобы модальное окно не закрывалось при ошибке
  } finally {
    loading.value = false;
  }
};

const handleTagCreated = async (newTag) => {
  if (selectedBrand.value) {
    // Close the modal
    showCreateGroupModal.value = false;

    // If the new tag has a parent, navigate to the parent to show the new tag
    if (newTag && newTag.parentId) {
      await navigateToTag(newTag.parentId);
    } else {
      // Otherwise, refresh the current tag list
      await fetchTags(selectedBrand.value, currentParentId.value);
    }

    toast.success('Тег успешно создан');
  }
};

const handleProductCreated = () => {
  toast.success('Товар успешно создан');
  loadProductsForCurrentLevel();
};

const onBrandSelect = async () => {
  selectedGroup.value = null;
  tagGroups.value = [];
  if (!selectedBrand.value) return;

  tagGroupsLoading.value = true;
  tagGroupsError.value = null;
  try {
    const response = await getTagGroupsByBrandId(selectedBrand.value);
    tagGroups.value = response.data;
    if (response.data.length === 0) {
      // Можно не считать это ошибкой, а просто показать сообщение
    }
  } catch (err) {
    console.error(`Ошибка при загрузке групп тегов для бренда ${selectedBrand.value}:`, err);
    tagGroupsError.value = 'Не удалось загрузить группы тегов.';
    // toast.error(tagGroupsError.value); // Уведомление может быть слишком навязчивым
  } finally {
    tagGroupsLoading.value = false;
  }
};
</script>

<style scoped>
.admin-panel {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

/* Заголовки в тёмной зоне — цвет кнопок */
.admin-panel h1,
.admin-panel h2 {
  color: #4a6cf7; /* основной цвет кнопок */
}

/* Но внутри белой карточки для тегов заголовок должен быть чёрным */
.tag-manager-container h2 {
  color: var(--text);
}

/* Themed container for tags/products block */
.tag-manager-container {
  background: var(--card);
  color: var(--text);
  border: 1px solid var(--card-border);
  border-radius: 14px;
  box-shadow: 0 10px 24px var(--shadow-color);
  padding: 16px;
}

/* Force theme for Bootstrap list styles inside the container */
.tag-manager-container :deep(.list-group),
.tag-manager-container :deep(.list-group-item),
.tag-manager-container :deep(.list-group-item-action) {
  background: var(--card) !important;
  color: var(--text) !important;
  border-color: var(--border) !important;
}
.tag-manager-container :deep(.tag-row.active) {
  background: var(--input-bg-hover) !important;
}
.tag-manager-container :deep(.badge.bg-secondary) { background: var(--input-bg); color: var(--text); }
.tag-manager-container :deep(.btn.btn-outline-secondary) { color: var(--text); border-color: var(--border); }
.tag-manager-container :deep(.btn.btn-outline-primary) { color: var(--primary); border-color: var(--primary); }
.tag-manager-container :deep(.btn.btn-outline-danger) { color: var(--danger); border-color: var(--danger); }

/* Theme alerts inside */
.tag-manager-container :deep(.alert),
.tag-manager-container :deep(.alert-info),
.tag-manager-container :deep(.alert-danger) {
  background: var(--input-bg) !important;
  color: var(--text) !important;
  border-color: var(--border) !important;
}

.brand-selector-container {
  margin-bottom: 20px;
}

.brand-selector-title {
  text-align: left;
  margin-bottom: 10px;
  font-size: 1.2rem;
  color: var(--text);
}

.brand-list-scroll {
  display: flex;
  flex-wrap: nowrap;
  overflow-x: auto;
  padding: 10px 0;
  gap: 8px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: #c1c1c1 #f1f1f1;
}

.brand-list-scroll::-webkit-scrollbar {
  height: 8px;
}

.brand-list-scroll::-webkit-scrollbar-track {
  background: var(--card);
  border-radius: 4px;
}

.brand-list-scroll::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 4px;
}

.brand-chip {
  flex: 0 0 auto;
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background-color: color-mix(in srgb, var(--success) 45%, var(--card));
  color: var(--text);
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  font-size: 14px;
}

/* Blue for OWNER role */
.brand-chip.owner {
  background-color: color-mix(in srgb, var(--primary) 60%, var(--card));
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 500;
  box-shadow: 0 2px 4px var(--shadow-color);
  transition: all 0.2s ease;
}

.brand-chip.owner:hover {
  background-color: color-mix(in srgb, var(--primary) 75%, var(--card));
  transform: translateY(-1px);
  box-shadow: 0 4px 8px var(--shadow-color);
}

.role-badge {
  background-color: var(--input-bg);
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Override list items (Bootstrap-like) to theme */
.tag-manager-container .list-group-item {
  background: var(--card);
  color: var(--text);
  border-color: var(--border);
}

/* Product cards in admin */
.product-card-admin {
  background: var(--card);
  color: var(--text);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  box-shadow: 0 6px 16px var(--shadow-color);
}
.pc-price .old { color: var(--muted); }
.pc-desc { color: var(--text); }

.brand-chip:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.brand-chip.active {
  border-color: #1a73e8;
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.2);
}

/* Tag list styles */
.tag-list {
  margin-top: 15px;
  max-height: 500px;
  overflow-y: auto;
  padding-right: 10px;
}

.tag-actions {
  display: flex;
  gap: 8px;
}

.tag-manager-container {
  margin-top: 20px;
  padding: 15px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background-color: #f8fafc;
}

/* Текущий путь под списком брендов */
.current-path { color: #94a3b8; font-size: 14px; }

/* Кнопки-крошки под брендами */
.path-chip {
  background: transparent;
  border: 0;
  color: #4a6cf7; /* цвет кнопок */
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 8px;
}
.path-chip:hover { text-decoration: underline; }
.path-chip.active {
  background: #e0e7ff;
  color: #1e40af; /* более тёмный синий для активной */
}
.path-sep { color: #64748b; padding: 0 2px; }

/* Контент списка тегов — белый фон, чёрный текст */
.tag-manager-content { background: #ffffff; color: #111827; border-radius: 8px; }

/* Список тегов и строки */
.list-group { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; }
.list-group-item.tag-row {
  background: #ffffff;
  color: #111827; /* основной текст — чёрный */
  border: 0;
  border-bottom: 1px solid #e5e7eb;
  padding: 10px 14px;
}
.list-group-item.tag-row:last-child { border-bottom: 0; }
.tag-row:hover { background: #f8fafc; }
.tag-row.active { background: #eef2ff; border-left: 4px solid #4a6cf7; }
.tag-title { color: #111827; font-weight: 600; }
.tag-row i { color: #475569; }

/* Кнопки внутри строки */
.tag-row .btn-outline-primary { border-color: #4a6cf7; color: #4a6cf7; }
.tag-row .btn-outline-primary:hover { background: #e0e7ff; }
.tag-row .btn-outline-success { border-color: #22c55e; color: #16a34a; }
.tag-row .btn-outline-success:hover { background: #dcfce7; }
.tag-row .btn-outline-secondary { border-color: #94a3b8; color: #475569; }
.tag-row .btn-outline-danger { border-color: #ef4444; color: #dc2626; }

.tag-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  background-color: white;
  border: 1px solid #e2e8f0;
  border-left: 4px solid #4a6cf7;
  border-radius: 6px;
  transition: all 0.2s ease;
  cursor: pointer;
  color: #1e293b;
}

.tag-item.has-children {
  border-left-color: #4CAF50;
  background-color: #f8fafc;
}

.tag-item.has-children:hover {
  background-color: #f0f7ff;
}

.tag-item .tag-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.tag-item .tag-name i {
  color: #64748b;
  font-size: 1.1em;
}

.tag-item.has-children .tag-name i {
  color: #4CAF50;
}

.tag-item:hover {
  background-color: #f8fafc;
  border-color: #cbd5e1;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.tag-item .tag-name {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1e293b;
  font-weight: 500;
}

.tag-item .tag-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tag-item:hover .tag-actions {
  opacity: 1;
}

.brand-sel.error-message {
  color: #ef4444;
  margin-top: 0.5rem;
  font-size: 0.9rem;
  font-weight: 500;
  background-color: #fef2f2;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border-left: 3px solid #ef4444;
}

.alert-danger {
  background-color: #fef2f2;
  color: #b91c1c;
  padding: 1rem 1.25rem;
  border-radius: 0.5rem;
  margin: 1rem 0;
  border-left: 4px solid #dc2626;
  font-size: 0.95rem;
  line-height: 1.5;
}

.alert-danger strong {
  font-weight: 600;
}

a.loading {
  text-align: center;
  padding: 2rem;
  color: var(--text-secondary);
  background: white;
  border-radius: 0.75rem;
  margin: 1rem 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.loading .spinner-border {
  display: inline-block;
  width: 2.5rem;
  height: 2.5rem;
  vertical-align: middle;
  border: 0.25em solid rgba(74, 108, 247, 0.2);
  border-right-color: var(primary-color);
  border-radius: 50%;
  animation: 0.75s linear infinite spinner-border;
  margin-bottom: 1rem;
}

.loading p {
  margin: 0.75rem 0 0 0;
  font-size: 1.1rem;
  font-weight: 500;
  color: var(--text-primary);
}

@keyframes spinner-border {
  to {
    transform: rotate(360deg);
  }
}

.admin-actions-container {
  margin: 20px 0;
  display: flex;
  gap: 10px;
}

.admin-action-btn {
  padding: 8px 16px;
  background-color: #4a6cf7;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.admin-action-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.admin-action-btn.secondary {
  background-color: #6c757d;
}

.admin-action-btn:hover:not(:disabled) {
  background-color: #3a5bd9;
}

.admin-action-btn.secondary:hover:not(:disabled) {
  background-color: #5a6268;
}

.admin-action-btn.secondary {
  background-color: #64748b;
}

.admin-action-btn.secondary:hover:not(:disabled) {
  background-color: #475569;
}

/* Товары: читаемый тёмный текст на белом фоне */
.products-section h5 { color: #111827; font-weight: 700; }
.product-grid-admin {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.product-card-admin {
  background: #ffffff;
  color: #111827;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
.product-card-admin .pc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.product-card-admin .pc-title { color: #111827; font-weight: 700; font-size: 14px; line-height: 1.2; }
.product-card-admin .pc-price { margin-top: 4px; display: flex; align-items: center; gap: 8px; }
.product-card-admin .pc-price .old { color: #6b7280; text-decoration: line-through; }
.product-card-admin .pc-price .new { color: #111827; font-weight: 800; }
.product-card-admin .promo-badge { background: #fde68a; color: #92400e; font-size: 11px; padding: 2px 6px; border-radius: 8px; }
.product-card-admin .pc-desc { margin-top: 8px; color: #1f2937; font-size: 13px; display: flex; align-items: center; gap: 6px; }
.product-card-admin .btn-link-more { background: transparent; border: 0; color: #4a6cf7; font-weight: 700; cursor: pointer; padding: 0; }

.btn-edit {
  padding: 4px 10px;
  background-color: #4a6cf7;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.btn-delete {
  padding: 4px 10px;
  background-color: #ef4444;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.admin-action-btn:hover:not(:disabled) {
  background-color: #3a5bd9;
}

.admin-action-btn.secondary:hover:not(:disabled) {
  background-color: #5a6268;
}
</style>
