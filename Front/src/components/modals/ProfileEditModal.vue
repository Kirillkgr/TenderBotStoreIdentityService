<template>
  <Modal :is-modal-visible="true" :width="757" @close="handleClose">
    <template #header>
      <h3>Редактирование профиля</h3>
    </template>
    <template #content>
      <div class="dl-layout">
        <!-- Колонка 1: Аватар + мета -->
        <div class="dl-col avatar-col">
          <div class="avatar-card">
            <div class="avatar-wrap">
              <img
                  :src="avatarPreview || authStore.user?.avatarUrl || placeholderAvatar"
                  alt="avatar"
                  class="avatar-img"
              />
            </div>
            <input id="avatarFile" ref="fileInput" accept="image/*" name="avatar" style="display:none" type="file"
                   @change="onAvatarSelected"/>
            <button class="btn btn-primary w-100 mt-2" type="button" @click="triggerFile">
              <i class="bi bi-upload me-1"></i>
              Загрузить фото
              <span v-if="avatarPreview" class="ok-dot" title="Выбран файл"></span>
            </button>
            <div class="avatar-meta">
              <div class="meta-row"><span class="label">Создан:</span><span
                  class="val">{{ formatDate(authStore.user?.createdAt) }}</span></div>
              <div class="meta-row"><span class="label">Обновлён:</span><span
                  class="val">{{ formatDate(authStore.user?.updatedAt) }}</span></div>
            </div>
          </div>
        </div>

        <!-- Колонки 2 и 3: единая форма-сетка 2x4 -->
        <!--noinspection JSUnresolvedVariable,JSUnresolvedReference -->
        <!--suppress JSUnresolvedReference -->
        <Form :key="formKey" :initial-values="prefillValues" :validation-schema="schema" class="edit-profile-form-grid"
              @submit.prevent="onSubmit">
          <!-- Ряд 1 -->
          <div class="form-item" style="grid-column: 1; grid-row: 1;">
            <label class="sr-only" for="lastName">Фамилия</label>
            <Field id="lastName" :class="{ invalid: !!errors.lastName }" class="form-control" name="lastName"
                   aria-label="Фамилия" autocomplete="family-name" placeholder="Фамилия" type="text"/>
            <ErrorMessage class="error-message" name="lastName"/>
          </div>
          <div class="form-item" style="grid-column: 2; grid-row: 1;">
            <div class="inline-controls">
              <label class="sr-only" for="email">Email</label>
              <Field id="email" :class="{ invalid: !!errors.email, 'email-warning': emailNeedsVerification && !emailVerifiedEffective, 'email-verified': emailVerifiedEffective }" class="form-control flex-1" name="email"
                     aria-label="Email" autocomplete="email" placeholder="Email"
                     type="email" @input="handleEmailChange"/>
              <span v-if="emailVerifiedEffective" class="email-verified-icon">✔️</span>
              <button v-if="!emailVerifiedEffective && !showEmailCode" class="btn btn-sm btn-outline-primary"
                      title="Подтвердить email" type="button" @click="sendEmailVerification">
                Подтв.
              </button>
              <label v-if="showEmailCode && !emailVerifiedEffective" class="sr-only" for="emailCode">Код
                подтверждения</label>
              <Field v-if="showEmailCode && !emailVerifiedEffective" id="emailCode" class="form-control code-input" maxlength="6"
                     aria-label="Код подтверждения" autocomplete="one-time-code" inputmode="numeric" name="emailCode"
                     placeholder="Код"
                     type="text" @input="onEmailCodeInput"/>
              <span v-if="isVerifyingCode" class="spinner"></span>
            </div>
            <ErrorMessage class="error-message" name="email"/>
          </div>

          <!-- Ряд 2 -->
          <div class="form-item" style="grid-column: 1; grid-row: 2;">
            <label class="sr-only" for="firstName">Имя</label>
            <Field id="firstName" :class="{ invalid: !!errors.firstName }" class="form-control" name="firstName"
                   aria-label="Имя" autocomplete="given-name" placeholder="Имя" type="text"/>
            <ErrorMessage class="error-message" name="firstName"/>
          </div>
          <div class="form-item" style="grid-column: 2; grid-row: 2;">
            <label class="sr-only" for="username">Логин</label>
            <input id="username" :placeholder="'Логин'" :value="authStore.user?.username || ''" autocomplete="username"
                   class="form-control" disabled
                   name="username" type="text"/>
          </div>

          <!-- Ряд 3 -->
          <div class="form-item" style="grid-column: 1; grid-row: 3;">
            <label class="sr-only" for="patronymic">Отчество</label>
            <Field id="patronymic" :class="{ invalid: !!errors.patronymic }" class="form-control" name="patronymic"
                   aria-label="Отчество" autocomplete="additional-name" placeholder="Отчество" type="text"/>
            <ErrorMessage class="error-message" name="patronymic"/>
          </div>
          <div class="form-item" style="grid-column: 2; grid-row: 3;">
            <label class="sr-only" for="phone">Телефон</label>
            <Field id="phone" :class="{ invalid: !!errors.phone }" class="form-control" name="phone" placeholder="Телефон"
                   aria-label="Телефон" autocomplete="tel" inputmode="tel" type="tel"/>
            <ErrorMessage class="error-message" name="phone"/>
          </div>

          <!-- Ряд 4 -->
          <div class="form-item" style="grid-column: 1; grid-row: 4;">
            <div class="control">
              <label class="sr-only" for="dateOfBirth">Дата рождения</label>
              <DateOfBirthField id="dateOfBirth" v-model="dobModel" aria-label="Дата рождения" class="w-100"/>
            </div>
          </div>
          <div class="form-item" style="grid-column: 2; grid-row: 4;">
            <label class="sr-only" for="roles">Роли</label>
            <input id="roles" :placeholder="'Роли'" :value="(authStore.user?.roles || []).join(', ')" autocomplete="off"
                   class="form-control"
                   disabled name="roles" type="text"/>
          </div>
        </Form>
      </div>
      <div class="footer-actions">
        <button id="save-profile-btn" class="submit-btn" type="button"
                @click.stop="onClickSubmit"
                @mousedown.stop.prevent="onClickSubmit"
                @pointerdown.stop.prevent="onClickSubmit"
                @touchend.stop.prevent="onClickSubmit"
                @keyup.enter.stop="onClickSubmit"
                @keydown.enter.stop.prevent
                :disabled="isSaving">
          Сохранить изменения
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup>
import {computed, nextTick, onMounted, ref, watch} from 'vue';
import {ErrorMessage, Field, Form, useForm} from 'vee-validate';
import * as yup from 'yup';
import Modal from '@/components/Modal.vue';
import DateOfBirthField from '@/components/fields/DateOfBirthField.vue';
import {useAuthStore} from '@/store/auth';
import {useToast} from 'vue-toastification';
import {formatLocalDateTime} from '@/utils/datetime';
import * as userService from '@/services/userService';
import userIcon from '@/assets/user.svg';

