<template>
  <div class="page">
    <header class="page-header">
      <h1>Управление персоналом</h1>
      <div class="actions">
        <button class="btn" @click="openCreateUser">Создать пользователя</button>
        <button class="btn" @click="openCreateDepartment">Создать отдел</button>
        <button class="btn btn-secondary" @click="exportCsv">Экспорт CSV</button>
      </div>
    </header>

    <section class="filters">
      <input v-model.trim="query" @input="applyClientFilters" placeholder="Поиск по логину или ФИО" />
      <select multiple v-model="filter.departmentIds" @change="applyClientFilters" title="Отделы (Ctrl/Shift для мультивыбора)">
        <option v-for="d in departments" :key="d.id" :value="d.id">{{ d.name }}</option>
      </select>
      <select multiple v-model="filter.roles" @change="applyClientFilters" title="Роли (Ctrl/Shift для мультивыбора)">
        <option value="USER">Пользователь</option>
        <option value="ADMIN">Администратор</option>
        <option value="OWNER">Владелец</option>
      </select>
      <button class="btn" @click="refresh">Применить</button>
    </section>

    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>Логин</th>
            <th>ФИО</th>
            <th>Роли</th>
            <th>Отдел</th>
            <th>Создан</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in filteredUsers" :key="u.id">
            <td><span v-html="hl(u.login)"></span></td>
            <td><span v-html="hl(fio(u))"></span></td>
            <td>
              <span v-for="r in u.roles" :key="r" class="role" :title="roleHint(r)">{{ r }}</span>
            </td>
            <td>{{ departmentName(u.departmentId) }}</td>
            <td>{{ formatDate(u.createdAt) }}</td>
            <td>
              <button class="link" @click="editUser(u)">Редактировать</button>
              <button class="link danger" @click="removeUser(u)">Удалить</button>
            </td>
          </tr>
          <tr v-if="!loading && users.length === 0">
            <td colspan="6" class="empty">Ничего не найдено</td>
          </tr>
          <tr v-if="loading">
            <td colspan="6" class="empty">Загрузка...</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination">
      <button class="btn" :disabled="page===1" @click="page--; refresh()">Назад</button>
      <span>Страница {{ page }}</span>
      <button class="btn" :disabled="!hasMore" @click="page++; refresh()">Вперёд</button>
    </div>

    <!-- Модалка создания пользователя: используем общий Modal (как у товара) -->
    <Modal :is-modal-visible="showCreateUser" width="720px" @close="showCreateUser=false">
      <template #header>
        <h3 class="modal-title" style="display:flex;align-items:center;gap:.5rem;">
          <span aria-hidden="true" style="font-size:18px;line-height:1">➕</span>
          Создание пользователя
        </h3>
      </template>
      <template #content>
        <UserForm
          :model="createUser"
          :departments="departments"
          :hideApiKey="true"
          :loading="createLoading"
          mode="create"
          @create-department="openCreateDepartment"
          @cancel="showCreateUser=false"
          @submit="handleCreateUserSubmit"
        />
      </template>
    </Modal>

    <!-- Модалка редактирования пользователя -->
    <ModalBase v-model="showEditUser" title="Редактирование пользователя" :width="860">
      <UserForm
        :model="editUserModel"
        :departments="departments"
        mode="edit"
        @create-department="openCreateDepartment"
        @cancel="showEditUser=false"
        @submit="handleEditUserSubmit"
      />
    </ModalBase>

    <!-- Модалка создания отдела -->
    <ModalBase v-model="showCreateDept" title="Создание отдела" :width="600">
      <form @submit.prevent="submitCreateDepartment" class="form">
        <label>Название отдела*<input v-model.trim="createDept.name" required /></label>
        <label>Описание<textarea v-model.trim="createDept.description" rows="3" /></label>
        <div class="footer">
          <button class="btn" type="submit" :disabled="!createDept.name">Сохранить</button>
          <button class="btn btn-secondary" type="button" @click="showCreateDept=false">Отмена</button>
        </div>
      </form>
    </ModalBase>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {StaffApi} from '@/services/staff';
import {useAuthStore} from '@/store/auth';
import ModalBase from '@/components/ui/ModalBase.vue';
import Modal from '@/components/Modal.vue';
import UserForm from '@/components/staff/UserForm.vue';


const auth = useAuthStore();
const merchantId = computed(() => auth.user?.id ?? '—');
const users = ref([]);
const loading = ref(false);
const page = ref(1);
const pageSize = 10;
const hasMore = ref(false);
const query = ref('');
const filter = reactive({ departmentIds: [], roles: [] });
const departments = ref([]);

