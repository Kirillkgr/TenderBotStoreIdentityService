<template>
  <Form @submit="onSubmit" class="login-form">
    <div class="form-group">
      <Field
        name="login"
        type="text"
        id="login-field"
        class="form-control"
        :class="{ 'is-invalid': loginError }"
        placeholder="Логин"
      />
    </div>

    <div class="form-group">
      <Field
        name="password"
        type="password"
        id="password-field"
        class="form-control"
        :class="{ 'is-invalid': passwordError }"
        placeholder="Пароль"
      />
    </div>

    <button type="submit" :disabled="isSubmitting" class="submit-btn">Войти</button>

    <div class="separator">или войти через</div>

    <div class="social-login">
      <button class="social-btn">
        <svg><use xlink:href="#logo_yandex_color_circle_24"></use></svg>
      </button>
      <button class="social-btn">
        <svg><use xlink:href="#logo_google_color_24"></use></svg>
      </button>
      <button class="social-btn">
        <svg><use xlink:href="#logo_vk_color_24"></use></svg>
      </button>
      <button class="social-btn">
        <svg><use xlink:href="#logo_telegram_color_24"></use></svg>
      </button>
    </div>
  </Form>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Form, Field } from 'vee-validate';
import { useAuthStore } from '../store/auth';
import { useToast } from 'vue-toastification';

const emit = defineEmits(['success', 'close']);

const authStore = useAuthStore();
const isSubmitting = ref(false);
const loginError = ref(false);
const passwordError = ref(false);
const toast = useToast();

onMounted(() => {
  setTimeout(() => {
    const field = document.getElementById('login-field');
    if (field) field.focus();
  }, 100);
});

async function onSubmit(values) {
  // Reset errors on each submission attempt
  loginError.value = false;
  passwordError.value = false;

  // Manual validation
  if (!values.login) {
    loginError.value = true;
  }
  if (!values.password) {
    passwordError.value = true;
  }

  // If there are validation errors, stop the submission.
  if (loginError.value || passwordError.value) {
    return;
  }

  isSubmitting.value = true;
  try {
    await authStore.login({ username: values.login, password: values.password });
    toast.success('Вы успешно вошли!');
    emit('success');
    emit('close');
  } catch (error) {
    if (error.response?.status === 429) {
      toast.error('Сервис в данный момент недоступен. Попробуйте позже.');
    } else {
      toast.error(error.response?.data?.message || 'Неверный логин или пароль.');
    }
    console.error('Login error:', error);
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<style scoped>
.login-form {
  display: inline-block; /* Allow parent padding to be clickable */
  width: 100%;
  max-width: 320px;
  margin: 0 auto;
}

.login-form > * + * {
  margin-top: 18px; /* Replaces gap */
}

.form-control {
  user-select: text; /* Allow text selection for inputs */
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 16px;
  background-color: #f7f7f7;
  color: var(--color-text-primary-light);
  transition: all 0.2s ease;
}

.form-control::placeholder {
  color: #9e9e9e;
}

.form-control:focus {
  outline: none;
  border-color: #007aff;
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.15);
}

.form-control.is-invalid {
  border-color: #ff3b30;
}

.form-control.is-invalid:focus {
  border-color: #ff3b30;
  box-shadow: 0 0 0 3px rgba(255, 59, 48, 0.2);
}

.submit-btn {
  width: 100%;
  padding: 14px;
  border-radius: 12px;
  font-size: 17px;
  font-weight: 500;
  color: #fff;
  background-color: #007aff;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  background-color: #005ecb;
  transform: translateY(-1px);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.separator {
  display: flex;
  align-items: center;
  text-align: center;
  color: #aeaeae;
  font-size: 13px;
}

.separator::before,
.separator::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid #efefef;
}

.separator:not(:empty)::before { margin-right: .75em; }
.separator:not(:empty)::after { margin-left: .75em; }

.social-login {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.social-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 1px solid #e8e8e8;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  padding: 8px; /* Adjusted padding */
}

.social-btn svg {
  width: 100%;
  height: 100%;
}

.social-btn:hover {
  transform: translateY(-2px);
  border-color: #dcdcdc;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.social-btn img {
  max-width: 100%;
  height: auto;
  object-fit: contain;
}

.social-btn.vk {
  background-color: #0077ff;
  border-color: #0077ff;
  padding: 0; /* Reset padding for the container */
}

.social-btn.vk img {
  filter: brightness(0) invert(1);
}

.social-btn.vk:hover { background-color: #0062d1; }

</style>
