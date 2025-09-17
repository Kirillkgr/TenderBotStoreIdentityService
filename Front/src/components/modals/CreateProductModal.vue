<template>
  <Modal :is-modal-visible="true" @close="$emit('close')">
    <template #header>
      <h3>Создать новый товар</h3>
    </template>
    <template #content>
      <Form @submit="onSubmit" :validation-schema="schema" class="form-body">
        <!-- Name -->
        <div class="form-group">
          <label for="productName">Название *</label>
          <Field name="name" type="text" id="productName" class="form-control" placeholder="Введите название"/>
          <ErrorMessage name="name" class="error-message"/>
        </div>

        <!-- Description -->
        <div class="form-group">
          <label for="productDescription">Описание</label>
          <Field name="description" as="textarea" id="productDescription" class="form-control" placeholder="Добавьте описание товара"/>
          <ErrorMessage name="description" class="error-message"/>
        </div>

        <!-- Price and Promotional Price -->
        <div class="form-row">
          <div class="form-group">
            <label for="productPrice">Цена *</label>
            <Field name="price" type="number" id="productPrice" class="form-control" placeholder="99.99" step="0.01" min="0" inputmode="decimal"/>
            <ErrorMessage name="price" class="error-message"/>
          </div>
          <div class="form-group">
            <label for="promoPrice">Акционная цена</label>
            <Field name="promotionalPrice" type="number" id="promoPrice" class="form-control" placeholder="79.99" step="0.01" min="0" inputmode="decimal"/>
            <ErrorMessage name="promotionalPrice" class="error-message"/>
          </div>
        </div>

        <!-- Brand Select -->
        <div class="form-group">
          <label for="brandId">Бренд *</label>
          <Field name="brandId" as="select" id="brandId" class="form-control" @change="onBrandChange($event)">
            <option value="" disabled>-- Выберите бренд --</option>
            <option v-for="brand in brands" :key="brand.id" :value="brand.id">{{ brand.name }}</option>
          </Field>
          <ErrorMessage name="brandId" class="error-message"/>
        </div>

        <!-- Tag Group Select -->
        <div class="form-group">
          <label for="tagGroupId">Группа тегов</label>
          <Field name="tagGroupId" as="select" id="tagGroupId" class="form-control" :disabled="tagGroupsLoading || !selectedBrandId">
            <option :value="0">-- Без группы --</option>
            <option v-for="group in tagGroups" :key="group.id" :value="group.id">{{ group.name }}</option>
          </Field>
          <small v-if="tagGroupsLoading">Загрузка групп...</small>
        </div>

        <!-- Visibility Toggle -->
        <div class="form-group-checkbox">
          <Field name="isVisible" type="checkbox" :value="true" id="isVisible" class="form-check-input"/>
          <label for="isVisible">Сделать товар видимым</label>
        </div>

        <!-- Actions -->
        <div class="form-actions">
          <button type="button" class="btn btn-danger" @click="$emit('close')">Отмена</button>
          <button type="submit" class="btn btn-primary">Создать</button>
        </div>
      </Form>
    </template>
  </Modal>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { ErrorMessage, Field, Form } from 'vee-validate';
import * as yup from 'yup';
import Modal from '@/components/Modal.vue';
import { useToast } from 'vue-toastification';
import { useTagStore } from '@/store/tag';
import { useProductStore } from '@/store/product';

const props = defineProps({ 
  brands: { type: Array, required: true },
  selectedBrand: { type: [Number, String], default: null },
  parentId: { type: [Number, String], default: 0 }
});
const emit = defineEmits(['close', 'saved']);
const toast = useToast();
const tagStore = useTagStore();
const productStore = useProductStore();

const tagGroups = ref([]); // плоский список с отступами
const tagGroupsLoading = ref(false);
const selectedBrandId = ref(null);

