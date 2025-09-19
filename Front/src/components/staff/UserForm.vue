<template>
  <form class="form" @submit.prevent="onSubmit">
    <div class="grid-2">
      <!-- Левая колонка: ФИО, дата -->
      <section class="col">
        <label>
          <span class="sr-only">Фамилия</span>
          <input v-model.trim="state.lastName" required aria-label="Фамилия" placeholder="Фамилия*" :class="{ invalid: showErrors && !state.lastName }" />
        </label>
        <label>
          <span class="sr-only">Имя</span>
          <input v-model.trim="state.firstName" required aria-label="Имя" placeholder="Имя*" :class="{ invalid: showErrors && !state.firstName }" />
        </label>
        <label>
          <span class="sr-only">Отчество</span>
          <input v-model.trim="state.patronymic" aria-label="Отчество" placeholder="Отчество" />
        </label>
        <label class="clickable" @click="openBirthCalendar">
          <span class="sr-only">Дата рождения</span>
          <DatePicker
            ref="dpRef"
            v-model="state.birthDate"
            value-type="format"
            format="YYYY-MM-DD"
            :editable="false"
            :clearable="true"
            :append-to-body="true"
            @change="onBirthChange"
            placeholder="Дата рождения"
            aria-label="Дата рождения"
          />
        </label>
      </section>

      <!-- Правая колонка: телефон, email, отдел, роли, логин/пароль -->
      <section class="col">
        <label>
          <span class="sr-only">Телефон</span>
          <input v-model.trim="state.phone" type="tel" placeholder="Телефон*" required aria-label="Телефон" @input="validatePhone" :class="{ invalid: !!errors.phone || (showErrors && !state.phone) }" />
          <small class="error" v-if="errors.phone">{{ errors.phone }}</small>
        </label>
        <label>
          <span class="sr-only">Email</span>
          <input v-model.trim="state.email" type="email" required aria-label="Email" placeholder="Email*" @input="validateEmail" :class="{ invalid: !!errors.email || (showErrors && !state.email) }" />
          <small class="error" v-if="errors.email">{{ errors.email }}</small>
        </label>
        <label>
          <span class="sr-only">Отдел</span>
          <div class="inline">
            <div class="custom-select" @click.stop="deptOpen = !deptOpen" @mousedown.stop tabindex="0" role="combobox" :aria-expanded="deptOpen" :aria-controls="'dept-list'">
              <span class="value">{{ deptLabel }}</span>
              <span class="caret">▾</span>
              <ul v-if="deptOpen" id="dept-list" class="dropdown" role="listbox" @click.stop @mousedown.stop>
                <li :class="{selected: state.departmentId===null}" role="option" @click.stop="selectDept(null)">—</li>
                <li v-for="d in departments" :key="d.id" :class="{selected: state.departmentId===d.id}" role="option" @click.stop="selectDept(d.id)">{{ d.name }}</li>
              </ul>
            </div>
            <button type="button" class="btn btn-secondary" @click.prevent="$emit('create-department')">Создать отдел</button>
          </div>
        </label>
        <div class="roles-rows" role="group" aria-label="Роли">
          <label class="role-row"><span>Пользователь</span><input type="checkbox" value="USER" v-model="state.roles" /></label>
          <label class="role-row"><span>Администратор</span><input type="checkbox" value="ADMIN" v-model="state.roles" /></label>
          <label class="role-row"><span>Владелец</span><input type="checkbox" value="OWNER" v-model="state.roles" /></label>
        </div>

        <!-- Ряд авторизации в правой колонке: слева логин, справа пароль и подтверждение -->
        <div class="auth-row">
          <label class="login">
            <span class="sr-only">Логин</span>
            <div class="inline">
              <input v-model.trim="state.login" required aria-label="Логин" placeholder="Логин*" :class="{ invalid: showErrors && !state.login }" />
              <button type="button" class="btn btn-secondary" @click.prevent="generateLogin">Сгенерировать</button>
            </div>
          </label>
          <label class="password">
            <span class="sr-only">Пароль</span>
            <input v-model="state.password" :required="mode==='create'" aria-label="Пароль" placeholder="Пароль*" @input="validatePassword" :class="{ invalid: showErrors && mode==='create' && !state.password }" />
          </label>
          <label class="confirm">
            <span class="sr-only">Подтверждение пароля</span>
            <input v-model="state.confirmPassword" :required="mode==='create'" aria-label="Подтверждение пароля" placeholder="Подтверждение пароля*" @input="validatePassword" :class="{ invalid: showErrors && mode==='create' && !state.confirmPassword }" />
            <small class="hint">Минимум 8 символов</small>
            <small class="error" v-if="errors.password">{{ errors.password }}</small>
          </label>
        </div>

        <label v-if="mode==='create' && !hideApiKey">
          ApiKey
          <input v-model="state.apiKey" readonly aria-readonly="true" aria-label="ApiKey" />
        </label>
      </section>
    </div>

    <div class="footer">
      <button class="btn" :class="{ loading }" type="submit" :disabled="!canSubmit || loading">
        <span v-if="loading" class="spinner" aria-hidden="true"></span>
        {{ submitText }}
      </button>
      <button class="btn btn-secondary" type="button" @click="$emit('cancel')">Отмена</button>
    </div>
  </form>
