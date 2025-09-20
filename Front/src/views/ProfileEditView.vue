<template>
  <ModalBase v-model="open" title="Редактирование профиля" :width="860" @update:modelValue="onModalToggle">
    <form class="form" @submit.prevent="onSubmit">
      <div class="grid">
        <label>
          <input v-model.trim="form.lastName" placeholder="Фамилия" required type="text"/>
        </label>
        <label>
          <input v-model.trim="form.firstName" placeholder="Имя" required type="text"/>
        </label>
        <label>
          <input v-model.trim="form.patronymic" placeholder="Отчество" type="text"/>
        </label>
        <label class="clickable" @click="openBirthPicker">
          <input ref="birthInput" v-model="form.birthDate" placeholder="Дата рождения дд.мм.гггг" type="date"
                 @blur="validateBirthDate" @change="validateBirthDate"/>
          <small v-if="errors.birthDate" class="error">{{ errors.birthDate }}</small>
        </label>
        <label>
          <input v-model.trim="form.email" placeholder="email@email.ru" required type="email" @blur="validateEmail"/>
          <small v-if="errors.email" class="error">{{ errors.email }}</small>

          <div v-if="emailChanged && !showEmailVerify" class="email-verify">
            <button class="btn full" type="button" @click="startEmailVerify" :disabled="sending">
              Подтвердить email
            </button>
          </div>

          <div v-else-if="emailChanged && showEmailVerify" class="inline">
            <input v-model="emailCode" placeholder="Код" class="code-input" />
            <button class="btn" type="button" @click="verifyEmail" :disabled="verifying || !emailCode">Подтвердить</button>
          </div>
        </label>
        <label>
          <input v-model.trim="form.phone" placeholder="+7 (123) 456-78-90" required type="tel" @blur="validatePhone"/>
          <small v-if="errors.phone" class="error">{{ errors.phone }}</small>
        </label>
        <label>
          <input v-model="form.password" autocomplete="new-password" placeholder="Пароль (оставьте пустым, если не меняете)"
                 type="password"/>
        </label>
        <label>
          <input v-model="form.confirmPassword" autocomplete="new-password" placeholder="Подтверждение пароля"
                 type="password" @blur="validatePasswords"/>
          <small v-if="errors.password" class="error">{{ errors.password }}</small>
        </label>
      </div>

      <div class="actions">
        <button type="submit" class="btn" :disabled="!isDirty || !isValid">Сохранить изменения</button>
      </div>
    </form>
  </ModalBase>
</template>

<script setup>
import {computed, nextTick, reactive, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {useAuthStore} from '@/store/auth';
import {UserApi} from '@/services/user';
import ModalBase from '@/components/ui/ModalBase.vue';
import {useToast} from 'vue-toastification';

const auth = useAuthStore();
const router = useRouter();
const open = ref(true);
function onModalToggle(v) { if (!v) router.back(); }

const toast = useToast();

// Нормализатор даты к формату YYYY-MM-DD для input[type=date]
function normalizeDate(val) {
  if (!val) return '';
  try {
    return String(val).slice(0, 10);
  } catch (_) {
    return '';
  }
}

const initial = {
  lastName: auth.user?.lastName || '',
  firstName: auth.user?.firstName || '',
  patronymic: auth.user?.patronymic || '',
  birthDate: normalizeDate(auth.user?.dateOfBirth) || '',
  email: auth.user?.email || '',
  phone: auth.user?.phone || '',
};

const form = reactive({
  ...initial,
  password: '',
  confirmPassword: '',
});

const errors = reactive({ birthDate: '', email: '', phone: '', password: '' });
const birthInput = ref(null);
const hasFilledOnce = ref(false);
function openBirthPicker(e){
  // Открыть нативный календарь по клику на всю область поля
  if (e?.target && e.target.tagName.toLowerCase() === 'input') return; // уже по инпуту
  if (birthInput.value) {
    birthInput.value.focus();
    // showPicker поддерживается не везде
    if (typeof birthInput.value.showPicker === 'function') {
      try { birthInput.value.showPicker(); } catch(_) {}
    }
  }
}

// Подтянуть данные из auth.user после гидратации/восстановления
function fillFromAuth(u) {
  if (!u) return;
  const next = {
    lastName: u.lastName || '',
    firstName: u.firstName || '',
    patronymic: u.patronymic || '',
    birthDate: normalizeDate(u.dateOfBirth) || '',
    email: u.email || '',
    phone: u.phone || '',
  };
  // Обновляем initial, чтобы корректно работала проверка isDirty
  initial.lastName = next.lastName;
  initial.firstName = next.firstName;
  initial.patronymic = next.patronymic;
  initial.birthDate = next.birthDate;
  initial.email = next.email;
  initial.phone = next.phone;

  // Заполняем форму один раз при первом появлении user, чтобы не перетирать ручные изменения
  if (!hasFilledOnce.value) {
    form.lastName = next.lastName;
    form.firstName = next.firstName;
    form.patronymic = next.patronymic;
    form.birthDate = next.birthDate;
    form.email = next.email;
    form.phone = next.phone;
    hasFilledOnce.value = true;
  }
}

// Сразу попробовать заполнить (если user уже есть), и следить за изменениями
watch(() => auth.user, (u) => {
  fillFromAuth(u);
}, {immediate: true});

function validateBirthDate() {
  errors.birthDate = '';
  if (!form.birthDate) {
    // Дата необязательная — ошибок нет, если пусто
    return;
  }
  // type="date" даёт ISO (YYYY-MM-DD). Проверим корректность
  const d = new Date(form.birthDate);
  if (Number.isNaN(d.getTime())) {
    errors.birthDate = 'Некорректная дата';
  }
}

function validateEmail() {
  errors.email = '';
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!re.test(form.email)) {
    errors.email = 'Некорректный email';
  }
}