const schema = yup.object({
  name: yup.string().required('Название обязательно'),
  description: yup.string(),
  price: yup.number().typeError('Цена должна быть числом').required('Цена обязательна').positive('Цена должна быть положительной'),
  promotionalPrice: yup.number().typeError('Цена должна быть числом').positive('Цена должна быть положительной').nullable(),
  brandId: yup.mixed().required('Необходимо выбрать бренд'),
  tagGroupId: yup.number().nullable(),
  isVisible: yup.boolean(),
});

const loadAvailableGroups = async (brandId) => {
  if (!brandId) { tagGroups.value = []; return; }
  tagGroupsLoading.value = true;
  try {
    // BFS по всем уровням: собираем все группы
    const queue = [0];
    const visited = new Set();
    const collected = [];
    while (queue.length) {
      const parentId = queue.shift();
      if (visited.has(parentId)) continue;
      visited.add(parentId);
      const level = await tagStore.fetchTagsByBrand(Number(brandId), parentId);
      (level || []).forEach(t => {
        collected.push(t);
        queue.push(t.id);
      });
    }
    // строим дерево и плоский список с отступами
    const buildTree = (pid = 0) => collected
      .filter(t => (t.parentId ?? 0) === pid)
      .map(t => ({ ...t, children: buildTree(t.id) }));
    const tree = buildTree(0);
    const flatten = (items, depth = 0) => items.flatMap(it => [
      { id: it.id, name: `${'— '.repeat(depth)}${it.name}` },
      ...(it.children?.length ? flatten(it.children, depth + 1) : [])
    ]);
    tagGroups.value = flatten(tree);
  } catch (error) {
    console.error('Ошибка при загрузке групп тегов:', error);
    toast.error('Не удалось загрузить группы тегов.');
  } finally {
    tagGroupsLoading.value = false;
  }
};

const onBrandChange = async (event) => {
  selectedBrandId.value = Number(event.target.value);
  await loadAvailableGroups(selectedBrandId.value);
};

onMounted(async () => {
  // Предзаполним бренд и родителя из контекста админки
  if (props.selectedBrand) {
    selectedBrandId.value = Number(props.selectedBrand);
    await loadAvailableGroups(selectedBrandId.value);
  }
});

async function onSubmit(values, { resetForm }) {
  try {
    const payload = {
      name: values.name,
      description: values.description || '',
      price: Number(values.price),
      promoPrice: values.promotionalPrice ? Number(values.promotionalPrice) : Number(values.price),
      brandId: Number(values.brandId),
      groupTagId: values.tagGroupId ? Number(values.tagGroupId) : 0,
      visible: values.isVisible === undefined ? true : Boolean(values.isVisible),
    };
    const created = await productStore.create(payload);
    toast.success('Товар успешно создан');
    emit('saved', created);
    resetForm();
    emit('close');
  } catch (error) {
    console.error('Ошибка создания товара:', error);
    toast.error(error?.message || 'Не удалось создать товар');
  }
}
</script>

<style scoped>
/* Using styles from CreateBrandModal for consistency */
.form-body {
  display: flex;
  flex-direction: column;
  gap: 15px; /* Adjusted gap */
  width: 100%;
}

.form-row {
  display: flex;
  gap: 15px;
}

.form-row .form-group {
  flex: 1;
}

.form-group {
  text-align: left;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-control {
  width: 100%;
  padding: 10px 14px; /* Adjusted padding */
  border: 1px solid #e0e0e0;
  border-radius: 10px; /* Adjusted radius */
  font-size: 15px; /* Adjusted font size */
  background-color: #f7f7f7;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-control:focus {
  outline: none;
  border-color: var(--primary-color);
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.15);
}

textarea.form-control {
  min-height: 80px;
  resize: vertical;
}

.error-message {
  color: #ff3b30;
  font-size: 13px;
  padding-top: 4px;
}

.form-group-checkbox {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 5px;
}

.form-check-input {
  width: 18px;
  height: 18px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 15px;
}

.btn {
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
  border: none;
  text-align: center;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background-color: var(--primary-color-dark);
  transform: translateY(-2px);
}

.btn-danger {
  background-color: #f0f2f5;
  color: #ff3b30;
}

.btn-danger:hover {
  background-color: #ff3b30;
  color: white;
}
</style>
