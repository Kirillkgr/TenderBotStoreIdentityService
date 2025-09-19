<template>
  <Modal :is-modal-visible="true" @close="handleClose">
    <template #header>
      <h3>Редактирование профиля</h3>
    </template>
    <template #content>
      
      <Form @submit.prevent="onSubmit" :validation-schema="schema" class="edit-profile-form">
        <div class="form-group">
          <label for="lastName">Фамилия</label>
          <Field name="lastName" type="text" id="lastName" class="form-control" :class="{ invalid: !!errors.lastName }" placeholder="Фамилия" />
          <ErrorMessage name="lastName" class="error-message" />
        </div>
        <div class="form-group">
          <label for="firstName">Имя</label>
          <Field name="firstName" type="text" id="firstName" class="form-control" :class="{ invalid: !!errors.firstName }" placeholder="Имя" />
          <ErrorMessage name="firstName" class="error-message" />
        </div>
        <div class="form-group">
          <label for="patronymic">Отчество</label>
          <Field name="patronymic" type="text" id="patronymic" class="form-control" :class="{ invalid: !!errors.patronymic }" placeholder="Отчество" />
          <ErrorMessage name="patronymic" class="error-message" />
        </div>
        <DateOfBirthField v-model="dobModel" />
        <div class="form-group email-group">
          <label for="email">Email</label>
          <div class="email-input-wrapper">
            <Field name="email" type="email" id="email" class="form-control" :class="{ invalid: !!errors.email, 'email-warning': emailNeedsVerification, 'email-verified': emailVerified }" placeholder="Email" @input="handleEmailChange" />
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
            <Field name="emailCode" type="text" id="emailCode" class="form-control" :class="{ invalid: !!errors.emailCode }" placeholder="Введите код из письма" maxlength="6" @input="onEmailCodeInput" />
            <span v-if="isVerifyingCode" class="spinner"></span>
          </div>
          <ErrorMessage name="emailCode" class="error-message" />
        </div>
        <div class="form-group">
          <label for="phone">Телефон</label>
          <Field name="phone" type="tel" id="phone" class="form-control" :class="{ invalid: !!errors.phone }" placeholder="+7 999 999-99-99" />
          <ErrorMessage name="phone" class="error-message" />
        </div>
        <button id="save-profile-btn" type="button" class="submit-btn" style="z-index: 1"
                @click.stop="onClickSubmit"
                @mousedown.stop.prevent="onClickSubmit"
                @pointerdown.stop.prevent="onClickSubmit"
                @touchend.stop.prevent="onClickSubmit"
                @keyup.enter.stop="onClickSubmit"
                @keydown.enter.stop.prevent
                :disabled="isSaving">
          Сохранить изменения
        </button>
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
import * as userService from '../../services/userService';

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
  // Дата рождения не блокирует сабмит (опционально, без проверки «прошлого»)
  dateOfBirth: yup.mixed().nullable(),
  email: yup.string()
    .required('Email обязателен')
    .email('Некорректный email'),
  // Подтверждение email НЕ обязательно для сохранения анкеты
  emailCode: yup.string().notRequired(),
  phone: yup.string()
    .required('Телефон обязателен')
    // Принимаем +79999999999, 8XXXXXXXXXX, пробелы/дефисы — нормализуем при сабмите
    .matches(/^\+?\d[\d\s\-()]{9,14}$/, 'Введите корректный телефон'),
});

