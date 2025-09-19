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
import TagsView from '../views/TagsView.vue';
import ProfileEditView from '../views/ProfileEditView.vue';
import StaffManagementView from '../views/StaffManagementView.vue';

const routes = [
    {
        path: '/',
        name: 'Home',
        meta: { title: 'Главная' },
        component: HomeView,
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
        path: '/admin',
        name: 'Admin',
        meta: { title: 'Админ панель', requiresAuth: true, requiresAdmin: true },
        component: AdminView,
    },
    {
        path: '/staff',
        name: 'StaffManagement',
        meta: { title: 'Управление персоналом', requiresAuth: true, roles: ['ADMIN', 'OWNER'] },
        component: StaffManagementView,
    },
    {
        path: '/brands/:brandId/tags',
        name: 'BrandTags',
        meta: { title: 'Управление тегами', requiresAuth: true },
        component: TagsView,
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

    // Проверка на необходимость аутентификации
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        return next('/login');
    }

    // Проверка на наличие требуемых ролей
    if (to.meta.roles) {
        const userRoles = authStore.user?.roles || [];
        const hasRequiredRole = to.meta.roles.some(role => userRoles.includes(role));

        if (!hasRequiredRole) {
            // Если у пользователя нет нужной роли, перенаправляем на главную
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
