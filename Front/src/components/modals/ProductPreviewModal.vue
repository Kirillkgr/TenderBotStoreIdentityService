<template>
  <Modal
      :is-modal-visible="true"
      :width="width"
      :height="height"
      :close-on-backdrop="closeOnBackdrop"
      :close-on-esc="closeOnEsc"
      :z-index="zIndex"
      :transparent-backdrop="true"
      @close="$emit('close')"
  >
    <template #header>
      <div class="header-row">
        <h3 class="modal-title">Просмотр товара</h3>
        <button class="close-x" aria-label="Закрыть" @click="$emit('close')">×</button>
      </div>
    </template>

    <template #content>
      <div class="preview-body">
        <div class="image-box" :style="{ backgroundImage: previewBg }"></div>
        <div class="info">
          <h4 class="name" :title="product?.name">{{ product?.name }}</h4>

          <div class="price-block">
            <template v-if="product?.promoPrice && product.promoPrice < product.price">
              <div class="old"><span class="val">{{ formatPrice(product.price) }}</span><span class="cur"> ₽</span>
              </div>
              <div class="new promo"><span class="val">{{ formatPrice(product.promoPrice) }}</span><span
                  class="cur"> ₽</span></div>
            </template>
            <template v-else>
              <div class="new"><span class="val">{{ formatPrice(product?.price) }}</span><span class="cur"> ₽</span>
              </div>
            </template>
          </div>

          <p class="desc">{{ product?.description || '—' }}</p>
        </div>
      </div>

      <div class="footer-actions mt-3">
        <button class="icon-square" title="Добавить в корзину" aria-label="Добавить в корзину">
          <svg aria-hidden="true" focusable="false" width="20" height="20" viewBox="0 0 96 96"
               xmlns="http://www.w3.org/2000/svg">
            <path d="M84,74.34H43.05a6.36,6.36,0,0,1-6.12-4.68L20.87,10.51A8.84,8.84,0,0,0,12.36,4H3A1,1,0,0,1,3,2h9.32A10.84,10.84,0,0,1,22.8,10L38.86,69.13a4.35,4.35,0,0,0,4.19,3.21H84a1,1,0,1,1,0,2Z"/>
            <path d="M79.41,61.73H35.54a1,1,0,0,1-1-.74L25.26,26.68a1,1,0,0,1,1-1.27H87.62a6.34,6.34,0,0,1,6,8.42L85.4,57.47A6.35,6.35,0,0,1,79.41,61.73Zm-43.1-2h43.1a4.34,4.34,0,0,0,4.1-2.91l8.21-23.65a4.35,4.35,0,0,0-4.1-5.76H27.53Z"/>
            <path d="M44,94a8.57,8.57,0,1,1,8.56-8.56A8.57,8.57,0,0,1,44,94Zm0-15.13a6.57,6.57,0,1,0,6.56,6.57A6.58,6.58,0,0,0,44,78.87Z"/>
            <path d="M81.22,94a8.57,8.57,0,1,1,8.57-8.56A8.57,8.57,0,0,1,81.22,94Zm0-15.13a6.57,6.57,0,1,0,6.57,6.57A6.57,6.57,0,0,0,81.22,78.87Z"/>
          </svg>
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup>
import Modal from '@/components/Modal.vue';
import {computed} from 'vue';

const props = defineProps({
  product: { type: Object, required: true },
  width: { type: [String, Number], default: '720px' },
  height: {type: [String, Number], default: '480px'},
  closeOnBackdrop: {type: Boolean, default: true},
  closeOnEsc: {type: Boolean, default: true},
  zIndex: {type: Number, default: 9999}
});

const emit = defineEmits(['close','edit']);

const FALLBACK_IMG = 'https://img1.reactor.cc/pics/post/mlp-neuroart-mlp-art-my-little-pony-Lyra-Heartstrings-9077295.jpeg';
const imageUrl = computed(() => props.product?.imageUrl || FALLBACK_IMG);
const previewBg = computed(() => `url(${imageUrl.value})`);

function formatPrice(val) {
  try {
    if (val === null || val === undefined || val === '') return '—';
    const num = Number(val);
    if (!Number.isFinite(num)) return String(val);
    return new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(num);
  } catch (e) {
    const s = String(val ?? '');
    return s.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  }
}
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.close-x {
  all: unset;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  color: #fff;
  background: #ef4444;
  border: 1px solid #dc2626;
  font-weight: 800;
}

.close-x:hover {
  background: #dc2626;
  border-color: #b91c1c;
}
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
  background: var(--input-bg) center/cover no-repeat;
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: 0 1px 3px var(--shadow-color);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.image-box:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--shadow-color);
}

.info {
  color: var(--text);
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
  scrollbar-width: thin;
  scrollbar-color: var(--border) var(--card);
}

.info::-webkit-scrollbar {
  width: 6px;
}

.info::-webkit-scrollbar-track {
  background: var(--card);
  border-radius: 3px;
}

.info::-webkit-scrollbar-thumb {
  background-color: var(--border);
  border-radius: 3px;
}

.name {
  margin: 0 0 16px 0;
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.3;
  color: var(--text);
  letter-spacing: -0.01em;
}

.price-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
}

.price-block .old {
  color: var(--muted);
  text-decoration: line-through;
  font-size: 1rem;
}

.price-block .new {
  color: var(--text);
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.price-block .new.promo {
  text-decoration: underline;
  text-underline-offset: 3px;
  text-decoration-thickness: 2px;
}

.price-block .cur {
  color: var(--muted);
  margin-left: 4px;
  font-weight: 600;
}


.desc {
  margin: 16px 0 24px;
  color: var(--text);
  line-height: 1.6;
  font-size: 0.9375rem;
  white-space: pre-wrap;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text);
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
.mods h5 { margin: 0 0 6px; font-size: 14px; color: var(--text); }
.mods .stub { font-size: 13px; color: var(--muted); padding: 8px; border: 1px dashed var(--border); border-radius: 8px; }
.footer-actions { display: flex; justify-content: flex-end; gap: 12px; align-items: center; }

.icon-square {
  width: 36px; height: 36px; display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid var(--border); border-radius: 10px; background: var(--input-bg); color: var(--text);
  cursor: pointer; transition: all .18s ease;
}
.icon-square:hover { background: var(--input-bg-hover); border-color: var(--primary); color: var(--text); }

</style>