function fio(u) { return [u.lastName, u.firstName, u.patronymic].filter(Boolean).join(' '); }
function roleHint(code) {
  return code === 'OWNER' ? 'Владелец' : code === 'ADMIN' ? 'Администратор' : 'Пользователь';
}
function roleTooltip(code) {
  return code === 'OWNER'
    ? 'Полный контроль, назначение ролей'
    : code === 'ADMIN'
      ? 'Администрирование персонала и данных'
      : 'Базовый доступ пользователя';
}
function departmentName(id) { return departments.value.find(d => d.id === id)?.name || '—'; }
function formatDate(iso) { return iso ? new Date(iso).toLocaleDateString() : '—'; }

// Подсветка совпадений в тексте
function escapeRegExp(s){return s.replace(/[.*+?^${}()|[\]\\]/g,'\\$&');}
function hl(text){
  const q = (query.value||'').trim();
  if(!q) return String(text||'');
  try{
    const re = new RegExp(escapeRegExp(q),'ig');
    return String(text||'').replace(re, m => `<mark>${m}</mark>`);
  }catch(_){return String(text||'');}
}

// Клиентская фильтрация по мультиселектам
const filteredUsers = computed(() => {
  let list = users.value || [];
  const dept = filter.departmentIds;
  const roles = filter.roles;
  if (dept && dept.length) {
    const ids = new Set(dept.map(String));
    list = list.filter(u => u.departmentId != null && ids.has(String(u.departmentId)));
  }
  if (roles && roles.length) {
    const rs = new Set(roles);
    list = list.filter(u => (u.roles||[]).some(r => rs.has(r)));
  }
  const q = (query.value||'').trim().toLowerCase();
  if (q) {
    list = list.filter(u =>
      (u.login||'').toLowerCase().includes(q) ||
      fio(u).toLowerCase().includes(q)
    );
  }
  return list;
});

function applyClientFilters(){ /* вычисляется реактивно */ }

async function refresh() {
  loading.value = true;
  try {
    const params = {
      masterId: auth.user?.id,
      query: query.value || undefined,
      // для мультиселектов оставляем клиентскую фильтрацию, сервер получает общий список мастера
      page: page.value,
      size: pageSize,
    };
    const res = await StaffApi.listUsers(params);
    users.value = res.items || [];
    hasMore.value = (res.page * res.size) < (res.total || 0);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  try {
    const list = await StaffApi.listDepartments?.();
    departments.value = list || [];
  } catch (e) {
    console.error('Не удалось загрузить отделы', e);
  }
  await refresh();
});

// Create user modal state
const showCreateUser = ref(false);
const showEditUser = ref(false);
const createUser = reactive({
  lastName: '', firstName: '', patronymic: '', birthDate: '', email: '', phone: '',
  departmentId: null, roles: ['USER'], login: '', password: ''
});
const editUserModel = reactive({ id: null, lastName: '', firstName: '', patronymic: '', birthDate: '', email: '', phone: '', departmentId: null, roles: ['USER'], login: '' });

function generateLogin() {
  const first = (createUser.firstName || 'u')[0]?.toLowerCase() || 'u';
  const last = (createUser.lastName || 'user').toLowerCase();
  const rnd = Math.floor(Math.random() * 900 + 100);
  createUser.login = `${first}${last}${rnd}`;
}
function regenPassword() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%';
  const len = Math.floor(Math.random() * 5) + 8; // 8-12
  let p = '';
  for (let i = 0; i < len; i++) p += chars[Math.floor(Math.random() * chars.length)];
  createUser.password = p;
}

const canCreateUser = computed(() => !!createUser.lastName && !!createUser.firstName && !!createUser.login && !!createUser.password);

function openCreateUser() {
  showCreateUser.value = true;
  if (!createUser.login) generateLogin();
  if (!createUser.password) regenPassword();
}

const createLoading = ref(false);

async function submitCreateUser() {
  const payload = {
    lastName: createUser.lastName,
    firstName: createUser.firstName,
    patronymic: createUser.patronymic || null,
    email: createUser.email,
    phone: createUser.phone,
    departmentId: createUser.departmentId || null,
    roles: createUser.roles,
    login: createUser.login,
    password: createUser.password,
    masterId: auth.user?.id,
  };
  if (createUser.birthDate) payload.birthDate = createUser.birthDate;
  try {
    createLoading.value = true;
    await StaffApi.createUser(payload);
    showCreateUser.value = false;
    await refresh();
  } finally {
    createLoading.value = false;
  }
}

