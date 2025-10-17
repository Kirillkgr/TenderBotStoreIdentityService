<template>
  <Modal :is-modal-visible="true" :width="520" @close="$emit('close')">
    <template #header>
      <h3 class="no-drag">{{ title }}</h3>
    </template>
    <template #content>
      <div class="form-wrap">
        <div class="field">
          <label>Название</label>
          <input v-model="form.name" type="text"/>
        </div>
        <div class="field">
          <label>Юр. наименование</label>
          <input v-model="form.organizationName" type="text"/>
        </div>
        <div class="field">
          <label>Описание</label>
          <textarea v-model="form.description" rows="3"></textarea>
        </div>
        <div class="field">
          <label>Telegram Bot Token</label>
          <input v-model="form.telegramBotToken" type="text"/>
        </div>
        <div class="actions">
          <button class="btn" type="button" @click="$emit('close')">Отмена</button>
          <button :disabled="saving" class="btn primary" type="button" @click="save">
            <span v-if="saving" class="spinner"></span>
            Сохранить
          </button>
        </div>
      </div>
    </template>
  </Modal>
</template>

<script setup>
import {computed, reactive, ref, watch} from 'vue';
import Modal from '@/components/Modal.vue';
import {createBrand, updateBrand} from '@/services/brandService';

const props = defineProps({
  brand: {type: Object, required: false, default: null},
  mode: {type: String, default: 'edit'} // 'edit' | 'create'
});

const emit = defineEmits(['close', 'saved']);
const saving = ref(false);

const form = reactive({
  id: null,
  name: '',
  organizationName: '',
  description: '',
  telegramBotToken: ''
});

const title = computed(() => props.mode === 'create' ? 'Создать бренд' : 'Редактировать бренд');

watch(() => props.brand, (b) => {
  if (!b) {
    form.id = null;
    form.name = '';
    form.organizationName = '';
    form.description = '';
    form.telegramBotToken = '';
    return;
  }
  form.id = b.id || null;
  form.name = b.name || '';
  form.organizationName = b.organizationName || '';
  form.description = b.description || '';
  form.telegramBotToken = b.telegramBotToken || '';
}, {immediate: true});

async function save() {
  saving.value = true;
  const payload = {
    name: form.name,
    organizationName: form.organizationName,
    description: form.description,
    telegramBotToken: form.telegramBotToken,
  };
  try {
    if (props.mode === 'create') {
      const resp = await createBrand(payload);
      emit('saved', resp?.data ?? payload);
    } else {
      const resp = await updateBrand(form.id, payload);
      emit('saved', resp?.data ?? payload);
    }
  } catch (e) {
    console.error('Не удалось сохранить бренд', e);
    alert(e?.response?.data?.message || 'Не удалось сохранить бренд');
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.form-wrap {
  padding: 16px 4px 8px;
  text-align: left;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

label {
  font-size: 13px;
  color: var(--muted);
}

input, textarea {
  padding: 8px 10px;
  border: 1px solid var(--card-border);
  border-radius: 8px;
  background: var(--card);
  color: var(--text);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.btn {
  padding: 6px 12px;
  border: 1px solid var(--card-border);
  border-radius: 8px;
  background: var(--card);
  color: var(--text);
  cursor: pointer;
}

.btn.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, .5);
  border-top-color: #fff;
  border-radius: 50%;
  margin-right: 6px;
  animation: spin .8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
