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
        <button class="crumb" :class="{ active: idx === currentPath.length - 1 }"
                @click="navigateToCrumb(node.id, idx)">{{ node.name }}
        </button>
      </template>
    </div>

    <div v-if="tagLoading" class="info-text">Загрузка групп...</div>
    <div v-else class="group-grid">
      <GroupCard
          v-for="group in groups"
          :key="group.id"
          :group="group"
          @open="openGroup"
      />
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
        v-for="(item, i) in openPreviews"
        :key="'pp-'+i+'-'+item.product?.id"
        :product="item.product"
        width="720px"
        height="540px"
        :close-on-backdrop="false"
        :close-on-esc="false"
        :z-index="item.z"
        :offset-x="item.offsetX || 0"
        :offset-y="item.offsetY || 0"
        @focus="focusPreview(i)"
        @close="closePreview(i)"
    />

  </div>

</template>

<script setup>
import {useAuthStore} from '@/store/auth';
import {useTagStore} from '@/store/tag';
import {getBrandHint, toSlug} from '@/utils/brandHint';
import {onMounted, onUnmounted, ref} from 'vue';
import ProductCard from '../components/ProductCard.vue';
import GroupCard from '../components/cards/GroupCard.vue';
import ProductPreviewModal from '../components/modals/ProductPreviewModal.vue';
import {getPublicBrands, getPublicBrandsMin} from '../services/brandService';
import tagService from '../services/tagService';
import {useProductStore} from '../store/product';

const productStore = useProductStore();
const tagStore = useTagStore();
const authStore = useAuthStore();

// Бренды
const brands = ref([]);
const brandLoading = ref(false);
const selectedBrandId = ref(null);

// Группы (текущий уровень выбранного бренда)
const groups = ref([]);
const tagLoading = ref(false);
const currentParentId = ref(0);
const currentPath = ref([]); // массив объектов {id, name}

// Стек предпросмотров товаров (несколько модалок одновременно)
const openPreviews = ref([]);
const zCounter = ref(10000);

function openProductPreview(p) {
  // Если модалка для этого товара уже открыта — просто поднимем её наверх
  const idx = openPreviews.value.findIndex(item => item.product?.id === p?.id);
  if (idx !== -1) {
    focusPreview(idx);
    return;
  }
  zCounter.value += 1;
  const count = openPreviews.value.length;
  const offsetStep = 12;
  openPreviews.value.push({product: p, z: zCounter.value, offsetX: count * offsetStep, offsetY: count * offsetStep});
}

function closePreview(index) {
  openPreviews.value.splice(index, 1);
}

function focusPreview(index) {
  if (index < 0 || index >= openPreviews.value.length) return;
  zCounter.value += 1;
  openPreviews.value[index].z = zCounter.value;
}

// Закрываем верхнюю модалку по ESC (первая открытая — последняя закрывается)
function onKey(e) {
  if (e.key !== 'Escape') return;
  if (!openPreviews.value.length) return;
  e.preventDefault();
  // Находим элемент с максимальным z
  let maxIdx = 0;
  let maxZ = -Infinity;
  openPreviews.value.forEach((it, i) => {
    if (it.z > maxZ) {
      maxZ = it.z;
      maxIdx = i;
    }
  });
  closePreview(maxIdx);
}

onMounted(() => window.addEventListener('keydown', onKey));
onUnmounted(() => window.removeEventListener('keydown', onKey));

