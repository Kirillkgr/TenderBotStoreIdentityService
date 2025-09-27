<template>
  <div class="modal" @click.self="emit('close')">
    <div class="modal__dialog">
      <div class="modal__header">
        <h3>Отзыв по заказу #{{ order?.id }}</h3>
        <button class="btn" @click="emit('close')">×</button>
      </div>
      <div class="modal__body">
        <div v-if="order?.rating" class="rating-wrap">
          <span aria-hidden="true" class="stars">
            <span v-for="n in 5" :key="n" :class="{ active: n <= Number(order.rating) }" class="star">★</span>
          </span>
          <span class="rating-text">{{ Number(order.rating).toFixed(1) }}/5</span>
        </div>
        <div v-else class="muted">Оценка отсутствует</div>
        <div v-if="order?.reviewComment" class="review-text" style="margin-top:8px; white-space: pre-wrap;">
          {{ order.reviewComment }}
        </div>
        <div v-else class="muted" style="margin-top:8px;">Комментарий не оставлен</div>
      </div>
      <div class="modal__footer">
        <button class="btn" @click="emit('close')">Закрыть</button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({order: {type: Object, required: true}});
const emit = defineEmits(['close']);
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

.rating-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stars .star {
  color: #888;
  font-size: 20px;
}

.stars .star.active {
  color: #ffd54f;
  text-shadow: 0 0 4px rgba(255, 213, 79, .5);
}

.rating-text {
  color: #bbb;
  font-size: .95rem;
}

.muted {
  color: #999;
}
</style>
