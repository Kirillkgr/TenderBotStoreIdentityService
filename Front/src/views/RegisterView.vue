<template>
  <Form @submit="onSubmit" class="register-form">
        <div class="form-group username-group">
      <Field name="username" type="text" class="form-control" :class="{ 'is-invalid': errors.username, 'has-feedback': usernameStatus !== 'idle' }" placeholder="Логин" v-model="username" />
      <span v-if="usernameStatus === 'available'" class="status-icon available" title="Логин свободен">✓</span>
      <span v-if="usernameStatus === 'taken'" class="status-icon taken" title="Логин занят">✗</span>
      <span v-if="usernameStatus === 'checking'" class="status-icon checking">...</span>
      <ErrorMessage name="username" class="error-message" />
    </div>
    <div class="form-group">
      <Field name="firstName" type="text" class="form-control" :class="{ 'is-invalid': errors.firstName }" placeholder="Имя" v-model="firstName" />
      <ErrorMessage name="firstName" class="error-message" />
    </div>
    <div class="form-group">
      <Field name="lastName" type="text" class="form-control" :class="{ 'is-invalid': errors.lastName }" placeholder="Фамилия" v-model="lastName" />
      <ErrorMessage name="lastName" class="error-message" />
    </div>
    <div class="form-group">
      <Field name="email" type="email" class="form-control" :class="{ 'is-invalid': errors.email }" placeholder="Email" v-model="email" />
      <ErrorMessage name="email" class="error-message" />
    </div>
                <div class="form-group password-group">
      <Field name="password" :type="passwordFieldType" class="form-control" :class="{ 'is-invalid': errors.password, 'is-valid': passwordsMatch }" placeholder="Пароль" v-model="password" />
      <span @click="togglePasswordVisibility" class="password-toggle-icon">
        <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
      </span>
      <ErrorMessage name="password" class="error-message" />
      <div v-if="password" class="password-strength-container">
        <div class="password-strength-meter">
          <div class="strength-section" :class="passwordStrengthClasses[0]"></div>
          <div class="strength-section" :class="passwordStrengthClasses[1]"></div>
          <div class="strength-section" :class="passwordStrengthClasses[2]"></div>
        </div>
        <span class="strength-label" :class="passwordStrengthClasses[3]">{{ passwordStrengthClasses[4] }}</span>
      </div>
    </div>
        <div class="form-group">
      <Field name="passwordConfirmation" :type="passwordConfirmationFieldType" class="form-control" :class="{ 'is-invalid': errors.passwordConfirmation, 'is-valid': passwordsMatch }" placeholder="Повторите пароль" v-model="passwordConfirmation" />
      <span @click="togglePasswordConfirmationVisibility" class="password-toggle-icon">
        <svg v-if="showPasswordConfirmation" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
      </span>
      <ErrorMessage name="passwordConfirmation" class="error-message" />
    </div>
    <button type="submit" :disabled="isSubmitting || !meta.valid" class="submit-btn">Зарегистрироваться</button>

    <div class="separator">или через</div>

    <div class="social-login">
      <button type="button" class="social-btn">
        <svg><use xlink:href="#logo_yandex_color_circle_24"></use></svg>
      </button>
      <button type="button" class="social-btn">
        <svg><use xlink:href="#logo_google_color_24"></use></svg>
      </button>
      <button type="button" class="social-btn">
        <svg><use xlink:href="#logo_vk_color_24"></use></svg>
      </button>
      <button type="button" class="social-btn">
        <svg><use xlink:href="#logo_telegram_color_24"></use></svg>
      </button>
    </div>
  </Form>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useForm, Field, ErrorMessage } from 'vee-validate';
import * as Yup from 'yup';
import { useAuthStore } from '@/store/auth';
import { useToast } from 'vue-toastification';


const authStore = useAuthStore();
const isSubmitting = ref(false);
const usernameStatus = ref('idle'); // idle, checking, available, taken
const toast = useToast();

const debounce = (fn, delay) => {
  let timeoutId;
  return function (...args) {
    clearTimeout(timeoutId);
    return new Promise((resolve) => {
      timeoutId = setTimeout(() => {
        resolve(fn(...args));
      }, delay);
    });
  };
};

const usernameValidationCache = new Map();

const checkUsernameUnique = debounce(async (username) => {
  if (!username) {
    usernameStatus.value = 'idle';
    return true;
  }

  if (usernameValidationCache.has(username)) {
    const isAvailable = usernameValidationCache.get(username);
    usernameStatus.value = isAvailable ? 'available' : 'taken';
    return isAvailable;
  }

  usernameStatus.value = 'checking';
  try {
    const response = await authStore.checkUsername(username);
    const isAvailable = response.available;
    usernameValidationCache.set(username, isAvailable);
    usernameStatus.value = isAvailable ? 'available' : 'taken';
    return isAvailable;
  } catch (error) {
    console.error("Username check failed:", error);

    if (error.response?.status === 429) {
      toast.error('Сервис временно недоступен, попробуйте позже.');
      usernameStatus.value = 'idle';
      return true; // Не блокируем пользователя из-за перегрузки
    }

    // Для всех других ошибок (включая 409 Conflict) считаем, что логин занят
    usernameStatus.value = 'taken';
    return false;
  }
}, 500);

const schema = Yup.object().shape({
  username: Yup.string()
    .required('Логин обязателен')
    .test('is-unique', 'Этот логин уже занят', value => checkUsernameUnique(value)),
  lastName: Yup.string().required('Фамилия обязательна'),
  firstName: Yup.string().required('Имя обязательно'),
  email: Yup.string().required('Email обязателен').email('Неверный формат email'),
  password: Yup.string().required('Пароль обязателен').min(6, 'Пароль должен быть не менее 6 символов'),
  passwordConfirmation: Yup.string()
    .oneOf([Yup.ref('password')], 'Пароли должны совпадать')
    .required('Подтверждение пароля обязательно'),
});

