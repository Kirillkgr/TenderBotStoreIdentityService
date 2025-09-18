<template>
  <div class="home-container">
    <!-- Бренды (чипы) -->
    <div class="brand-chips">
      <button
        v-for="brand in brands"
        :key="brand.id"
        class="brand-chip"
        :class="{ active: selectedBrandId === brand.id, owner: brand.role === 'OWNER' || brand.role === 'ROLE_OWNER' }"
        @click="selectBrand(brand.id)"
      >
        {{ brand.name }}
      </button>
    </div>

    <!-- Список групп-тегов выбранного бренда: карточки 1-го уровня -->
    <div v-if="brandLoading" class="info-text">Загрузка брендов...</div>
    <div v-else-if="brands.length === 0" class="info-text">Бренды не найдены</div>

    <!-- Крошки пути по группам бренда -->
    <div v-if="selectedBrandId" class="group-breadcrumbs">
      <button class="crumb" :class="{ active: currentParentId === 0 }" @click="navigateToCrumb(0, -1)">Корень</button>
      <template v-for="(node, idx) in currentPath" :key="'crumb-'+node.id">
        <span class="sep">/</span>
        <button class="crumb" :class="{ active: idx === currentPath.length - 1 }" @click="navigateToCrumb(node.id, idx)">{{ node.name }}</button>
      </template>
    </div>

    <div v-if="tagLoading" class="info-text">Загрузка групп...</div>
    <div v-else class="group-grid">
      <div 
        v-for="group in groups" 
        :key="group.id" 
        class="group-card" 
        @click="openGroup(group)"
        :title="group.name"
      >
        <div class="group-image">
          <img v-if="group.imageUrl" :src="group.imageUrl" :alt="group.name" />
          <div v-else class="image-placeholder">{{ group.name?.charAt(0)?.toUpperCase() }}</div>
        </div>
        <div class="group-title">{{ group.name }}</div>
      </div>
      <div v-if="!tagLoading && selectedBrandId && groups.length === 0" class="info-text">
        В этом бренде пока нет групп
      </div>
    </div>

    <!-- Сетка товаров (как было) -->
    <div v-if="productStore.loading" class="info-text">Загрузка товаров...</div>
    <div v-else-if="productStore.products.length === 0" class="info-text">Товары не найдены.</div>
    <div v-else class="product-grid">
      <ProductCard
        v-for="product in productStore.products"
        :key="product.id"
        :product="product"
        :openInModal="true"
        @preview="openProductPreview"
      />
    </div>

    <ProductPreviewModal
      v-if="showProductPreview"
      :product="previewProduct"
      width="640px"
      height="560px"
      @close="showProductPreview = false"
    />
  </div>
  
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useProductStore } from '../store/product';
import ProductCard from '../components/ProductCard.vue';
import ProductPreviewModal from '../components/modals/ProductPreviewModal.vue';
import { getPublicBrands } from '../services/brandService';
import tagService from '../services/tagService';
import { useTagStore } from '@/store/tag';

const productStore = useProductStore();
const tagStore = useTagStore();

// Бренды
const brands = ref([]);
const brandLoading = ref(false);
const selectedBrandId = ref(null);

// Группы (текущий уровень выбранного бренда)
const groups = ref([]);
const tagLoading = ref(false);
const currentParentId = ref(0);
const currentPath = ref([]); // массив объектов {id, name}

// Предпросмотр товара (модалка)
const showProductPreview = ref(false);
const previewProduct = ref(null);
function openProductPreview(p) {
  previewProduct.value = p;
  showProductPreview.value = true;
}

// Товары как было
onMounted(async () => {
  await loadBrands();
  // Если есть хотя бы один бренд — выберем первый, чтобы показать контент
  if (brands.value.length > 0) {
    await selectBrand(brands.value[0].id);
  }
});

const loadBrands = async () => {
  brandLoading.value = true;
  try {
    const response = await getPublicBrands();
    if (Array.isArray(response)) {
      brands.value = response;
    } else if (response && Array.isArray(response.data)) {
      brands.value = response.data;
    } else if (response && response.data && Array.isArray(response.data.data)) {
      brands.value = response.data.data;
    } else {
      brands.value = [];
    }
  } catch (e) {
    console.error('Не удалось загрузить бренды:', e);
    brands.value = [];
  } finally {
    brandLoading.value = false;
  }
};