</template>

<script setup>
import { computed, reactive, watch, ref, nextTick, onMounted, onBeforeUnmount } from 'vue';
import DatePicker from 'vue-datepicker-next';
import 'vue-datepicker-next/index.css';

const props = defineProps({
  model: { type: Object, required: true },
  departments: { type: Array, default: () => [] },
  mode: { type: String, default: 'create' }, // create | edit
  hideApiKey: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
});
const emit = defineEmits(['submit', 'cancel', 'create-department']);

const state = reactive({
  lastName: '', firstName: '', patronymic: '', birthDate: '', email: '', phone: '',
  departmentId: null, roles: ['USER'], login: '', password: '', confirmPassword: '', apiKey: '',
});

const errors = reactive({ email: '', phone: '', password: '' });
const dpRef = ref(null);
const showErrors = ref(false);
const deptOpen = ref(false);
const deptLabel = computed(() => {
  if (state.departmentId == null) return '—';
  const d = (props.departments || []).find(x => x.id === state.departmentId);
  return d?.name ?? '—';
});

watch(() => props.model, (m) => {
  if (!m) return;
  state.lastName = m.lastName || '';
  state.firstName = m.firstName || '';
  state.patronymic = m.patronymic || '';
  state.birthDate = (m.birthDate || '').substring ? (m.birthDate || '').substring(0, 10) : (m.birthDate || '');
  state.email = m.email || '';
  state.phone = m.phone || '';
  state.departmentId = m.departmentId ?? null;
  state.roles = Array.isArray(m.roles) && m.roles.length ? [...new Set(m.roles)] : ['USER'];
  state.login = m.login || '';
  state.password = m.password || '';
  state.confirmPassword = '';
  if (props.mode === 'create' && !state.apiKey) state.apiKey = generateApiKey();
}, { immediate: true });

const submitText = computed(() => props.mode === 'edit' ? 'Сохранить' : 'Создать');
const canSubmit = computed(() => {
  const basic = !!state.lastName && !!state.firstName && !!state.email && !!state.phone && !!state.login;
  const pwdOk = props.mode === 'edit' ? true : (state.password && state.password.length >= 8 && state.password === state.confirmPassword);
  const emailOk = !errors.email;
  const phoneOk = !errors.phone;
  return basic && pwdOk && emailOk && phoneOk;
});

function generateLogin() {
  const first = (state.firstName || 'u')[0]?.toLowerCase() || 'u';
  const last = (state.lastName || 'user').toLowerCase();
  const rnd = Math.floor(Math.random() * 900 + 100);
  state.login = `${first}${last}${rnd}`;
}

function regenPassword() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%';
  const len = Math.floor(Math.random() * 5) + 8; // 8-12
  let p = '';
  for (let i = 0; i < len; i++) p += chars[Math.floor(Math.random() * chars.length)];
  state.password = p;
  state.confirmPassword = p;
  validatePassword();
}

function onSubmit() {
  if (!canSubmit.value) {
    showErrors.value = true;
    validateEmail();
    validatePhone();
    validatePassword();
    return;
  }
  emit('submit', { ...state });
}

function selectDept(id) {
  state.departmentId = id;
  deptOpen.value = false;
}

