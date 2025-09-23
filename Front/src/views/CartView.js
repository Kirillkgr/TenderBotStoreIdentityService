import {onMounted} from 'vue';
import {useCartStore} from '../store/cart';
import {useToast} from 'vue-toastification';

export default {
  setup() {
    const cartStore = useCartStore();
    const toast = useToast();

    onMounted(() => {
      cartStore.fetchCart();
    });

    async function handleRemoveItem(cartItemId) {
      try {
        await cartStore.removeItem(cartItemId);
        toast.success('Товар удален из корзины.');
      } catch (error) {
        toast.error('Не удалось удалить товар.');
      }
    }

    return {
      cartStore,
      handleRemoveItem,
    };
  },
};
