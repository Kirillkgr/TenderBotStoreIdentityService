<template>
  <div>
    <div
        :class="{
        open: !ui.isDesktop && ui.sidebarOpen,
        docked: ui.isDesktop && ui.sidebarOpen,
        collapsed: ui.isDesktop && ui.sidebarCollapsed,
      }"
        class="sidebar"
        @click.stop
    >
      <div class="sidebar__header hidden"></div>

      <nav class="menu">
        <ul v-if="isAuth">
          <!-- Главные пункты с иконками -->
          <li class="main-item">
            <router-link to="/" @click.native="onNavigate"><span class="ico">📊</span><span class="txt">Статистика</span>
            </router-link>
          </li>
          <li class="main-item">
            <router-link v-can="{ any: ['ADMIN','OWNER'], mode: 'hide' }" to="/admin" @click.native="onNavigate"><span
                class="ico">💰</span><span class="txt">Финансы</span></router-link>
          </li>
          <li class="main-item">
            <router-link to="/" @click.native="onNavigate"><span class="ico">📋</span><span class="txt">Меню</span>
            </router-link>
          </li>

          <!-- Раздел: Склад -->
          <li :class="{ hidden: ui.isDesktop && ui.sidebarCollapsed }" class="group">
            <button class="group-btn" type="button" @click="toggleGroup('sklad')">
              <span class="ico">📦</span>
              <span class="txt strong">Склад</span>
              <span :class="{ open: expanded.sklad }" class="chev">▸</span>
            </button>
          </li>
          <template v-if="expanded.sklad">
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER','COOK','CASHIER'], mode: 'hide' }" to="/admin/inventory/stock"
                           @click.native="onNavigate"><span class="txt">Остатки</span></router-link>
            </li>
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER'], mode: 'hide' }" to="/admin/inventory/supplies"
                           @click.native="onNavigate"><span class="txt">Поставки</span></router-link>
            </li>
            <li class="subitem muted" title="Требует реализации"><a href="#" @click.prevent><span class="txt">Производства</span></a>
            </li>
            <li class="subitem muted" title="Требует реализации"><a href="#" @click.prevent><span class="txt">Переработки</span></a>
            </li>
            <li class="subitem muted" title="Требует реализации"><a href="#" @click.prevent><span class="txt">Перемещения</span></a>
            </li>
            <li class="subitem muted" title="Требует реализации"><a href="#" @click.prevent><span
                class="txt">Списания</span></a></li>
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER','COOK','CASHIER'], mode: 'hide' }"
                           to="/admin/inventory/warehouses" @click.native="onNavigate">
                <span class="txt">Склады</span></router-link>
            </li>
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER','COOK','CASHIER'], mode: 'hide' }"
                           to="/admin/inventory/ingredients" @click.native="onNavigate">
                <span class="txt">Ингредиенты</span></router-link>
            </li>
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER'], mode: 'hide' }" to="/admin/inventory/units"
                           @click.native="onNavigate"><span class="txt">Единицы</span></router-link>
            </li>
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER'], mode: 'hide' }" to="/admin/inventory/suppliers"
                           @click.native="onNavigate"><span class="txt">Поставщики</span></router-link>
            </li>
          </template>

          <!-- Раздел: Маркетинг -->
          <li :class="{ hidden: ui.isDesktop && ui.sidebarCollapsed }" class="group">
            <button class="group-btn" type="button" @click="toggleGroup('marketing')">
              <span class="ico">📣</span>
              <span class="txt strong">Маркетинг</span>
              <span :class="{ open: expanded.marketing }" class="chev">▸</span>
            </button>
          </li>
          <template v-if="expanded.marketing">
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER'], mode: 'hide' }" to="/admin/clients"
                           @click.native="onNavigate"><span class="txt">Клиенты</span></router-link>
            </li>
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER','CASHIER'], mode: 'hide' }" to="/admin/orders"
                           @click.native="onNavigate"><span class="txt">Заказы</span></router-link>
            </li>
          </template>

          <!-- Раздел: Операции -->
          <li :class="{ hidden: ui.isDesktop && ui.sidebarCollapsed }" class="group">
            <button class="group-btn" type="button" @click="toggleGroup('ops')">
              <span class="ico">🛠️</span>
              <span class="txt strong">Операции</span>
              <span :class="{ open: expanded.ops }" class="chev">▸</span>
            </button>
          </li>
          <template v-if="expanded.ops">
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER','COOK'], mode: 'hide' }" to="/kitchen"
                           @click.native="onNavigate"><span class="txt">Кухня</span></router-link>
            </li>
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER','CASHIER'], mode: 'hide' }" to="/cashier"
                           @click.native="onNavigate"><span class="txt">Касса</span></router-link>
            </li>
          </template>

          <!-- Раздел: Другое -->
          <li :class="{ hidden: ui.isDesktop && ui.sidebarCollapsed }" class="group">
            <button class="group-btn" type="button" @click="toggleGroup('other')">
              <span class="ico">📁</span>
              <span class="txt strong">Другое</span>
              <span :class="{ open: expanded.other }" class="chev">▸</span>
            </button>
          </li>
          <template v-if="expanded.other">
            <li class="subitem">
              <router-link to="/my-orders" @click.native="onNavigate"><span class="txt">Мои заказы</span></router-link>
            </li>
            <li class="subitem">
              <router-link to="/profile" @click.native="onNavigate"><span class="txt">Профиль</span></router-link>
            </li>
            <li class="subitem">
              <router-link to="/profile/edit" @click.native="onNavigate"><span class="txt">Профиль (ред.)</span>
              </router-link>
            </li>
            <li v-if="auth.brandId" class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER'], mode: 'hide' }" :to="`/brands/${auth.brandId}/tags`"
                           @click.native="onNavigate"><span class="txt">Теги бренда</span></router-link>
            </li>
            <li class="subitem">
              <router-link v-can="{ any: ['ADMIN','OWNER'], mode: 'hide' }" to="/admin/archive"
                           @click.native="onNavigate"><span class="txt">Архив товаров</span></router-link>
            </li>
            <li class="subitem">
              <router-link to="/staff" @click.native="onNavigate"><span class="txt">Сотрудники</span></router-link>
            </li>
            <li class="subitem">
              <router-link to="/cart" @click.native="onNavigate"><span class="txt">Корзина</span></router-link>
            </li>
            <li class="subitem">
              <router-link to="/checkout" @click.native="onNavigate"><span class="txt">Оформление заказа</span>
              </router-link>
            </li>
          </template>

          <!-- Главный пункт: Админ -->
          <li class="main-item">
            <router-link v-can="{ any: ['ADMIN','OWNER'], mode: 'hide' }" to="/admin" @click.native="onNavigate"><span
                class="ico">⚙️</span><span class="txt">Админ</span></router-link>
          </li>
        </ul>
        <ul v-else>
          <li class="main-item">
            <button class="login-btn" type="button" @click="openLogin"><span class="ico">🔐</span><span
                class="txt">Войти</span></button>
          </li>
        </ul>
      </nav>

      <div v-if="isAuth && ui.isDesktop && !ui.sidebarCollapsed" class="sidebar__footer">
        <button class="logout-btn" type="button" @click="logout">Выйти</button>
      </div>
    </div>

    <div v-if="!ui.isDesktop && ui.sidebarOpen" class="overlay" @click="ui.closeSidebar"></div>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, watch} from 'vue';
