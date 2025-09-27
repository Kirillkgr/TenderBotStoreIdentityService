<template>
  <div class="modal" @click.self="emit('close')">
    <div class="modal__dialog">
      <div class="modal__header">
        <h3>Оцените заказ #{{ order?.id }}</h3>
        <button class="btn" @click="emit('close')">×</button>
      </div>
      <div class="modal__body">
        <div class="rating">
          <button
              v-for="n in 5"
              :key="n"
              :class="{ active: rating >= n }"
              aria-label="Выставить оценку"
              class="star"
              type="button"
              @click="rating = n"
          >★
          </button>
        </div>
        <div v-if="rating>0" class="improve">
          <label v-if="rating<5" class="label">Что нам улучшить, чтобы вы поставили 5 звёзд?</label>
          <label v-else class="label">Ваш комментарий (необязательно)</label>
          <textarea v-model="comment" class="input" placeholder="Напишите отзыв (по желанию)" rows="4"></textarea>
          <div v-if="rating===5" class="thanks" style="margin-top:6px;">Спасибо за высокую оценку!</div>
        </div>
      </div>
      <div v-if="!alreadyReviewed" class="modal__footer">
        <button :disabled="!rating || saving" class="btn" @click="submit">Отправить</button>
      </div>
      <div v-else class="modal__footer">
        <div class="muted" style="width:100%">Отзыв уже отправлен. Изменение не требуется.</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import orderClientService from '@/services/orderClientService';

const props = defineProps({
  order: {type: Object, required: true},
});
const emit = defineEmits(['close', 'submitted']);

const rating = ref(0);
const comment = ref('');
const saving = ref(false);

const alreadyReviewed = computed(() => {
  try {
    const rated = Number.isFinite(Number(props.order?.rating)) && Number(props.order?.rating) > 0;
    const local = localStorage.getItem('reviewed_' + props.order?.id) === '1';
    return rated || local;
  } catch (_) {
    return Number(props.order?.rating) > 0;
  }
});

onMounted(() => {
  // Если отзыв уже существует, заполним форму значениями
  try {
    const r = Number(props.order?.rating ?? 0);
    if (Number.isFinite(r) && r > 0) rating.value = r;
    if (props.order?.reviewComment) comment.value = String(props.order.reviewComment);
  } catch (_) {
  }
});

async function submit() {
  if (!rating.value) return;
  saving.value = true;
  try {
    await orderClientService.submitReview(props.order.id, rating.value, comment.value || null);
    emit('submitted', props.order.id);
  } catch (e) {
    // показать ошибку/тост, если нужно
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal__dialog {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  width: min(520px, 96vw);
}

.modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-bottom: 1px solid var(--border);
}

.modal__body {
  padding: 12px;
}

.modal__footer {
  padding: 12px;
  display: flex;
  justify-content: flex-end;
}

.btn {
  border: 1px solid var(--border);
  background: var(--input-bg);
  color: var(--text);
  border-radius: 8px;
  padding: 6px 10px;
  cursor: pointer;
}

.input {
  width: 100%;
  border: 1px solid var(--border);
  background: var(--input-bg);
  color: var(--text);
  border-radius: 8px;
  padding: 6px 10px;
}

.rating {
  display: flex;
  gap: 6px;
  font-size: 28px;
  margin-bottom: 8px;
}

.star {
  background: transparent;
  border: none;
  color: #888;
  cursor: pointer;
  padding: 4px;
}

.star.active {
  color: #ffd54f;
  text-shadow: 0 0 6px rgba(255, 213, 79, .55);
}

.label {
  display: block;
  margin-bottom: 6px;
  color: #bbb;
}

.thanks {
  color: #9ccc65;
  font-size: .95rem;
}
</style>
