<template>
  <Modal :is-modal-visible="true" :width="width" :height="height" @close="$emit('close')">
    <template #header>
      <h3 class="modal-title">Просмотр товара</h3>
    </template>

    <template #content>
      <div class="preview-body">
        <div class="image-box" :style="{ backgroundImage: previewBg }"></div>
        <div class="info">
          <h4 class="name" :title="product?.name">{{ product?.name }}</h4>

          <div class="price-row">
            <template v-if="product?.promoPrice && product.promoPrice < product.price">
              <span class="old">{{ formatPrice(product.price) }}</span>
              <span class="new">{{ formatPrice(product.promoPrice) }}</span>
              <span class="promo-badge">Промо</span>
            </template>
            <template v-else>
              <span class="new">{{ formatPrice(product?.price) }}</span>
            </template>
            <span class="visible-badge" :class="product?.visible ? 'on' : 'off'">{{ product?.visible ? 'Видим' : 'Скрыт' }}</span>
          </div>

          <p class="desc">{{ product?.description || '—' }}</p>

          <div class="mods">
            <h5>Модификаторы</h5>
            <div class="stub">Заглушка для модификаторов (скоро)</div>
          </div>
        </div>
      </div>

      <div class="footer-actions mt-3">
        <button class="btn btn-outline-secondary" @click="$emit('close')">Закрыть</button>
        <button class="btn btn-primary">
          <i class="bi bi-cart-plus me-1"></i>
          В корзину (заглушка)
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup>
import Modal from '@/components/Modal.vue';
import { computed } from 'vue';

const props = defineProps({
  product: { type: Object, required: true },
  width: { type: [String, Number], default: '720px' },
  height: { type: [String, Number], default: '480px' }
});

const emit = defineEmits(['close']);

const FALLBACK_IMG = 'https://img1.reactor.cc/pics/post/mlp-neuroart-mlp-art-my-little-pony-Lyra-Heartstrings-9077295.jpeg';
const imageUrl = computed(() => props.product?.imageUrl || FALLBACK_IMG);
const previewBg = computed(() => `url(${imageUrl.value})`);

function formatPrice(val) {
  if (val === null || val === undefined || val === '') return '';
  const num = Number(val);
  if (Number.isNaN(num)) return String(val);
  return num.toFixed(2);
}
</script>

<style scoped>
.preview-body {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  align-items: start;
  padding: 8px 4px 4px 0;
}

.image-box {
  width: 300px;
  height: 300px;
  background: #f8fafc center/cover no-repeat;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.image-box:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.info {
  color: #1e293b;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 #f1f5f9;
}

.info::-webkit-scrollbar {
  width: 6px;
}

.info::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.info::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 3px;
}

.name {
  margin: 0 0 16px 0;
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.3;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.price-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.old {
  color: #94a3b8;
  font-size: 1rem;
  text-decoration: line-through;
}

.new {
  color: #0f172a;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.promo-badge {
  background: linear-gradient(135deg, #fcd34d 0%, #fbbf24 100%);
  color: #78350f;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.visible-badge {
  font-size: 0.75rem;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 16px;
}

.visible-badge.on {
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.visible-badge.off {
  background: #f8fafc;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.desc {
  margin: 16px 0 24px;
  color: #475569;
  line-height: 1.6;
  font-size: 0.9375rem;
  white-space: pre-wrap;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  padding: 0;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .preview-body {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .image-box {
    width: 100%;
    max-width: 300px;
    margin: 0 auto;
  }
  
  .info {
    max-height: none;
    padding-right: 0;
  }
}
.mods h5 { margin: 0 0 6px; font-size: 14px; color: #111827; }
.mods .stub { font-size: 13px; color: #6b7280; padding: 8px; border: 1px dashed #d1d5db; border-radius: 8px; }
.footer-actions { display: flex; justify-content: flex-end; gap: 8px; }
</style>
