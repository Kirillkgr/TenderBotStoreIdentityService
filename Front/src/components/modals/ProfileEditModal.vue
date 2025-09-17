<template>
  <Modal :is-modal-visible="true" @close="handleClose">
    <template #header>
      <h3>Редактирование профиля</h3>
    </template>
    <template #content>
      
      <Form @submit="onSubmit" :validation-schema="schema" class="edit-profile-form">
        <div class="form-group">
          <label for="lastName">Фамилия</label>
          <Field name="lastName" type="text" id="lastName" class="form-control" placeholder="Фамилия" />
          <ErrorMessage name="lastName" class="error-message" />
        </div>
        <div class="form-group">
          <label for="firstName">Имя</label>
          <Field name="firstName" type="text" id="firstName" class="form-control" placeholder="Имя" />
          <ErrorMessage name="firstName" class="error-message" />
        </div>
        <div class="form-group">
          <label for="patronymic">Отчество</label>
          <Field name="patronymic" type="text" id="patronymic" class="form-control" placeholder="Отчество" />
          <ErrorMessage name="patronymic" class="error-message" />
        </div>
        <DateOfBirthField v-model="values.dateOfBirth" />
        <div class="form-group email-group">
          <label for="email">Email</label>
          <div class="email-input-wrapper">
            <Field name="email" type="email" id="email" class="form-control" placeholder="Email" @input="handleEmailChange" :class="{'email-warning': emailNeedsVerification, 'email-verified': emailVerified}" />
            <span v-if="emailNeedsVerification && !showEmailCode" class="email-warning-icon" @click="sendEmailVerification">
              ⚠️
              <span class="email-tooltip">Подтвердите email</span>
            </span>
            <span v-if="emailVerified" class="email-verified-icon">✔️</span>
          </div>
          <ErrorMessage name="email" class="error-message" />
        </div>
        <div v-if="showEmailCode" class="form-group">
          <label for="emailCode">Код подтверждения Email</label>
          <div class="email-code-wrapper">
            <Field name="emailCode" type="text" id="emailCode" class="form-control" placeholder="Введите код из письма" maxlength="6" @input="onEmailCodeInput" />
            <span v-if="isVerifyingCode" class="spinner"></span>
          </div>
          <ErrorMessage name="emailCode" class="error-message" />
        </div>
        <div class="form-group">
          <label for="phone">Телефон</label>
          <Field name="phone" type="tel" id="phone" class="form-control" placeholder="+7 999 999-99-99" />
          <ErrorMessage name="phone" class="error-message" />
        </div>
        <button type="submit" class="submit-btn">Сохранить изменения</button>
      </Form>
    </template>
  </Modal>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { Form, Field, ErrorMessage, useForm } from 'vee-validate';
import * as yup from 'yup';
import Modal from '../Modal.vue';
import DateOfBirthField from '../fields/DateOfBirthField.vue';
import { useAuthStore } from '../../store/auth';
import { useToast } from 'vue-toastification';

const emit = defineEmits(['close', 'success']);
const authStore = useAuthStore();
const toast = useToast();

const LOCAL_STORAGE_KEY = 'profileEditDraft';
const user = authStore.user || {};

const schema = yup.object({
  lastName: yup.string()
    .required('Фамилия обязательна')
    .matches(/^[А-Яа-яA-Za-z\-\s]+$/, 'Только буквы, пробелы и тире')
    .min(3, 'Минимум 3 символа'),
  firstName: yup.string()
    .required('Имя обязательно')
    .matches(/^[А-Яа-яA-Za-z\-\s]+$/, 'Только буквы, пробелы и тире')
    .min(3, 'Минимум 3 символа'),
  patronymic: yup.string()
    .matches(/^[А-Яа-яA-Za-z\-\s]*$/, 'Только буквы, пробелы и тире'),
  dateOfBirth: yup.date()
    .typeError('Введите корректную дату')
    .max(new Date(), 'Дата должна быть в прошлом'),
  email: yup.string()
    .required('Email обязателен')
    .email('Некорректный email'),
  emailCode: yup.string()
    .when('email', {
      is: (val) => val && val !== user.email,
      then: yup.string().required('Введите код из письма'),
      otherwise: yup.string().notRequired()
    }),
  phone: yup.string()
    .required('Телефон обязателен')
    .matches(/^\+7\s?\d{3}\s?\d{3}-?\d{2}-?\d{2}$/, 'Формат: +7 999 999-99-99'),
});