import {useRoute} from 'vue-router';
import {useUiStore} from '../store/ui';
import {useAuthStore} from '../store/auth';

const ui = useUiStore();
const route = useRoute();
const auth = useAuthStore();
const isAuth = computed(() => auth.isAuthenticated);

const expanded = reactive({sklad: false, marketing: false, ops: false, other: false});

function toggleGroup(key) {
  if (ui.isDesktop && ui.sidebarCollapsed) {
    ui.setCollapsed(false);
    // раскрываем нужную группу после разворота
    expanded[key] = true;
    return;
  }
  expanded[key] = !expanded[key];
}

// persist expanded state
watch(expanded, (v) => {
  try {
    localStorage.setItem('ui_sidebar_expanded', JSON.stringify(v));
  } catch {
  }
}, {deep: true});

function restoreExpanded() {
  try {
    const raw = localStorage.getItem('ui_sidebar_expanded');
    if (raw) {
      const obj = JSON.parse(raw);
      expanded.sklad = !!obj.sklad;
      expanded.marketing = !!obj.marketing;
      expanded.ops = !!obj.ops;
      expanded.other = !!obj.other;
    }
  } catch {
  }
}

function autoExpandByRoute(path) {
  // inventory -> склад
  if (/^\/admin\/(inventory|orders)?/.test(path)) {
    // inventory definitely склад; orders относится к маркетингу, обработаем ниже
    if (/^\/admin\/inventory\//.test(path)) expanded.sklad = true;
  }
  if (/^\/admin\/(clients|orders)/.test(path)) expanded.marketing = true;
  if (/^\/(kitchen|cashier)/.test(path)) expanded.ops = true;
  if (/^\/(profile(\/edit)?|my-orders|cart|checkout|staff|admin\/archive|brands\/[^/]+\/tags)/.test(path)) expanded.other = true;
}

onMounted(() => {
  restoreExpanded();
  autoExpandByRoute(route.path || '');
});

watch(() => route.path, (p) => autoExpandByRoute(p || ''));

function onNavigate() {
  if (!ui.isDesktop) ui.closeSidebar();
}

async function logout() {
  try {
    await auth.logout();
  } catch {
  }
}

function openLogin() {
  try {
    window.dispatchEvent(new Event('open-login-modal'));
  } catch {
  }
  if (!ui.isDesktop) ui.closeSidebar();
}
</script>

<style scoped>
.sidebar {
  position: fixed;
  top: var(--header-height, 60px);
  left: 0;
  bottom: 0;
  width: 320px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border);
  transform: translateX(-100%);
  transition: transform .2s ease;
  z-index: 1200;
  color: var(--text);
  padding-bottom: 16px;
  display: flex;
  flex-direction: column;
}