function onDeptKeydown(e) {
  // Предотвращаем открытие других модалок горячими клавишами/space
  const keys = ['Enter', ' ', 'Spacebar', 'ArrowDown', 'ArrowUp'];
  if (keys.includes(e.key)) {
    deptOpen.value = !deptOpen.value;
  }
}

function onBirthChange(val) {
  // vue-datepicker-next может отдавать строку/Date в зависимости от настроек
  if (val instanceof Date) {
    const yyyy = val.getFullYear();
    const mm = String(val.getMonth() + 1).padStart(2, '0');
    const dd = String(val.getDate()).padStart(2, '0');
    state.birthDate = `${yyyy}-${mm}-${dd}`;
  } else {
    state.birthDate = val || '';
  }
}

function validateEmail() {
  errors.email = '';
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!re.test(state.email || '')) errors.email = 'Некорректный email';
}
function validatePhone() {
  errors.phone = '';
  const re = /^\+7\s?\(?\d{3}\)?\s?\d{3}-?\d{2}-?\d{2}$/;
  if (!re.test(state.phone || '')) errors.phone = 'Формат: +7 (999) 999-99-99';
}
function validatePassword() {
  errors.password = '';
  if (props.mode === 'edit' && !state.password && !state.confirmPassword) return;
  if ((state.password && state.password.length < 8)) errors.password = 'Минимум 8 символов';
  else if (state.password !== state.confirmPassword) errors.password = 'Пароли не совпадают';
}
function generateApiKey() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789';
  let s = '';
  for (let i=0;i<24;i++) s += chars[Math.floor(Math.random()*chars.length)];
  return s;
}

function openBirthCalendar() {
  nextTick(() => {
    // 1) Попробуем кликнуть по инпуту внутри пикера
    const el = dpRef.value?.$el?.querySelector('input') || dpRef.value?.input || dpRef.value?.$refs?.input;
    if (el) {
      el.focus();
      el.click();
    }
    // 2) Явно попросим открыть попап (в разных версиях API отличаются названия)
    try { dpRef.value?.open?.(); } catch {}
    try { dpRef.value?.showPopup?.(); } catch {}
    try { dpRef.value?.togglePopup?.(true); } catch {}
  });
}

function onDocumentClick(e) {
  // Закрываем список отделов при клике вне кастомного селекта
  const sel = e.target.closest?.('.custom-select');
  if (!sel) deptOpen.value = false;
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick, true);
});
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick, true);
});
</script>