// Товары как было
onMounted(async () => {
  // Быстрая проверка сабдомена: если зашли на субдомен, сначала проверим, что он существует в системе
  try {
    const host = window.location.hostname.toLowerCase();
    const envRoot = (import.meta?.env?.VITE_MAIN_DOMAIN || '').toString().trim().toLowerCase();
    const root = envRoot || 'tbspro.ru';
    const isLocalSubdomain = host !== 'localhost' && host.includes('.localhost');
    const isProdSubdomain = host.endsWith('.' + root) && host !== root;
    if (isLocalSubdomain || isProdSubdomain) {
      const hint = toSlug(getBrandHint() || '');
      if (hint) {
        try {
          const res = await getPublicBrandsMin();
          const list = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : []);
          const found = list.find(x => toSlug(x.domain || '') === hint);
          if (found && found.id) {
            // Быстро выбрать бренд, далее догрузим бренды/данные обычным путём
            await selectBrand(Number(found.id));
          } else {
            // Сабдомен неизвестен — редирект на корневой сайт
            const protocol = window.location.protocol || 'http:';
            const port = window.location.port ? `:${window.location.port}` : '';
            const rootUrl = isLocalSubdomain ? `${protocol}//localhost${port}/` : `${protocol}//${root}/`;
            window.location.replace(rootUrl);
            return;
          }
        } catch (e) {
          // В случае ошибки проверки продолжаем обычную загрузку
        }
      }
    }
  } catch (_) {
  }

  await loadBrands();

  // 1) Приоритет: параметр ?brand=ID
  try {
    const params = new URLSearchParams(window.location.search);
    const qBrand = params.get('brand');
    const exists = qBrand && (brands.value || []).some(b => Number(b.id) === Number(qBrand));
    if (exists) {
      await selectBrand(Number(qBrand));
      return;
    }
  } catch (_) {
  }

  // 2) Затем localStorage: ранее выбранный бренд
  try {
    const storedId = localStorage.getItem('current_brand_id');
    const existsStored = storedId && (brands.value || []).some(b => Number(b.id) === Number(storedId));
    if (existsStored) {
      await selectBrand(Number(storedId));
      return;
    }
  } catch (_) {
  }

  // 3) Затем подсказка из субдомена: сравниваем по имени без учета регистра
  try {
    const hint = (getBrandHint() || '').toLowerCase();
    if (hint && Array.isArray(brands.value) && brands.value.length > 0) {
      const normalize = (s) => String(s || '').toLowerCase().trim();
      const byName = brands.value.find(b => normalize(b.name) === hint);
      // также пробуем поля slug/code/subdomain/domain
      const bySlug = brands.value.find(b => normalize(b.slug || b.code || b.subdomain || b.domain) === hint);
      if (byName) {
        await selectBrand(byName.id);
        return;
      }
      if (bySlug) {
        await selectBrand(bySlug.id);
        return;
      }
      // fallback: частичное совпадение (напр., brand-2 vs brand2)
      const compact = (s) => normalize(s).replace(/[^a-z0-9а-яё]+/gi, '');
      let relaxed = brands.value.find(b => compact(b.name) === compact(hint));
      if (!relaxed) relaxed = brands.value.find(b => compact(b.slug || b.code || b.subdomain || b.domain) === compact(hint));
      if (!relaxed) relaxed = brands.value.find(b => normalize(b.name).includes(hint));
      if (relaxed) {
        await selectBrand(relaxed.id);
        return;
      }
    }
  } catch (_) {
  }

  // 4) Финальный запасной вариант — первый бренд, если есть
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
  const nextId = Number(brandId);
  const currentId = selectedBrandId.value == null ? null : Number(selectedBrandId.value);
  // Ранее для неадминов открывали новый бренд в новой вкладке.
  // Теперь всегда переключаем бренд in-place без открытия новой вкладки.
  // Если пользователь уже находится на странице субдомена — делаем переход на субдомен выбранного бренда
  try {
    const host = window.location.hostname.toLowerCase();
    const protocol = window.location.protocol || 'http:';
    const port = window.location.port ? `:${window.location.port}` : '';
    const envRoot = (import.meta?.env?.VITE_MAIN_DOMAIN || '').toString().trim().toLowerCase();
    const root = envRoot || 'tbspro.ru';

    const isLocalSubdomain = host !== 'localhost' && host.includes('.localhost');
    const isProdSubdomain = host.endsWith('.' + root) && host !== root;
    if (isLocalSubdomain || isProdSubdomain) {
      const b = (brands.value || []).find(x => Number(x.id) === nextId);
      if (b) {
        const targetLabel = toSlug(b.domain || b.slug || b.code || b.subdomain || b.name || '');
        const currentLabel = toSlug(getBrandHint() || '');
        // Если уже на нужном сабдомене — не перенаправлять (избежать зацикливания)
        if (targetLabel && targetLabel !== currentLabel) {
          const targetHost = isLocalSubdomain ? `${targetLabel}.localhost${port}` : `${targetLabel}.${root}`;
          const url = `${protocol}//${targetHost}/`;
          window.location.href = url;
          return;
        }
      }
    }
  } catch (_) {
  }

  selectedBrandId.value = nextId;
  try {
    localStorage.setItem('current_brand_id', String(nextId));
  } catch (_) {
  }
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
    currentPath.value.push({id: group.id, name: group.name});
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
  max-width: 1280px; /* контентная ширина как у популярных магазинов */
  margin: 0 auto; /* центрируем весь контент */
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

.brand-chip:hover {
  background: var(--input-bg-hover);
}

.brand-chip.owner {
  background: var(--brand-accent, var(--primary));
  color: var(--brand-accent-contrast, #fff);
}

.brand-chip.active {
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--brand-accent, var(--primary)) 35%, transparent);
}

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

.group-breadcrumbs .sep {
  color: var(--muted);
  opacity: .7;
}

.crumb {
  background: transparent;
  color: var(--text);
  border: 1px solid var(--border);
  padding: 6px 10px;
  border-radius: 10px;
  font-weight: 600;
  transition: all .18s ease;
}

.crumb:hover {
  background: var(--input-bg-hover);
}

.crumb.active {
  background: color-mix(in srgb, var(--brand-accent, var(--primary)) 18%, transparent);
  color: var(--text);
  border-color: var(--brand-accent, var(--primary));
}

/* Группы (теги) квадратными карточками */
.group-grid {
  display: grid;
  /* Центрированные ряды с фиксированной шириной карточек */
  grid-template-columns: repeat(auto-fit, minmax(170px, 200px));
  justify-content: center; /* центрируем последнюю строку */
  gap: 18px;
  margin-bottom: 2rem;
}

.group-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Товары */
.product-grid {
  display: grid;
  /* Чётные столбцы карточек 260–280px, центрирование как у витрин */
  grid-template-columns: repeat(auto-fit, minmax(260px, 280px));
  justify-content: center; /* центрируем последнюю строку */
  gap: 24px;
}

.info-text {
  color: var(--muted);
  margin: 10px 0 18px;
  text-align: center;
}

@media (max-width: 480px) {
  .home-container {
    padding: 1rem;
  }
}
</style>
