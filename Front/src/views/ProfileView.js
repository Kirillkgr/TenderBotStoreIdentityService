import {computed, onMounted} from 'vue';
import {useAuthStore} from '../store/auth';
import {useOrderStore} from '../store/order';
import OrderListItem from '../components/OrderListItem.vue';

export default {
  components: {
    OrderListItem,
  },
  setup() {
    const authStore = useAuthStore();
    const orderStore = useOrderStore();

    const formattedDateOfBirth = computed(() => {
      if (!authStore.user || !authStore.user.dateOfBirth) return '';
      const [year, month, day] = authStore.user.dateOfBirth.split('-');
      return `${day}.${month}`;
    });

    onMounted(() => {
      orderStore.fetchOrders();
    });

    return {
      authStore,
      orderStore,
      formattedDateOfBirth,
    };
  },
};