<style scoped>
.form { display: flex; flex-direction: column; gap: 1rem; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.col { display: grid; grid-template-columns: 1fr; gap: 10px; align-content: start; }
label { display: flex; flex-direction: column; gap: 4px; font-size: 14px; }
input, select { padding: 8px 10px; border-radius: 8px; border: 1px solid var(--border,#3b3b3b); background: var(--input-bg,#1e1e1e); color: var(--text,#fff); width: 100%; }
.inline { display: flex; gap: 8px; align-items: center; }
.inline input { flex: 1 1 auto; min-width: 0; }
.inline .btn, .inline button { flex: 0 0 auto; white-space: nowrap; }
.custom-select { position: relative; display: inline-flex; align-items: center; justify-content: space-between; gap: 8px; padding: 8px 10px; min-width: 160px; border-radius: 8px; border: 1px solid var(--border,#3b3b3b); background: var(--input-bg,#1e1e1e); color: var(--text,#fff); cursor: pointer; }
.custom-select .value { pointer-events: none; }
.custom-select .caret { opacity: .7; pointer-events: none; }
.custom-select:focus { outline: none; box-shadow: 0 0 0 2px color-mix(in srgb, var(--primary,#3498db) 25%, transparent); border-color: var(--primary,#3498db); }
.dropdown { position: absolute; top: calc(100% + 4px); left: 0; right: 0; max-height: 220px; overflow-y: auto; background: var(--input-bg,#1e1e1e); color: var(--text,#fff); border: 1px solid var(--border,#3b3b3b); border-radius: 8px; padding: 6px; z-index: 10020; }
.dropdown li { padding: 8px 10px; border-radius: 6px; cursor: pointer; }
.dropdown li:hover { background: var(--input-bg-hover, #2a2a2a); }
.dropdown li.selected { background: color-mix(in srgb, var(--primary,#3498db) 16%, transparent); border: 1px solid var(--primary,#3498db); }
.roles { display: flex; flex-direction: column; gap: 8px; border: 1px solid var(--border,#333); border-radius: 8px; padding: .5rem .75rem; background: var(--card, transparent); }
.roles-list { display: grid; grid-template-columns: 1fr; gap: 6px; }
.role-item { display: flex; align-items: center; gap: 8px; line-height: 1.2; }
.legend { font-size: 12px; color: var(--muted,#aaa); margin-bottom: 2px; font-weight: 600; }
.clickable :deep(.mx-input) { width: 100%; }
.clickable :deep(.mx-input) { background: var(--input-bg,#1e1e1e); color: var(--text,#fff); border: 1px solid var(--border,#3b3b3b); border-radius: 8px; }
.auth-row { display: grid; grid-template-columns: 1.8fr 1fr; gap: 10px 16px; align-items: start; }
.auth-row .login { grid-column: 1; }
.auth-row .password { grid-column: 2; }
.auth-row .confirm { grid-column: 2; }
.auth-row .login .inline { width: 100%; }
.auth-row .login input { width: 100%; }
.auth-row .login .inline > :not(input):not(button) { display: none !important; }
@media (max-width: 768px) {
  .auth-row { grid-template-columns: 1fr; }
  .auth-row .login, .auth-row .password, .auth-row .confirm { grid-column: 1; }
}
.roles-rows { display: grid; grid-template-rows: repeat(3, auto); gap: 6px; }
.role-row { display: grid; grid-template-columns: 1fr auto; align-items: center; gap: 8px; }
.role-row span { white-space: nowrap; }
.role-row input { width: 16px; height: 16px; }
/* DatePicker popup above modal */
:deep(.mx-datepicker-popup), :deep(.mx-datepicker-main) { z-index: 100000 !important; position: absolute !important; }
:deep(.mx-datepicker-main) { background: var(--card,#1e1e1e); border: 1px solid var(--border,#3b3b3b); border-radius: 10px; }
:deep(.mx-calendar) { background: transparent; color: var(--text,#fff); }
:deep(.mx-calendar-header) { border-bottom: 1px solid var(--border,#333); }
:deep(.mx-table-date th), :deep(.mx-table-date td) { color: var(--text,#fff); }
:deep(.mx-calendar .cell.today) { background: color-mix(in srgb, var(--primary,#3498db) 12%, transparent); border-radius: 6px; }
.footer { display: flex; gap: .5rem; justify-content: flex-end; }
.btn { background: var(--primary,#3498db); border: none; padding: 8px 12px; border-radius: 8px; color: #fff; cursor: pointer; }
.btn-secondary { background: transparent; border: 1px solid var(--border,#3b3b3b); color: var(--text,#fff); }
.hint { color: var(--muted,#aaa); font-size: 12px; }
.error { color: #ff7675; font-size: 12px; }
.clickable { cursor: pointer; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
input:focus, select:focus { outline: none; border-color: var(--primary,#3498db); box-shadow: 0 0 0 2px color-mix(in srgb, var(--primary,#3498db) 25%, transparent); }
input.invalid, select.invalid { border-color: var(--error,#ff5a5f); }
/* Make select dropdown closer to theme where possible */
.select-input { background: var(--input-bg,#1e1e1e); color: var(--text,#fff); border: 1px solid var(--border,#3b3b3b); }
.select-input option { background: var(--input-bg,#1e1e1e); color: var(--text,#fff); }
</style>

<!-- Global styles for 3rd-party popups appended to <body> -->
<style>
/* vue-datepicker-next popup rendered to body: enforce dark theme and topmost z-index */
.mx-datepicker-popup,
.mx-datepicker-main {
  z-index: 100000 !important;
}
.mx-datepicker-main {
  background: var(--card, #1e1e1e) !important;
  border: 1px solid var(--border, #3b3b3b) !important;
  border-radius: 10px !important;
}
.mx-calendar { background: transparent !important; color: var(--text, #fff) !important; }
.mx-calendar-header { border-bottom: 1px solid var(--border, #333) !important; }
.mx-table-date th, .mx-table-date td { color: var(--text, #fff) !important; }
.mx-calendar .cell.today { background: color-mix(in srgb, var(--primary, #3498db) 12%, transparent) !important; border-radius: 6px !important; }
</style>