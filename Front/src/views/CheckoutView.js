import {ref} from 'vue';
import {ErrorMessage, Field, Form} from 'vee-validate';
import * as yup from 'yup';
import {useRouter} from 'vue-router';
import * as orderService from '../services/orderService';
import {useToast} from 'vue-toastification';

export default {
  components: {
    Form,
    Field,
    ErrorMessage,
  },
  setup() {
    const router = useRouter();
    const isSubmitting = ref(false);
    const toast = useToast();

    const schema = yup.object({
      address: yup.string().required('Адрес обязателен для заполнения'),
      phone: yup.string().required('Телефон обязателен').matches(/^[\d\s()+-]+$/, 'Некорректный формат телефона'),
      comment: yup.string(),
    });

    async function handlePlaceOrder(values) {
      isSubmitting.value = true;
      try {
        await orderService.placeOrder(values);
        toast.success('Ваш заказ успешно оформлен!');
        // Опционально: очистить корзину после заказа
        // const cartStore = useCartStore();
        // cartStore.clearCart();
        router.push('/profile'); // Перенаправляем в профиль, где будут заказы
      } catch (error) {
        toast.error('Произошла ошибка при оформлении заказа.');
        console.error(error);
      } finally {
        isSubmitting.value = false;
      }
    }

    return {
      schema,
      isSubmitting,
      handlePlaceOrder,
    };
  },
};
