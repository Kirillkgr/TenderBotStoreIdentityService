<template>
  <Modal :is-modal-visible="true" :width="560" @close="$emit('close')">
    <template #header>
      <h3 class="no-drag">Информация о бренде</h3>
    </template>
    <template #content>
      <div class="view-wrap">
        <div class="row">
          <span class="label">Название</span>
          <span class="value">{{ brand?.name || '—' }}</span>
        </div>
        <div class="row">
          <span class="label">Юр. наименование</span>
          <span class="value">{{ brand?.organizationName || '—' }}</span>
        </div>
        <div class="row">
          <span class="label">Описание</span>
          <span class="value multiline">{{ brand?.description || '—' }}</span>
        </div>
        <div class="row">
          <span class="label">Telegram Bot Token</span>
          <span class="value code">{{ brand?.telegramBotToken || '—' }}</span>
        </div>
        <div class="row cols">
          <div>
            <span class="label">Создан</span>
            <span class="value">{{ formatDate(brand?.createdAt) }}</span>
          </div>
          <div>
            <span class="label">Изменён</span>
            <span class="value">{{ formatDate(brand?.updatedAt) }}</span>
          </div>
        </div>
        <div class="actions">
          <button class="btn" @click="$emit('close')">Закрыть</button>
        </div>
      </div>
    </template>
  </Modal>
</template>

<script setup>
import Modal from '@/components/Modal.vue';

const props = defineProps({
  brand: {type: Object, required: true}
});

function formatDate(v) {
  try {
    if (!v) return '—';
    const d = new Date(v);
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleString();
  } catch {
    return '—';
  }
}
</script>

<style scoped>
.view-wrap {
  padding: 16px 4px 8px;
  text-align: left;
}

.row {
  display: flex;
  gap: 12px;
  align-items: baseline;
  margin-bottom: 10px;
}

.label {
  min-width: 180px;
  color: var(--muted);
  font-size: 13px;
}

.value {
  color: var(--text);
}

.value.code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.value.multiline {
  white-space: pre-wrap;
}

.cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.btn {
  padding: 6px 12px;
  border: 1px solid var(--card-border);
  border-radius: 8px;
  background: var(--card);
  color: var(--text);
  cursor: pointer;
}
</style>