const selectBrand = async (brandId) => {
  selectedBrandId.value = Number(brandId);
  // Сбрасываем путь и загружаем корень
  currentParentId.value = 0;
  currentPath.value = [];
  await loadGroups(0);
  // Параллельно подтянем товары корневого уровня выбранного бренда
  try {
    await productStore.fetchPublicByBrandAndGroup(selectedBrandId.value, 0, true);
  } catch (e) {
    console.error('Не удалось загрузить товары бренда (корень):', e);
  }
};

const loadGroups = async (parentId = 0) => {
  groups.value = [];
  if (!selectedBrandId.value && selectedBrandId.value !== 0) return;
  tagLoading.value = true;
  try {
    // Загружаем группы и товары текущего уровня (параллельно)
    const [res] = await Promise.all([
      tagService.getPublicTagsByBrand(selectedBrandId.value, parentId),
      productStore.fetchPublicByBrandAndGroup(selectedBrandId.value, parentId || 0, true)
    ]);
    const arr = Array.isArray(res) ? res : (res?.data ?? res?.data?.data ?? []);
    groups.value = (arr || []).map(t => ({
      id: t.id ?? t.tagId,
      name: t.name ?? t.tagName ?? 'Без названия',
      imageUrl: t.imageUrl || t.iconUrl || null,
      childrenCount: t.childrenCount || (t.children ? t.children.length : 0),
      hasChildren: (t.childrenCount && t.childrenCount > 0) || (t.children && t.children.length > 0)
    }));
    currentParentId.value = parentId || 0;
  } catch (e) {
    console.error('Не удалось загрузить группы бренда:', e);
  } finally {
    tagLoading.value = false;
  }
};

const openGroup = async (group) => {
  // Переходим внутрь выбранной группы
  // Строим путь — если уже есть этот id в пути, обрезаем до него
  const idx = currentPath.value.findIndex(n => n.id === group.id);
  if (idx >= 0) {
    currentPath.value = currentPath.value.slice(0, idx + 1);
  } else {
    currentPath.value.push({ id: group.id, name: group.name });
  }
  await loadGroups(group.id);
};

const navigateToCrumb = async (parentId, index) => {
  if (parentId === 0) {
    currentPath.value = [];
  } else if (index >= 0) {
    currentPath.value = currentPath.value.slice(0, index + 1);
  }
  await loadGroups(parentId || 0);
};

</script>

<style scoped>
.home-container {
  padding: 2rem;
}

/* Чипы брендов */
.brand-chips {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 1.25rem;
}
.brand-chip {
  padding: 8px 14px;
  border-radius: var(--brand-button-radius, 20px);
  border: 1px solid var(--border);
  background: var(--input-bg);
  color: var(--text);
  cursor: pointer;
  transition: all .18s ease;
  backdrop-filter: blur(6px);
}
.brand-chip:hover { background: var(--input-bg-hover); }
.brand-chip.owner {
  background: var(--brand-accent, var(--primary));
  color: var(--brand-accent-contrast, #fff);
}
.brand-chip.active { box-shadow: 0 0 0 2px color-mix(in srgb, var(--brand-accent, var(--primary)) 35%, transparent); }

/* Крошки пути по группам */
.group-breadcrumbs {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--muted);
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.group-breadcrumbs .sep { color: var(--muted); opacity: .7; }
.crumb {
  background: transparent;
  color: var(--text);
  border: 1px solid var(--border);
  padding: 6px 10px;
  border-radius: 10px;
  font-weight: 600;
  transition: all .18s ease;
}
.crumb:hover { background: var(--input-bg-hover); }
.crumb.active { background: color-mix(in srgb, var(--brand-accent, var(--primary)) 18%, transparent); color: var(--text); border-color: var(--brand-accent, var(--primary)); }

/* Группы (теги) квадратными карточками */
.group-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 18px;
  margin-bottom: 2rem;
}
.group-card {
  background: var(--card);
  border: 1px solid var(--card-border);
  border-radius: 16px;
  box-shadow: 0 6px 16px var(--shadow-color);
  overflow: hidden;
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease;
}
.group-card:hover { transform: translateY(-3px); box-shadow: 0 10px 22px var(--shadow-color); border-color: var(--border); }
.group-image { aspect-ratio: 1 / 1; background: var(--surface); display: flex; align-items: center; justify-content: center; }
.group-image img { width: 100%; height: 100%; object-fit: cover; }
.image-placeholder { font-size: 28px; font-weight: 800; color: var(--muted); letter-spacing: .5px; }
.group-title { text-align: center; padding: 12px 10px; color: var(--text); font-weight: 700; }

/* Товары */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1.5rem;
}

.info-text { color: var(--muted); margin: 10px 0 18px; text-align: center; }

@media (max-width: 480px) {
  .home-container { padding: 1rem; }
}
</style>