function handleCreateUserSubmit(formState) {
  // Прямо переиспользуем submitCreateUser, подхватив state
  Object.assign(createUser, formState);
  submitCreateUser();
}

// Department modal
const showCreateDept = ref(false);
const createDept = reactive({ name: '', description: '' });
function openCreateDepartment() { showCreateDept.value = true; }
async function submitCreateDepartment() {
  const dto = await StaffApi.createDepartment({ name: createDept.name, description: createDept.description || '' });
  if (dto?.id) {
    departments.value.push(dto);
  }
  showCreateDept.value = false;
}

async function editUser(u) {
  // Заполняем модель и открываем модалку
  Object.assign(editUserModel, {
    id: u.id,
    lastName: u.lastName,
    firstName: u.firstName,
    patronymic: u.patronymic,
    birthDate: (u.birthDate || '').substring ? u.birthDate.substring(0,10) : '',
    email: u.email,
    phone: u.phone,
    departmentId: u.departmentId ?? null,
    roles: Array.isArray(u.roles) ? [...u.roles] : ['USER'],
    login: u.login,
  });
  showEditUser.value = true;
}

async function handleEditUserSubmit(state) {
  const payload = {
    lastName: state.lastName,
    firstName: state.firstName,
    patronymic: state.patronymic || null,
    email: state.email,
    phone: state.phone,
    departmentId: state.departmentId || null,
    roles: state.roles,
    login: state.login,
    // пароль опционально — только если указан
    ...(state.password ? { password: state.password } : {}),
  };
  if (state.birthDate) payload.birthDate = state.birthDate;
  await StaffApi.updateUser(editUserModel.id, payload);
  showEditUser.value = false;
  await refresh();
}
async function removeUser(u) {
  if (confirm('Удалить пользователя?')) {
    await StaffApi.deleteUser(u.id);
    await refresh();
  }
}

function exportCsv() {
  const header = ['login','lastName','firstName','patronymic','roles','department','createdAt'];
  const rows = users.value.map(u => [u.login,u.lastName,u.firstName,u.patronymic,(u.roles||[]).join('|'),departmentName(u.departmentId),u.createdAt||'']);
  const csv = [header.join(','), ...rows.map(r => r.map(v => '"'+String(v??'').replaceAll('"','""')+'"').join(','))].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = 'staff.csv'; a.click();
  URL.revokeObjectURL(url);
}
</script>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; padding: 1rem; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1rem; }
.actions { display: flex; gap: .5rem; }
.filters { display: flex; gap: .5rem; margin-bottom: .75rem; flex-wrap: wrap; }
.input { padding: 8px 10px; border-radius: 8px; border: 1px solid #3b3b3b; background: #1e1e1e; color: #fff; }
.table-wrap { overflow: auto; border: 1px solid #3b3b3b; border-radius: 8px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #333; text-align: left; }
.role { display: inline-block; background: #2c3e50; padding: 2px 6px; border-radius: 6px; margin-right: 4px; font-size: 12px; }
.empty { text-align: center; color: #aaa; }
.pagination { display: flex; align-items: center; justify-content: center; gap: .75rem; padding: .75rem 0; }
.btn { background: #3498db; border: none; padding: 8px 12px; border-radius: 8px; color: #fff; cursor: pointer; }
.btn-secondary { background: transparent; border: 1px solid #3b3b3b; }
.link { background: none; border: none; color: #6db6ff; cursor: pointer; padding: 0; }
.link.danger { color: #ff7675; }

.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: .5rem; }
.inline { display: flex; gap: .5rem; align-items: center; }
.roles { border: 1px solid #333; border-radius: 8px; padding: .5rem; }

/* Create User modal wide card layout */
.create-user-grid {
  /* компенсируем внутренний паддинг modal-body (1rem) до ровных 32px */
  margin: -16px;
  padding: 32px;
  display: grid;
  grid-template-columns: 30% 70%;
  gap: 24px;
}
.create-user-grid .left { display: flex; flex-direction: column; gap: 16px; }
.create-user-grid .left label { display: flex; flex-direction: column; gap: 6px; font-size: 14px; }
.create-user-grid .left input { width: 100%; padding: 8px 10px; border-radius: 4px; border: 1px solid #3b3b3b; background: #1e1e1e; color: #fff; }
.avatar-placeholder { width: 100%; aspect-ratio: 1/1; border: 1px dashed #3b3b3b; border-radius: 4px; display: grid; place-items: center; color: #888; font-size: 42px; }
.create-user-grid .right { min-width: 0; }

@media (max-width: 768px) {
  .create-user-grid { grid-template-columns: 1fr; padding: 24px; margin: -16px; }
}
</style>