const { handleSubmit, setValues, values, resetForm } = useForm({
  validationSchema: schema,
  initialValues: {
    lastName: user.lastName || '',
    firstName: user.firstName || '',
    patronymic: user.patronymic || '',
    dateOfBirth: user.dateOfBirth || '',
    email: user.email || '',
    emailCode: '',
    phone: user.phone || '',
  }
});

const showEmailCode = ref(false);
const emailNeedsVerification = ref(false);
const emailVerified = ref(false);
const isVerifyingCode = ref(false);

function handleEmailChange(e) {
  emailVerified.value = false;
  emailNeedsVerification.value = e.target.value && e.target.value !== user.email;
  // поле для кода не показываем до клика по иконке
  if (!emailNeedsVerification.value) showEmailCode.value = false;
}

async function sendEmailVerification() {
  // Отправить POST /v1/user/verifield/email
  try {
    const email = values.email;
    // TODO: заменить на userService
    await fetch('/v1/user/verifield/email', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });
    showEmailCode.value = true;
  } catch (e) {
    toast.error('Ошибка при отправке письма');
  }
}

async function onEmailCodeInput(e) {
  const code = e.target.value;
  values.emailCode = code;
  if (code.length === 6 && !isVerifyingCode.value) {
    isVerifyingCode.value = true;
    try {
      // TODO: заменить на userService
      const resp = await fetch('/v1/user/verifield/email', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: values.email, code })
      });
      if (resp.ok) {
        emailVerified.value = true;
        showEmailCode.value = false;
        toast.success('Email подтвержден!');
      } else {
        toast.error('Неверный код');
      }
    } catch (e) {
      toast.error('Ошибка при подтверждении');
    } finally {
      isVerifyingCode.value = false;
    }
  }
}

watch(values, (newVal) => {
  localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(newVal));
}, { deep: true });

onMounted(() => {
  // Подгружаем черновик, если есть
  const draft = localStorage.getItem(LOCAL_STORAGE_KEY);
  if (draft) {
    try {
      setValues(JSON.parse(draft));
      showEmailCode.value = values.email !== user.email;
    } catch {}
  }
});

function handleClose() {
  emit('close');
}

const onSubmit = handleSubmit(async (formData) => {
  try {
    // TODO: отправка emailCode если email менялся
    // await authStore.updateProfile(formData);
    localStorage.removeItem(LOCAL_STORAGE_KEY);
    toast.success('Профиль успешно обновлён!');
    emit('success');
    emit('close');
  } catch (error) {
    toast.error('Ошибка при обновлении профиля.');
    console.error(error);
  }
});
</script>

<style scoped>
.edit-profile-form {
  width: 100%;
  max-width: 320px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.form-control {
  padding: 14px;
  border-radius: 12px;
  border: 1px solid #e8e8e8;
  background: #fff;
  color: #000;
}
.form-control.email-warning {
  border-color: #ffd600;
}
.form-control.email-verified {
  border-color: #34c759;
}
.email-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.email-warning-icon {
  color: #ffd600;
  margin-left: 8px;
  cursor: pointer;
  position: relative;
}
.email-warning-icon:hover .email-tooltip {
  display: block;
}
.email-tooltip {
  display: none;
  position: absolute;
  left: 30px;
  top: -5px;
  background: #222;
  color: #ffd600;
  border: 1px solid #ffd600;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 0.85em;
  white-space: nowrap;
  z-index: 10;
}
.email-verified-icon {
  color: #34c759;
  margin-left: 8px;
}
.spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 3px solid #ccc;
  border-top: 3px solid #2980b9;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-left: 10px;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.email-code-wrapper {
  display: flex;
  align-items: center;
}
.error-message {
  color: #ff6b6b;
  font-size: 0.9em;
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
</style>
