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
      <p v-if="product.description" :title="product.description" class="product-desc">
        {{ product.description }}
      </p>
      <div v-if="product.createdAt" :title="formatFull(product.createdAt)" class="meta-time">
        {{ timeAgoStr(product.createdAt) }}
      </div>
      <div class="price-row">
        <div class="prices">
          <template v-if="product.promoPrice && product.promoPrice < product.price">
            <span class="old-price"><span class="val">{{ formatPrice(product.price) }}</span><span class="cur"> ₽</span></span>
            <span class="current-price promo"><span class="val">{{ formatPrice(product.promoPrice) }}</span><span
                class="cur"> ₽</span></span>
          </template>
          <template v-else>
            <span class="current-price"><span class="val">{{ formatPrice(product.price) }}</span><span
                class="cur"> ₽</span></span>
          </template>
        </div>
        <button
          v-if="!hideAddToCart"
          class="add-to-cart-btn"
          aria-label="Добавить в корзину"
          @click.stop="addToCart"
        >
          <svg aria-hidden="true" focusable="false" width="18" height="18" viewBox="0 0 96 96"
               xmlns="http://www.w3.org/2000/svg">
            <path d="M84,74.34H43.05a6.36,6.36,0,0,1-6.12-4.68L20.87,10.51A8.84,8.84,0,0,0,12.36,4H3A1,1,0,0,1,3,2h9.32A10.84,10.84,0,0,1,22.8,10L38.86,69.13a4.35,4.35,0,0,0,4.19,3.21H84a1,1,0,1,1,0,2Z"/>
            <path d="M79.41,61.73H35.54a1,1,0,0,1-1-.74L25.26,26.68a1,1,0,0,1,1-1.27H87.62a6.34,6.34,0,0,1,6,8.42L85.4,57.47A6.35,6.35,0,0,1,79.41,61.73Zm-43.1-2h43.1a4.34,4.34,0,0,0,4.1-2.91l8.21-23.65a4.35,4.35,0,0,0-4.1-5.76H27.53Z"/>
            <path d="M44,94a8.57,8.57,0,1,1,8.56-8.56A8.57,8.57,0,0,1,44,94Zm0-15.13a6.57,6.57,0,1,0,6.56,6.57A6.58,6.58,0,0,0,44,78.87Z"/>
            <path d="M81.22,94a8.57,8.57,0,1,1,8.57-8.56A8.57,8.57,0,0,1,81.22,94Zm0-15.13a6.57,6.57,0,1,0,6.57,6.57A6.57,6.57,0,0,0,81.22,78.87Z"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {defineEmits, defineProps} from 'vue';
import {useRouter} from 'vue-router';
import {useCartStore} from '@/store/cart';
import {useAuthStore} from '@/store/auth';
import {useToast} from 'vue-toastification';
import {useNotificationsStore} from '@/store/notifications';
import {formatLocalDateTime, timeAgo} from '@/utils/datetime';

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
  openInModal: {type: Boolean, default: false},
  hideAddToCart: {type: Boolean, default: false}
});

const emit = defineEmits(['preview']);

const FALLBACK_IMG = 'https://img1.reactor.cc/pics/post/mlp-neuroart-mlp-art-my-little-pony-Lyra-Heartstrings-9077295.jpeg';

const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();
const toast = useToast();
const nStore = useNotificationsStore();

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
    let result = await cartStore.addProduct(props.product, 1);
    if (result?.conflict) {
      const ok = window.confirm('В корзине уже есть товары другого бренда. Очистить корзину и добавить этот товар?');
      if (ok) {
        await cartStore.clearServerCart();
        result = await cartStore.addProduct(props.product, 1);
        toast.info('Корзина очищена. Продолжаем с выбранным брендом.');
      } else {
        toast.info('Добавление отменено: корзина содержит товары другого бренда.');
        return;
      }
    }
    if (result?.cleared) {
      toast.info('Корзина была очищена из-за смены бренда. Продолжайте с товарами выбранного бренда.');
    }
    if (nStore.shouldShowAddedProductToast(props.product.id)) {
      toast.success(`'${props.product.name}' добавлен в корзину!`);
    }
    if (!authStore.isAuthenticated && nStore.shouldShowAnonCartPrompt()) {
      // Предложим зарегистрироваться с привязкой к бренду корзины
      toast.info('Зарегистрируйтесь, чтобы оформить заказ для выбранного бренда.', {timeout: 4000});
    }
  } catch (error) {
    toast.error('Не удалось добавить товар. Возможно, нужно войти в систему.');
  }
}

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

function timeAgoStr(val) {
  return timeAgo(val);
}

function formatFull(val) {
  return formatLocalDateTime(val);
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

.meta-time {
  font-size: 0.8rem;
  color: var(--muted);
  margin: -6px 0 8px 0;
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

.product-desc {
  margin: -6px 0 10px 0;
  color: var(--muted);
  font-size: 0.92rem;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 2.4em;
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

.current-price.promo {
  text-decoration: underline;
  text-underline-offset: 2px;
  text-decoration-thickness: 2px;
}

.prices .cur {
  color: var(--muted);
  margin-left: 4px;
  font-weight: 600;
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
