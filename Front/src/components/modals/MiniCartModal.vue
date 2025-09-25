<template>
  <ModalBase v-model="open" :closeOnOverlay="false" :lockBodyScroll="false" :noOverlay="true" :title="'Корзина'" :width="720"
             primaryHeader>
    <div class="mini-cart">
      <div v-if="items.length === 0" class="empty">
        <p>Ваша корзина пуста</p>
      </div>
      <div v-else class="list">
        <div v-for="it in items" :key="it.id" class="row">
          <div class="title">
            <div class="name">{{ it.productName || ('Товар #' + it.productId) }}</div>
            <div class="meta">Количество: {{ it.quantity }}</div>
          </div>
          <div class="qty">
            <button class="qty-btn" @click="dec(it)">−</button>
            <span class="qty-val">{{ it.quantity }}</span>
            <button class="qty-btn" @click="inc(it)">+</button>
          </div>
          <div class="price">{{ formatPrice(it.price) }} ₽</div>
          <button aria-label="Удалить" class="del" @click="remove(it.id)">×</button>
        </div>
      </div>
      <div v-if="items.length > 0" class="summary">
        <div class="total">Итого: <b>{{ formatPrice(total) }} ₽</b></div>
        <div class="actions">
          <button :disabled="!canCheckout" :title="checkoutHint" class="btn" @click="onCheckout">
            Оформить заказ
          </button>
          <button v-if="!authStore.isAuthenticated" class="btn btn-secondary" @click="emit('open-login')">
            Войти / Регистрация
          </button>
        </div>
        <small v-if="!authStore.isAuthenticated" class="hint">Для оформления заказа нужно авторизоваться</small>
      </div>
    </div>
  </ModalBase>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import ModalBase from '@/components/ui/ModalBase.vue';
import {useCartStore} from '@/store/cart';
import {useAuthStore} from '@/store/auth';
import {updateCartItemQuantity} from '@/services/cartService';

const props = defineProps({
  modelValue: {type: Boolean, required: true}
});
const emit = defineEmits(['update:modelValue', 'open-login', 'open-checkout']);

const cartStore = useCartStore();
const authStore = useAuthStore();

const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
});

const items = computed(() => cartStore.items || []);
const total = computed(() => cartStore.total || 0);
const canCheckout = computed(() => authStore.isAuthenticated && items.value.length > 0);
const checkoutHint = computed(() => authStore.isAuthenticated ? '' : 'Нужно авторизоваться');

onMounted(async () => {
  await cartStore.fetchCart();
});

async function remove(id) {
  await cartStore.removeItem(id);
}

function onCheckout() {
  if (!authStore.isAuthenticated) return;
  emit('open-checkout');
}

async function inc(item) {
  try {
    await updateCartItemQuantity(item.id, (item.quantity || 0) + 1);
    await cartStore.fetchCart();
  } catch (e) {
    console.error('Не удалось увеличить количество', e);
  }
}

async function dec(item) {
  try {
    const next = (item.quantity || 0) - 1;
    if (next <= 0) {
      await cartStore.removeItem(item.id);
    } else {
      await updateCartItemQuantity(item.id, next);
      await cartStore.fetchCart();
    }
  } catch (e) {
    console.error('Не удалось уменьшить количество', e);
  }
}

function formatPrice(val) {
  try {
    const num = Number(val);
    if (!Number.isFinite(num)) return String(val);
    return new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(num);
  } catch (_) {
    return String(val ?? '');
  }
}
</script>

<style scoped>
.mini-cart {
  display: grid;
  gap: 12px;
}

.empty {
  color: var(--muted);
  text-align: center;
  padding: 24px 6px;
}

.list {
  display: grid;
  gap: 8px;
}

.row {
  display: grid;
  grid-template-columns: 1fr auto auto 28px;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border);
}

.title .name {
  font-weight: 600;
}

.title .meta {
  color: var(--muted);
  font-size: .9rem;
}

.qty {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.qty-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--input-bg);
  color: var(--text);
  cursor: pointer;
  font-weight: 700;
}

.qty-val {
  min-width: 20px;
  text-align: center;
}

.price {
  font-weight: 700;
}

.del {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--input-bg);
  color: var(--text);
  cursor: pointer;
}

.summary {
  display: grid;
  gap: 10px;
}

.total {
  text-align: right;
}

.actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.btn {
  padding: .5rem .8rem;
  border-radius: 8px;
  border: 1px solid var(--primary);
  background: var(--primary);
  color: #fff;
  cursor: pointer;
}

.btn:disabled {
  opacity: .6;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--input-bg);
  color: var(--text);
  border-color: var(--border);
}

.hint {
  color: var(--muted);
}
</style>
