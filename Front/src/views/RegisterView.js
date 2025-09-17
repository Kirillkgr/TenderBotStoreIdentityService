import { ref } from 'vue';
import { useAuthStore } from '../store/auth';
import { useRouter } from 'vue-router';
import * as yup from 'yup';

export default {
  setup() {
    const authStore = useAuthStore();
    const router = useRouter();

    const username = ref('');
    const password = ref('');
    const email = ref('');
    const error = ref(null);

    const schema = yup.object({
      username: yup.string().required('Имя пользователя обязательно'),
      password: yup.string().required('Пароль обязателен').min(6, 'Пароль должен быть не менее 6 символов'),
      email: yup.string().email('Некорректный email').required('Email обязателен'),
    });

    const register = async () => {
      try {
        await schema.validate({ username: username.value, password: password.value, email: email.value }, { abortEarly: false });
        await authStore.register({ username: username.value, password: password.value, email: email.value });
        await router.push('/');
      } catch (err) {
        if (err.inner) {
          error.value = err.inner.map(e => e.message).join(', ');
        } else {
          error.value = 'Ошибка регистрации. Пожалуйста, попробуйте еще раз.';
        }
        console.error('Ошибка регистрации:', err);
      }
    };

    return {
      username,
      password,
      email,
      error,
      register,
    };
  },
};
