<template>
  <div class="checkout-modal">
    <h3>Оформление заказа (самовывоз)</h3>

    <div v-if="brandId == null" class="warn">Не выбран бренд корзины. Добавьте товар в корзину.</div>

    <div class="field">
      <label>Точка самовывоза</label>
      <div v-if="loading" class="muted">Загрузка точек...</div>
      <select v-model="selectedPickupId" :disabled="loading || pickupPoints.length === 0">
        <option disabled value="">Выберите точку</option>
        <option v-for="p in pickupPoints" :key="p.id" :value="p.id">
          {{ p.name }} — {{ p.address }}
        </option>
      </select>
      <div v-if="!loading && pickupPoints.length === 0" class="muted">Нет доступных точек.</div>
    </div>

    <div class="field">
      <label>Комментарий к заказу (необязательно)</label>
      <textarea v-model="comment" placeholder="Например: без сахара" rows="3"></textarea>
    </div>

    <div class="actions">
      <button :disabled="submitting || !canSubmit" class="btn" @click="submit">
        {{ submitting ? 'Оформляем...' : 'Оформить' }}
      </button>
    </div>

    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="successOrderId" class="success">
      Заказ #{{ successOrderId }} успешно оформлен
      <div class="actions" style="margin-top:8px;">
        <button class="btn" @click="goMyOrders">Перейти в Мои заказы</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref, computed, watch} from 'vue';
import {useRouter} from 'vue-router';
import {useAuthStore} from '@/store/auth';
import {useCartStore} from '@/store/cart';
import {listPickupPoints} from '@/services/pickupService';
import {checkout} from '@/services/checkoutService';
import {useToast} from 'vue-toastification';

const emit = defineEmits(['success']);
const toast = useToast();
const router = useRouter();
const authStore = useAuthStore();

const cart = useCartStore();
const pickupPoints = ref([]);
const loading = ref(false);
const submitting = ref(false);
const selectedPickupId = ref('');
const comment = ref('');
const error = ref('');
const successOrderId = ref(null);

const brandId = computed(() => cart.currentBrandId);
const canSubmit = computed(() => !!brandId.value && !!selectedPickupId.value);

async function loadPickup() {
  error.value = '';
  if (!brandId.value) {
    pickupPoints.value = [];
    return;
  }
  loading.value = true;
  try {
    const {data} = await listPickupPoints(brandId.value);
    pickupPoints.value = Array.isArray(data) ? data : [];
    if (pickupPoints.value.length > 0) {
      selectedPickupId.value = String(pickupPoints.value[0].id);
    }
  } catch (e) {
    console.error('Не удалось загрузить точки самовывоза', e);
    error.value = 'Не удалось загрузить точки самовывоза';
  } finally {
    loading.value = false;
  }
}

async function submit() {
  if (!canSubmit.value) return;
  submitting.value = true;
  error.value = '';
  try {
    // Убедимся, что есть валидный токен
    if (!authStore.accessToken) {
      try {
        await authStore.restoreSession();
      } catch (_) {
      }
    }
    if (!authStore.accessToken) {
      error.value = 'Необходимо войти в систему для оформления заказа.';
      return;
    }
    // Синхронизируем корзину с сервером на момент оформления
    try {
      await cart.fetchCart();
    } catch (_) {
    }
    if (!Array.isArray(cart.items) || cart.items.length === 0) {
      error.value = 'Корзина пуста. Добавьте товары перед оформлением заказа.';
      return;
    }
    const payload = {
      mode: 'PICKUP',
      pickupPointId: Number(selectedPickupId.value),
      comment: comment.value || undefined,
    };
    const {data} = await checkout(payload);
    const orderId = data?.id;
    successOrderId.value = orderId || null;
    toast.success(orderId ? `Заказ #${orderId} оформлен` : 'Заказ оформлен');
    // Обновим корзину — она должна очиститься на сервере
    await cart.fetchCart();
    emit('success');
  } catch (e) {
    console.error('Ошибка при оформлении заказа', e);
    const msg = e?.response?.data?.message || 'Не удалось оформить заказ';
    error.value = msg;
    toast.error(msg);
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  if (cart.items.length === 0) {
    // на всякий случай подтянем корзину
    try {
      await cart.fetchCart();
    } catch {
    }
  }
  await loadPickup();
});

watch(brandId, async () => {
  await loadPickup();
});

function goMyOrders() {
  emit('success');
  router.push({name: 'MyOrders'});
}
</script>

<style scoped>
.checkout-modal {
  text-align: left;
  min-width: 380px;
}

.field {
  margin-bottom: 14px;
}

label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #ccc;
}

select, textarea {
  width: 100%;
  box-sizing: border-box;
}

.muted {
  color: #999;
  font-size: 13px;
}

.error {
  color: #ff6b6b;
  margin-top: 10px;
}

.success {
  color: #4AAE9B;
  margin-top: 10px;
}

.actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.btn {
  background: #4AAE9B;
  border: none;
  color: #fff;
  padding: 8px 14px;
  border-radius: 4px;
  cursor: pointer;
}

.btn[disabled] {
  opacity: .6;
  cursor: default;
}
</style>