const { errors, handleSubmit, defineField, meta, setErrors } = useForm({
  validationSchema: schema,
});

const [username, usernameAttrs] = defineField('username');
const [lastName] = defineField('lastName');
const [firstName] = defineField('firstName');
const [email] = defineField('email');
const [password] = defineField('password');
const [passwordConfirmation] = defineField('passwordConfirmation');



watch(username, (newValue) => {
  if (!newValue) {
    usernameStatus.value = 'idle';
  }
});

const calculatePasswordStrength = (password) => {
  if (!password) return 0;

  const checks = [
    password.length >= 8,
    /[a-z]/.test(password) && /[A-Z]/.test(password), // Mixed case
    /\d/.test(password), // Numbers
    /[^a-zA-Z0-9]/.test(password) // Special chars
  ];

  const passedChecks = checks.filter(Boolean).length;

  if (password.length < 8) return passedChecks > 1 ? 1 : 0;

  if (passedChecks === 1) return 1; // Weak (just long enough)
  if (passedChecks >= 2 && passedChecks < 4) return 2; // Medium
  if (passedChecks === 4) return 3; // Strong

  return 0;
};

const passwordStrengthClasses = computed(() => {
  const score = calculatePasswordStrength(password.value);
  const classes = ['empty', 'empty', 'empty'];
  let label = '';
  let colorClass = '';

  if (score > 0) {
    const strengthMap = {
      1: { color: 'weak', label: 'Слабый' },
      2: { color: 'medium', label: 'Средний' },
      3: { color: 'strong', label: 'Надёжный' },
    };
    const { color, label: currentLabel } = strengthMap[score];
    label = currentLabel;
    colorClass = color;
    for (let i = 0; i < score; i++) {
      classes[i] = color;
    }
  }
  return [...classes, colorClass, label];
});

const showPassword = ref(false);
const showPasswordConfirmation = ref(false);

const passwordFieldType = computed(() => (showPassword.value ? 'text' : 'password'));
const passwordConfirmationFieldType = computed(() => (showPasswordConfirmation.value ? 'text' : 'password'));

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value;
};

const togglePasswordConfirmationVisibility = () => {
  showPasswordConfirmation.value = !showPasswordConfirmation.value;
};

const passwordsMatch = computed(() => {
  return password.value && password.value === passwordConfirmation.value && !errors.value.password && !errors.value.passwordConfirmation;
});

const onSubmit = handleSubmit(async (values) => {
  isSubmitting.value = true;
  try {
    const registrationData = { ...values };
    delete registrationData.passwordConfirmation;

    await authStore.register(registrationData);
    toast.success('Вы успешно зарегистрировались! Теперь вы можете войти.');
    emit('success');
  } catch (error) {
    const errorMessage = error.response?.data?.message || 'Ошибка регистрации.';
    toast.error(errorMessage);
    if (error.response?.data?.errors) {
      setErrors(error.response.data.errors);
    }
    console.error('Ошибка регистрации:', error);
  } finally {
    isSubmitting.value = false;
  }
});
</script>

<style scoped>
.register-form {
  display: inline-block;
  width: 100%;
  max-width: 320px;
  margin: 0 auto;
}

.register-form > * + * {
  margin-top: 18px;
}

.username-group {
  position: relative;
}

.form-control {
  padding-right: 45px; /* Увеличиваем отступ для иконки глаза */
}

.form-control.has-feedback {
  padding-right: 45px; /* Место для иконки */
}

.password-toggle-icon {
  position: absolute;
  right: 15px;
  top: 15px; /* Выравнивание по вертикали */
  cursor: pointer;
  color: #888;
  z-index: 3;
}

.status-icon {
  position: absolute;
  right: 15px;
  top: 15px;
  font-size: 20px;
  line-height: 1;
  pointer-events: all; /* Чтобы title работал */
  cursor: help;
}

.status-icon.available {
  color: #34c759;
}

.status-icon.taken {
  color: #ff3b30;
}

.status-icon.checking {
  color: #9e9e9e;
  font-size: 14px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

.password-group {
  /* Увеличиваем отступ снизу, чтобы было место для индикатора */
  margin-bottom: 24px !important;
}

.password-strength-container {
  position: absolute;
  bottom: -20px; /* Позиционируем точно между полями */
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 16px;
}

.password-strength-meter {
  display: flex;
  flex-grow: 1;
  gap: 4px;
  height: 6px;
}

.strength-section {
  flex: 1;
  height: 100%;
  border-radius: 3px;
  background-color: #e0e0e0;
  transition: background-color 0.3s ease;
}

.strength-label {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

/* Color classes */
.strength-section.weak, .strength-label.weak { color: #ff3b30; }
.strength-section.weak { background-color: #ff3b30; }

.strength-section.medium, .strength-label.medium { color: #ff9500; }
.strength-section.medium { background-color: #ff9500; }

.strength-section.strong, .strength-label.strong { color: #34c759; }
.strength-section.strong { background-color: #34c759; }

.form-group {
  position: relative;
  margin-bottom: 2px;
}

.error-message {
  color: #ff3b30;
  font-size: 13px;
  padding: 4px 0 0 4px;
  text-align: left;
  display: block;
  height: 18px;
}

.form-control {
  user-select: text;
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

.form-control.is-valid {
  border-color: #34c759;
}

.form-control.is-valid:focus {
  border-color: #34c759;
  box-shadow: 0 0 0 3px rgba(52, 199, 89, 0.2);
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
  padding: 12px;
}

.social-btn.vk svg {
  width: 100%;
  height: 100%;
}
</style>