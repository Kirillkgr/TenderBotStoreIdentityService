<template>
  <div
    class="product-card"
    :class="{ 'cursor-pointer': !openInModal, 'has-promo': product.promoPrice && product.promoPrice < product.price }"
    @click="handleClick"
  >
    <div class="image-container">
      <img
        :src="product.imageUrl || FALLBACK_IMG"
        :alt="product.name"
        class="product-image"
        @error="handleImageError"
      />
      <div v-if="product.promoPrice && product.promoPrice < product.price" class="promo-flag">
        <span>Акция</span>
      </div>
    </div>
    <div class="card-content">
      <h3 class="product-name" :title="product.name">
        {{ product.name }}
      </h3>
      <div class="price-row">
        <div class="prices">
          <template v-if="product.promoPrice && product.promoPrice < product.price">
            <span class="old-price">{{ formatPrice(product.price) }}</span>
            <span class="current-price">{{ formatPrice(product.promoPrice) }}</span>
          </template>
          <template v-else>
            <span class="current-price">{{ formatPrice(product.price) }}</span>
          </template>
        </div>
        <button
          v-if="!hideAddToCart"
          class="add-to-cart-btn"
          aria-label="Добавить в корзину"
          @click.stop="addToCart"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5"
            viewBox="0 0 20 20"
          >
            <path
              fill-rule="evenodd"
              d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"
              clip-rule="evenodd"
            />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../store/cart';
import { useToast } from 'vue-toastification';

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
  openInModal: { type: Boolean, default: false }
});

const emit = defineEmits(['preview']);

const FALLBACK_IMG = 'https://img1.reactor.cc/pics/post/mlp-neuroart-mlp-art-my-little-pony-Lyra-Heartstrings-9077295.jpeg';

const router = useRouter();
const cartStore = useCartStore();
const toast = useToast();

function handleClick() {
  if (props.openInModal) {
    emit('preview', props.product);
  } else {
    router.push(`/product/${props.product.id}`);
  }
}

function handleImageError() {
  // handle image error
}

async function addToCart() {
  try {
    await cartStore.addItem(props.product.id, 1);
    toast.success(`'${props.product.name}' добавлен в корзину!`);
  } catch (error) {
    toast.error('Не удалось добавить товар. Возможно, нужно войти в систему.');
  }
}

function formatPrice(price) {
  return `${price} ₽`;
}
</script>

<style scoped>
.product-card {
  background: var(--card);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 6px 16px var(--shadow-color);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--card-border);
}

.product-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 22px var(--shadow-color);
  border-color: var(--border);
}

.product-card.has-promo .image-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #f59e0b 0%, #fbbf24 100%);
  z-index: 2;
}

.image-container {
  position: relative;
  width: 100%;
  aspect-ratio: 1/1;
  overflow: hidden;
  background-color: var(--input-bg);
  border-radius: 14px 14px 0 0;
}

.promo-flag {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #fcd34d 0%, #fbbf24 100%);
  color: #78350f;
  font-size: 0.7rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  z-index: 2;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform 0.5s ease;
}

.product-card:hover .product-image {
  transform: scale(1.05);
}

.card-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  background: var(--card);
  border-radius: 0 0 16px 16px;
}

.product-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 12px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 2.8em;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}

.prices {
  display: flex;
  align-items: center;
  gap: 6px;
}

.old-price {
  color: var(--muted);
  font-size: 0.85rem;
  text-decoration: line-through;
}

.current-price {
  color: var(--text);
  font-size: 1.1rem;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.add-to-cart-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background-color: var(--input-bg);
  color: var(--text);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.add-to-cart-btn:hover { background-color: var(--input-bg-hover); color: var(--text); border-color: var(--primary); }

.add-to-cart-btn:active {
  transform: scale(0.95);
}

.add-to-cart-btn svg {
  width: 16px;
  height: 16px;
}

.cursor-pointer {
  cursor: pointer;
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .product-card {
    border-radius: 14px;
  }
  
  .image-container {
    border-radius: 12px 12px 0 0;
  }
  
  .card-content {
    padding: 12px;
  }
  
  .product-name {
    font-size: 0.95rem;
    margin-bottom: 10px;
  }
  
  .current-price {
    font-size: 1.05rem;
  }
  
  .old-price {
    font-size: 0.8rem;
  }
  
  .add-to-cart-btn {
    padding: 0.4rem 0.8rem;
    font-size: 0.9rem;
  }
}
</style>
