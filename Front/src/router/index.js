import {createRouter, createWebHistory} from 'vue-router';
import {useAuthStore} from '../store/auth';
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
import ProfileView from '../views/ProfileView.vue';
import CartView from '../views/CartView.vue';
import ProductDetailView from '../views/ProductDetailView.vue';
import CheckoutView from '../views/CheckoutView.vue';
import AdminView from '../views/AdminView.vue';
import KitchenView from '../views/KitchenView.vue';
import CashierView from '../views/CashierView.vue';
import AdminOrdersView from '../views/AdminOrdersView.vue';
import AdminClientsView from '../views/AdminClientsView.vue';
import TagsView from '../views/TagsView.vue';
import ProfileEditView from '../views/ProfileEditView.vue';
import StaffManagementView from '../views/StaffManagementView.vue';
import WarehousesView from '../views/inventory/WarehousesView.vue';
import UnitsView from '../views/inventory/UnitsView.vue';
import SuppliersView from '../views/inventory/SuppliersView.vue';
import SuppliesView from '../views/inventory/SuppliesView.vue';
import IngredientsView from '../views/inventory/IngredientsView.vue';
import StockView from '../views/inventory/StockView.vue';
import ArchiveProductsView from '../views/ArchiveProductsView.vue';
import MyOrdersView from '../views/MyOrdersView.vue';

const routes = [
    {
        path: '/',
        name: 'Home',
        meta: { title: 'Главная' },
        component: HomeView,
    },
    {
        path: '/admin/archive',
        name: 'AdminArchive',
        meta: { title: 'Архив товаров', requiresAuth: true, requiresAdmin: true },
        component: ArchiveProductsView,
    },
    {
        path: '/login',
        name: 'Login',
        meta: { title: 'Вход' },
        component: LoginView,
    },
    {
        path: '/register',
        name: 'Register',
        meta: { title: 'Регистрация' },
        component: RegisterView,
    },
    {
        path: '/profile',
        name: 'Profile',
        meta: { title: 'Профиль', requiresAuth: true },
        component: ProfileView,
    },
    {
        path: '/profile/edit',
        name: 'ProfileEdit',
        meta: { title: 'Редактировать профиль', requiresAuth: true },
        component: ProfileEditView,
    },
    {
        path: '/cart',
        name: 'Cart',
        meta: { title: 'Корзина', requiresAuth: true },
        component: CartView,
    },
    {
        path: '/product/:id',
        name: 'ProductDetail',
        meta: { title: 'Товар' },
        component: ProductDetailView,
    },
    {
        path: '/checkout',
        name: 'Checkout',
        meta: { title: 'Оформление заказа', requiresAuth: true },
        component: CheckoutView,
    },
    {
        path: '/my-orders',
        name: 'MyOrders',
        meta: {title: 'Мои заказы', requiresAuth: true},
        component: MyOrdersView,
    },
    {
        path: '/admin',
        name: 'Admin',
        meta: { title: 'Админ панель', requiresAuth: true, requiresAdmin: true },
        component: AdminView,
    },
    {
        path: '/admin/orders',
        name: 'AdminOrders',
        meta: {title: 'Заказы', requiresAuth: true, roles: ['ADMIN', 'OWNER', 'CASHIER']},
        component: AdminOrdersView,
    },
    {
        path: '/admin/clients',
        name: 'AdminClients',
        meta: {title: 'Клиенты', requiresAuth: true, roles: ['ADMIN', 'OWNER']},
        component: AdminClientsView,
    },
    {
        path: '/staff',
        name: 'StaffManagement',
        component: StaffManagementView,
    },
    {
        path: '/admin/inventory/warehouses',
        name: 'Warehouses',
        meta: {title: 'Склады', requiresAuth: true, roles: ['ADMIN', 'OWNER', 'COOK', 'CASHIER']},
        component: WarehousesView,
    },
    {
        path: '/admin/inventory/units',
        name: 'Units',
        meta: {title: 'Единицы измерения', requiresAuth: true, roles: ['ADMIN', 'OWNER']},
        component: UnitsView,
    },
    {
        path: '/admin/inventory/ingredients',
        name: 'Ingredients',
        meta: {title: 'Ингредиенты', requiresAuth: true, roles: ['ADMIN', 'OWNER', 'COOK', 'CASHIER']},
        component: IngredientsView,
    },
    {
        path: '/admin/inventory/stock',
        name: 'Stock',
        meta: {title: 'Остатки', requiresAuth: true, roles: ['ADMIN', 'OWNER', 'COOK', 'CASHIER']},
        component: StockView,
    },
    {
        path: '/admin/inventory/supplies',
        name: 'Supplies',
        meta: {title: 'Поставки', requiresAuth: true, roles: ['ADMIN', 'OWNER']},
        component: SuppliesView,
    },
    {
        path: '/admin/inventory/suppliers',
        name: 'Suppliers',
        meta: {title: 'Поставщики', requiresAuth: true, roles: ['ADMIN', 'OWNER']},
        component: SuppliersView,
    },
    {
        path: '/brands/:brandId/tags',
        name: 'BrandTags',
        meta: {title: 'Управление тегами', requiresAuth: true, roles: ['ADMIN', 'OWNER']},
        component: TagsView,
    },
    {
        path: '/kitchen',
        name: 'Kitchen',
        meta: {title: 'Кухня', requiresAuth: true, roles: ['COOK']},
        component: KitchenView,
    },
    {
        path: '/cashier',
        name: 'Cashier',
        meta: {title: 'Касса', requiresAuth: true, roles: ['CASHIER']},
        component: CashierView,
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore();

    // Сохраняем последнюю страницу (кроме явных auth-роутов)
    const exclude = new Set(['Login', 'Register']);
    if (from?.name && !exclude.has(from.name)) {
        localStorage.setItem('last_path', from.fullPath || '/');
    }

    // Если нет accessToken – пробуем восстановиться 1 раз перед проверками
    if (!authStore.accessToken && !authStore.isRestoringSession) {
        try { await authStore.restoreSession(); } catch (_) {}
    }

    // Гидратируем профиль/роли из localStorage, если они ещё не загружены
    if (!authStore.user) {
        try { authStore.hydrateFromStorage(); } catch (_) {}
    }

    // Проверка на необходимость аутентификации
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        return next('/login');
    }

    // Проверка на наличие требуемых ролей (используем роли из JWT в store)
    if (to.meta.roles) {
        const roles = Array.isArray(authStore.roles) ? authStore.roles : [];
        const hasRequiredRole = to.meta.roles.some(role => roles.includes(role));
        if (!hasRequiredRole) {
            return next({ name: 'Home' });
        }
    }

    next(); // Разрешаем переход
});

// После каждого перехода сохраняем текущий путь как последний
router.afterEach((to) => {
    const exclude = new Set(['Login', 'Register']);
    if (to?.name && !exclude.has(to.name)) {
        localStorage.setItem('last_path', to.fullPath || '/');
    }
});

export default router;