const emit = defineEmits(['close', 'success']);
const authStore = useAuthStore();
const formKey = ref(0);
const prefillValues = ref({
  lastName: '',
  firstName: '',
  patronymic: '',
  dateOfBirth: '',
  email: '',
  emailCode: '',
  phone: ''
});
const fileInput = ref(null);
const placeholderAvatar = userIcon;
const avatarPreview = ref('');
const selectedAvatarFile = ref(null);
const toast = useToast();

const LOCAL_STORAGE_KEY = 'profileEditDraft';
const user = authStore.user || {};

function formatDate(val) {
  return formatLocalDateTime(val);
}

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

// watcher будет добавлен ниже, после объявления useForm

// noinspection JSUnresolvedReference
const {handleSubmit, setFieldValue, values, resetForm, errors} = useForm({
  // noinspection JSUnresolvedVariable
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
// noinspection JSUnresolvedReference
const isExternalProvider = computed(() => !!(authStore.user?.oauthProvider || authStore.user?.provider || authStore.user?.external));
const emailVerifiedEffective = computed(() => !!(emailVerified.value || authStore.user?.emailVerified || isExternalProvider.value));

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
    // 1) Сначала сохраняем профиль
    const { data: updated } = await userService.editProfile(snapshot);
    authStore.setUser(updated);
    // 2) Затем, если выбран новый файл аватара — загрузим и обновим профиль
    if (selectedAvatarFile.value) {
      try {
        const {data} = await userService.uploadAvatar(selectedAvatarFile.value);
        if (data?.avatarUrl) {
          authStore.setUser({...(authStore.user || {}), avatarUrl: data.avatarUrl});
        } else if (data && typeof data === 'object') {
          authStore.setUser({...(authStore.user || {}), ...data});
        }
      } catch (e) {
        // Не проваливаем весь сабмит из-за ошибки загрузки аватара
        console.warn('Avatar upload failed', e);
      }
      // Очистим превью после попытки загрузки
      selectedAvatarFile.value = null;
      avatarPreview.value = '';
    }
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
onMounted(async () => {
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
      const parsed = JSON.parse(draft) || {};
      // По требованию: основной источник — user_data из localStorage
      let u = {};
      try {
        const rawUser = localStorage.getItem('user_data');
        if (rawUser) u = JSON.parse(rawUser) || {};
      } catch {
      }
      // если user_data нет — fallback к authStore.user
      if (!u || Object.keys(u).length === 0) {
        u = authStore.user || {};
      }
      // Сливаем черновик с профилем: пустые поля берём из user
      const pick = (v, fallback) => (v === undefined || v === null || v === '' ? fallback : v);
      const merged = {
        lastName: pick(parsed.lastName, u.lastName || ''),
        firstName: pick(parsed.firstName, u.firstName || ''),
        patronymic: pick(parsed.patronymic, u.patronymic || ''),
        dateOfBirth: pick(parsed.dateOfBirth, u.dateOfBirth || ''),
        email: pick(parsed.email, u.email || ''),
        emailCode: '',
        phone: pick(parsed.phone, u.phone || ''),
      };
      prefillValues.value = merged;
      formKey.value++;
      await nextTick();
      // noinspection JSUnresolvedReference
      resetForm({values: merged, keepDirty: false});
      // Форсируем проставление значений в Field, если по какой-то причине initial-values не подхватились
      // noinspection JSUnresolvedReference
      setFieldValue('lastName', merged.lastName, false);
      // noinspection JSUnresolvedReference
      setFieldValue('firstName', merged.firstName, false);
      // noinspection JSUnresolvedReference
      setFieldValue('patronymic', merged.patronymic, false);
      // noinspection JSUnresolvedReference
      setFieldValue('email', merged.email, false);
      // noinspection JSUnresolvedReference
      setFieldValue('phone', merged.phone, false);
      // noinspection JSUnresolvedReference
      setFieldValue('dateOfBirth', merged.dateOfBirth, false);
      console.debug('[ProfileEdit] prefill from draft+user_data', merged);
      // Поле кода не показываем автоматически
      showEmailCode.value = false;
    } catch {}
  } else {
    // Если черновика нет — заполняем из authStore.user (локальное хранилище уже могло гидратиться)
    let u = authStore.user || {};
    // Фоллбек: берём профиль напрямую из localStorage, если в store его ещё нет
    try {
      if (!u || Object.keys(u).length === 0) {
        const rawUser = localStorage.getItem('user_data');
        if (rawUser) u = JSON.parse(rawUser) || {};
      }
    } catch {
    }
    if (u && Object.keys(u).length > 0) {
      await nextTick();
      const vals = {
        lastName: u.lastName || '',
        firstName: u.firstName || '',
        patronymic: u.patronymic || '',
        dateOfBirth: u.dateOfBirth || '',
        email: u.email || '',
        emailCode: '',
        phone: u.phone || '',
      };
      prefillValues.value = vals;
      formKey.value++;
      // noinspection JSUnresolvedReference
      resetForm({values: vals, keepDirty: false});
      // Форсируем проставление значений
      // noinspection JSUnresolvedReference
      setFieldValue('lastName', vals.lastName, false);
      // noinspection JSUnresolvedReference
      setFieldValue('firstName', vals.firstName, false);
      // noinspection JSUnresolvedReference
      setFieldValue('patronymic', vals.patronymic, false);
      // noinspection JSUnresolvedReference
      setFieldValue('email', vals.email, false);
      // noinspection JSUnresolvedReference
      setFieldValue('phone', vals.phone, false);
      // noinspection JSUnresolvedReference
      setFieldValue('dateOfBirth', vals.dateOfBirth, false);
      console.debug('[ProfileEdit] prefill from user_data/authStore', vals);
      dobModel.value = u.dateOfBirth || '';
    }
  }
  // Проверка статуса подтверждения email при открытии модалки
  // Fire-and-forget check; 'void' hints to IDE/linter that awaiting is intentionaly omitted
  void (async () => {
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

function triggerFile() {
  fileInput.value?.click();
}

function onAvatarSelected(evt) {
  const file = evt?.target?.files?.[0];
  if (!file) return;
  selectedAvatarFile.value = file;
  try {
    avatarPreview.value = URL.createObjectURL(file);
  } catch (_) {
  }
}

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
/* Визуально скрытая подпись для соответствия label[for] и id инпутов (доступность и автозаполнение) */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* Контейнер модалки под ширину карточки профиля */
.dl-layout {
  display: grid;
  grid-template-columns: 200px 1fr 1fr;
  gap: 12px; /* стало меньше, чтобы уместиться в 757px вместе с внутренними паддингами */
  align-items: start;
  max-width: 757px;
  text-align: left;
}

.avatar-col {
  min-width: 200px;
}

.avatar-card {
  background: var(--card);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 12px;
  text-align: center;
}

.avatar-wrap {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  margin: 0 auto;
  overflow: hidden;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-meta {
  margin-top: 10px;
  text-align: left;
  font-size: 12px;
  color: var(--muted);
}

.avatar-meta .meta-row {
  display: flex;
  gap: 6px;
}

.avatar-meta .label {
  min-width: 70px;
  color: var(--muted);
}

.ok-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #22c55e;
  border-radius: 50%;
  margin-left: 6px;
  vertical-align: middle;
}

/* Новая форма-сетка 2x4 для симметрии полей */
.edit-profile-form-grid {
  grid-column: 2 / 4; /* занимать обе правые колонки */
  display: grid;
  grid-template-columns: 240px 240px; /* фиксированные равные колонки */
  justify-content: space-between; /* равномерно распределить */
  column-gap: 16px; /* меньше, чтобы вписаться */
  row-gap: 10px;
  align-items: start;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.inline-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-controls .form-control {
  height: 32px;
}

.inline-controls .btn {
  height: 32px;
  line-height: 1;
  white-space: nowrap;
  padding: 0 8px;
  font-size: 12px;
}

.inline-controls .code-input {
  height: 32px;
}

.code-input {
  width: 84px;
  text-align: center;
}

.flex-1 {
  flex: 1 1 auto;
}

.w-100 {
  width: 100%;
}

.form-control {
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid var(--input-border, var(--card-border, #3a3a3a));
  background: var(--input-bg, #2b2b2b);
  color: var(--text, #ffffff);
  font-size: 12px;
  height: 32px;
}

.form-control::placeholder {
  color: var(--muted, #bdbdbd);
  opacity: 1;
}

.form-control:focus {
  outline: none;
  border-color: var(--primary, #0a84ff);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--primary, #0a84ff) 25%, transparent);
}

.form-control.invalid {
  border-color: #ff6b6b;
  box-shadow: 0 0 0 1px rgba(255, 107, 107, .3);
}

.form-control.email-warning {
  border-color: #ffd600;
}

.form-control.email-verified {
  border-color: #34c759;
}

.email-verified-icon {
  color: #34c759;
  margin-left: 8px;
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #ccc;
  border-top: 2px solid #2980b9;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-left: 8px;
}

/* Кнопка сохранения внизу по ширине контента */
.footer-actions {
  margin-top: 12px;
  max-width: 900px;
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
  z-index: 1000;
}

.submit-btn:hover:not(:disabled) {
  background-color: #005ecb;
  transform: translateY(-1px);
}

/* Мобильная версия: одна колонка, аватар сверху */
@media (max-width: 768px) {
  .dl-layout {
    grid-template-columns: 1fr;
    max-width: 92vw;
  }

  .footer-actions {
    max-width: 92vw;
  }

  .avatar-wrap {
    width: 96px;
    height: 96px;
  }

  .edit-profile-form-grid {
    grid-template-columns: 1fr;
    justify-content: stretch;
  }
}

/* Пытаемся стилизовать возможный внутренний input компонента даты под тему */
.control :where(input, .input) {
  background: var(--input-bg, #2b2b2b);
  color: var(--text, #ffffff);
  border: 1px solid var(--input-border, var(--card-border, #3a3a3a));
}

.control :where(input, .input)::placeholder {
  color: var(--muted, #bdbdbd);
}
</style>
