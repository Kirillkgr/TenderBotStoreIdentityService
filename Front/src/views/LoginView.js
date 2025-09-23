import {ref} from 'vue';
import * as yup from 'yup';
import {useAuthStore} from '../store/auth';
import {useRouter} from 'vue-router';
import {useToast} from 'vue-toastification';

export default {
  setup() {
    const schema = yup.object({
      username: yup.string().required('Имя пользователя обязательно'),
      password: yup.string().required('Пароль обязателен'),
    });

    const authStore = useAuthStore();
    const router = useRouter();
    const isSubmitting = ref(false);
    const toast = useToast();

    async function onSubmit(values) {
      isSubmitting.value = true;
      try {
        await authStore.login(values);
        router.push('/');
        toast.success('Вы успешно вошли!');
      } catch (error) {
        toast.error(error.response?.data?.message || 'Неверное имя пользователя или пароль.');
        console.error(error);
      } finally {
        isSubmitting.value = false;
      }
    }

    return {
      schema,
      isSubmitting,
      onSubmit,
    };
  },
};