.sidebar.open {
  transform: translateX(0);
}

.sidebar.docked {
  transform: none;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, .06);
}

.menu {
  padding: 8px 0;
  flex: 1;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.menu ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.menu li {
  padding: 4px 10px;
}

.menu a {
  color: var(--text);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
}

.group {
  margin-top: 8px;
  font-weight: 700;
  color: var(--muted);
}

.group-btn {
  width: 100%;
  background: transparent;
  border: 0;
  color: inherit;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 6px;
  cursor: pointer;
  border-radius: 8px;
}

.group-btn:hover {
  background: rgba(255, 255, 255, .05);
}

.strong {
  font-weight: 700;
}

.chev {
  display: inline-block;
  transform: rotate(0deg);
  transition: transform .15s ease;
  width: 14px;
  text-align: center;
}

.chev.open {
  transform: rotate(90deg);
}

.main-item a {
  font-size: 15px;
  font-weight: 600;
}

.subitem a {
  font-size: 13px;
  padding-left: 26px;
}

.main-item + .group {
  margin-top: 10px;
}

.muted {
  opacity: .55;
  cursor: not-allowed;
}

.overlay {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  top: var(--header-height, 60px);
  background: rgba(0, 0, 0, .45);
  z-index: 1100;
}

.txt {
  display: inline-block;
}

.ico {
  width: 20px;
  text-align: center;
}

.hidden {
  display: none;
}

.sidebar.collapsed .txt {
  display: none;
}

.sidebar__footer {
  border-top: 1px solid var(--sidebar-border);
  padding: 10px 12px;
}

.logout-btn {
  width: 100%;
  background: transparent;
  border: 1px solid var(--sidebar-border);
  color: var(--text);
  border-radius: 8px;
  padding: 8px 10px;
  cursor: pointer;
}

.logout-btn:hover {
  background: var(--active-bg);
}

.login-btn {
  width: 100%;
  text-align: left;
  background: transparent;
  border: 0;
  color: var(--text);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
}

.login-btn:hover {
  background: var(--active-bg);
}

/* Sidebar scrollbar (WebKit) */
.menu::-webkit-scrollbar {
  width: 8px;
}

.menu::-webkit-scrollbar-track {
  background: transparent;
  margin: 6px 0;
}

.menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, .16);
  border-radius: 8px;
  border: 2px solid transparent; /* creates padding around thumb */
  background-clip: padding-box;
}

.menu:hover::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, .28);
}

/* Sidebar scrollbar (Firefox) */
.menu {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, .28) transparent;
}

/* Mobile adaptations: iPhone SE (<=360px) */
@media (max-width: 360px) {
  .sidebar {
    width: min(88vw, 300px);
    top: var(--header-height, 52px);
  }

  .menu a {
    padding: 12px;
    min-height: 44px;
    font-size: 14px;
  }

  .subitem a {
    padding-left: 28px;
  }

  .group-btn {
    padding: 12px 8px;
    min-height: 44px;
  }

  .ico {
    width: 18px;
  }

  .overlay {
    top: var(--header-height, 52px);
  }
}

/* Ultra small: <=320px */
@media (max-width: 320px) {
  .sidebar {
    width: min(90vw, 280px);
    top: var(--header-height, 50px);
  }

  .menu a {
    padding: 10px;
    min-height: 44px;
    font-size: 13px;
  }

  .group-btn {
    padding: 10px 8px;
  }
}

/* Legacy tiny: <=300px */
@media (max-width: 300px) {
  .sidebar {
    width: 90vw;
    top: var(--header-height, 48px);
  }

  .menu a {
    padding: 10px;
    min-height: 44px;
    font-size: 12.5px;
  }

  .subitem a {
    padding-left: 24px;
  }
}
</style>