function validatePhone() {
  errors.phone = '';
  if (!form.phone) {
    // Дата необязательная — ошибок нет, если пусто
    return;
  }

  const re = /^(?:\+?(?:7|375|380|996|992|993|994|998|995|90|91|92|93|94|95|960|961|962|963|964|965|966|967|968|970|971|972|973|974|975|976|977|81|82|84|86)|8)\s?\d{2,4}[\s-]?\d{2,4}[\s-]?\d{2,4}$/;
  if (!re.test(form.phone)) {
    errors.phone = 'Формат: +7 999 999-99-99';
  }
}

function validatePasswords() {
  errors.password = '';
  if (form.password || form.confirmPassword) {
    if (form.password !== form.confirmPassword) {
      errors.password = 'Пароли не совпадают';
    } else if (form.password.length > 0 && form.password.length < 8) {
      errors.password = 'Пароль должен быть не менее 8 символов';
    }
  }
}

const isDirty = computed(() => {
  return (
    form.lastName !== initial.lastName ||
    form.firstName !== initial.firstName ||
    form.patronymic !== initial.patronymic ||
    form.birthDate !== initial.birthDate ||
    form.email !== initial.email ||
    form.phone !== initial.phone ||
    form.password.length > 0
  );
});

const isValid = computed(() => {
  return (
    !errors.birthDate &&
    !errors.email &&
    !errors.phone &&
    !errors.password &&
    form.lastName &&
    form.firstName &&
    form.email &&
    form.phone &&
    (!form.password || (form.password && form.password === form.confirmPassword))
  );
});

const saving = ref(false);
const emailCode = ref('');
const sending = ref(false);
const verifying = ref(false);
const showEmailVerify = ref(false);
const emailChanged = computed(() => (form.email || '') !== (initial.email || ''));

async function onSubmit() {
  validateBirthDate();
  validateEmail();
  validatePhone();
  validatePasswords();
  if (!isValid.value) return;
  const payload = {
    lastName: form.lastName,
    firstName: form.firstName,
    patronymic: form.patronymic || null,
    // дату отправляем только если заполнена
    email: form.email,
    phone: form.phone,
  };
  if (form.birthDate) payload.dateOfBirth = form.birthDate;
  try {
    saving.value = true;
    const updated = await UserApi.editProfile(payload);
    // обновим auth store локально
    auth.setUser({ ...(auth.user || {}), ...updated });
    toast.success('Профиль обновлён');
  } finally {
    saving.value = false;
  }
}

async function startEmailVerify() {
  sending.value = true;
  try {
    await UserApi.requestEmailCode(form.email);
    showEmailVerify.value = true;
    await nextTick();
  } finally {
    sending.value = false;
  }
}

async function verifyEmail() {
  if (!emailCode.value) return;
  verifying.value = true;
  try {
    const res = await UserApi.verifyEmailCode(form.email, emailCode.value.trim());
    if (res?.verified) {
      alert('Email подтвержден');
    }
  } finally {
    verifying.value = false;
  }
}
</script>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1rem; }
label { display: flex; flex-direction: column; gap: 6px; font-size: 14px; }
input { padding: 8px 10px; border-radius: 8px; border: 1px solid #3b3b3b; background: #1e1e1e; color: #fff; }
.actions { margin-top: 1rem; }
.btn { background: #3498db; border: none; padding: 10px 16px; border-radius: 8px; color: #fff; cursor: pointer; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.error { color: #ff7675; }
.inline { display: flex; gap: .5rem; align-items: center; margin-top: .35rem; }
.clickable { cursor: pointer; }
.email-verify { margin-top: .5rem; }
.btn.full { width: 100%; display: inline-flex; justify-content: center; }
.code-input { max-width: 200px; }
</style>