const { handleSubmit, submitForm, setValues, setFieldValue, values, resetForm, errors, validate } = useForm({
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

const isSaving = ref(false);

async function onClickSubmit(evt) {
  evt?.preventDefault?.();
  console.log('[ProfileEdit] force submit (no validation)');
  await directSave();
}

async function directSave() {
  try {
    if (isSaving.value) return; // guard
    isSaving.value = true;
    const snapshot = JSON.parse(JSON.stringify(values));
    // Дата: приоритет локальной модели
    if (dobModel.value) snapshot.dateOfBirth = dobModel.value;
    // Fallback к DOM-значениям, если автозаполнение не попало в vee-validate
    const dom = (id) => (document.getElementById(id)?.value ?? '').trim();
    snapshot.lastName = (snapshot.lastName || dom('lastName'))?.trim?.() || '';
    snapshot.firstName = (snapshot.firstName || dom('firstName'))?.trim?.() || '';
    snapshot.patronymic = (snapshot.patronymic || dom('patronymic'))?.trim?.() || '';
    snapshot.email = (snapshot.email || dom('email'))?.trim?.() || '';
    snapshot.phone = (snapshot.phone || dom('phone'))?.trim?.() || '';
    // Если поле кода скрыто — явно очищаем emailCode
    if (!showEmailCode.value) snapshot.emailCode = '';
    // Нормализуем телефон
    if (typeof snapshot.phone === 'string') {
      const digits = snapshot.phone.replace(/\D/g, '');
      if (digits.startsWith('8') && digits.length === 11) snapshot.phone = '+7' + digits.slice(1);
      else if (digits.startsWith('7') && digits.length === 11) snapshot.phone = '+7' + digits.slice(1);
      else if (digits.length === 10) snapshot.phone = '+7' + digits;
      else if (!snapshot.phone.startsWith('+') && digits.length >= 11) snapshot.phone = '+' + digits;
    }
    console.log('[ProfileEdit] direct save payload', snapshot);
    const { data: updated } = await userService.editProfile(snapshot);
    authStore.setUser(updated);
    localStorage.removeItem(LOCAL_STORAGE_KEY);
    toast.success('Профиль успешно обновлён!');
    emit('success');
    emit('close');
  } catch (error) {
    console.error('directSave error', error);
    toast.error(error?.response?.data?.message || 'Ошибка при обновлении профиля.');
  } finally {
    isSaving.value = false;
  }
}

// Локальная модель для даты рождения, чтобы не мутировать values напрямую
const dobModel = ref(user.dateOfBirth || '');
const showEmailCode = ref(false);
const emailNeedsVerification = ref(false);
const emailVerified = ref(false);
const isVerifyingCode = ref(false);

function handleEmailChange(e) {
  emailVerified.value = false;
  // Через API vee-validate, не мутируем values напрямую
  setFieldValue('email', e.target.value);
  emailNeedsVerification.value = e.target.value && e.target.value !== user.email;
  // поле для кода не показываем до клика по иконке
  if (!emailNeedsVerification.value) showEmailCode.value = false;
}

async function sendEmailVerification() {
  try {
    // Берём email из формы, или из профиля, или напрямую из DOM, затем trim
    let email = (values.email || user.email || '').trim();
    if (!email) {
      const el = document.getElementById('email');
      if (el && el.value) email = el.value.trim();
    }
    if (!email) {
      toast.error('Укажите email');
      return;
    }
    await userService.requestEmailVerification(email);
    showEmailCode.value = true;
  } catch (e) {
    toast.error('Ошибка при отправке письма');
  }
}

async function onEmailCodeInput(e) {
  const code = e.target.value;
  setFieldValue('emailCode', code);
  if (code.length === 6 && !isVerifyingCode.value) {
    isVerifyingCode.value = true;
    try {
      let email = (values.email || user.email || '').trim();
      if (!email) {
        const el = document.getElementById('email');
        if (el && el.value) email = el.value.trim();
      }
      if (!email) {
        toast.error('Укажите email');
        isVerifyingCode.value = false;
        return;
      }
      await userService.verifyEmailCode(email, code);
      emailVerified.value = true;
      showEmailCode.value = false;
      toast.success('Email подтвержден!');
    } catch (e) {
      toast.error('Неверный код или ошибка подтверждения');
    } finally {
      isVerifyingCode.value = false;
    }
  }
}

// Синхроним локальную дату рождения в форму vee-validate
watch(dobModel, (v) => {
  setFieldValue('dateOfBirth', v);
});

watch(values, (newVal) => {
  localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(newVal));
}, { deep: true });

let listenersBound = false;
onMounted(() => {
  // Fallback: если по какой-то причине клики не доходят, подцепимся к кнопке напрямую
  try {
    const btn = document.getElementById('save-profile-btn');
    if (btn && !listenersBound) {
      ['click','mousedown','pointerdown','touchend','keyup'].forEach(ev => {
        btn.addEventListener(ev, (e) => {
          if (ev === 'keyup' && e.key !== 'Enter') return;
          e.stopPropagation?.();
          e.preventDefault?.();
          onClickSubmit(e);
        }, { passive: false });
      });
      listenersBound = true;
    }
  } catch (e) { console.warn('bind btn handlers failed', e); }

  // Подгружаем черновик, если есть
  const draft = localStorage.getItem(LOCAL_STORAGE_KEY);
  if (draft) {
    try {
      setValues(JSON.parse(draft));
      // Поле кода не показываем автоматически
      showEmailCode.value = false;
    } catch {}
  }
  // Проверка статуса подтверждения email при открытии модалки
  (async () => {
    try {
      const emailToCheck = values.email || user.email;
      if (!emailToCheck) return;
      const resp = await userService.checkEmailVerified(emailToCheck);
      const verified = typeof resp.data === 'boolean' ? resp.data : !!resp.data?.verified;
      emailVerified.value = verified;
      emailNeedsVerification.value = !verified && (!!values.email || !!user.email);
      // Не открываем поле и не отправляем код автоматически. Пользователь сам решает, когда подтвердить.
    } catch (_) { /* ignore network errors here */ }
  })();
});

function handleClose() {
  emit('close');
}

const onSubmit = handleSubmit(async (formData) => {
  try {
    console.log('[ProfileEdit] submit start', formData);
    toast.clear();
    toast.info('Сохраняю профиль...', { timeout: 1500 });
    // Нормализация даты: если пришёл объект Date — превратим в yyyy-MM-dd
    if (formData.dateOfBirth instanceof Date) {
      const y = formData.dateOfBirth.getFullYear();
      const m = String(formData.dateOfBirth.getMonth() + 1).padStart(2, '0');
      const d = String(formData.dateOfBirth.getDate()).padStart(2, '0');
      formData.dateOfBirth = `${y}-${m}-${d}`;
    }

    // Разрешаем сохранять без подтверждения email. Подтверждение — отдельный поток.

    // Нормализуем телефон: только цифры, приводим к +7...
    if (typeof formData.phone === 'string') {
      const digits = formData.phone.replace(/\D/g, '');
      if (digits.startsWith('8') && digits.length === 11) {
        formData.phone = '+7' + digits.slice(1);
      } else if (digits.startsWith('7') && digits.length === 11) {
        formData.phone = '+7' + digits.slice(1);
      } else if (digits.length === 10) {
        formData.phone = '+7' + digits;
      } else if (!formData.phone.startsWith('+') && digits.length >= 11) {
        formData.phone = '+' + digits;
      }
    }

    const { data: updated } = await userService.editProfile(formData);
    console.log('[ProfileEdit] server response', updated);

    // Обновляем стор и локальное хранилище
    authStore.setUser(updated);

    localStorage.removeItem(LOCAL_STORAGE_KEY);
    toast.success('Профиль успешно обновлён!');
    emit('success');
    emit('close');
  } catch (error) {
    toast.error(error?.response?.data?.message || 'Ошибка при обновлении профиля.');
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
.form-control.invalid {
  border-color: #ff6b6b;
  box-shadow: 0 0 0 1px rgba(255,107,107,.3);
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
  background: #0a84ff;
  color: white;
  font-weight: 600;
  border: none;
  cursor: pointer;
  pointer-events: auto;
  position: relative;
  z-index: 1000; /* на случай перекрытий */
}
.submit-btn:hover:not(:disabled) {
  background-color: #005ecb;
  transform: translateY(-1px);
}
</style>
