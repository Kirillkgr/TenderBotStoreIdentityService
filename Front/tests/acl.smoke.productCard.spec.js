import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/vue';
import {createPinia, setActivePinia} from 'pinia';
import ProductCard from '@/components/ProductCard.vue';

vi.mock('vue-toastification', () => ({useToast: () => ({info: vi.fn(), success: vi.fn(), error: vi.fn()})}));

// Mock cart store to intercept addProduct
vi.mock('@/store/cart', () => {
    const addProduct = vi.fn(async () => ({ok: true}));
    const clearServerCart = vi.fn(async () => {
    });
    return {
        useCartStore: () => ({addProduct, clearServerCart, items: []}),
    };
});

// Mock auth store: guest vs user
vi.mock('@/store/auth', async (orig) => {
    const actual = await orig();
    return {
        ...actual,
        useAuthStore: () => ({isAuthenticated: false})
    }
});

function renderCard(product, extraProps = {}) {
    setActivePinia(createPinia());
    return render(ProductCard, {
        props: {product, ...extraProps},
        global: {
            stubs: {'router-link': {template: '<a><slot /></a>'}}
        }
    });
}

describe('ACL Smoke: ProductCard', () => {
    beforeEach(() => vi.resetModules());

    it('Add to Cart is visible and clickable for guest', async () => {
        const product = {id: 1, name: 'Cola', price: 100};
        renderCard(product);
        const btn = await screen.findByRole('button', {name: 'Добавить в корзину'});
        expect(btn).toBeTruthy();
        await fireEvent.click(btn);
        // If no error thrown, success; cart store mock handled
    });

    it('Add to Cart remains when hideAddToCart=false and disappears when true', async () => {
        const product = {id: 2, name: 'Burger', price: 200};
        const {rerender} = renderCard(product, {hideAddToCart: false});
        expect(screen.queryByRole('button', {name: 'Добавить в корзину'})).toBeTruthy();
        await rerender({product, hideAddToCart: true});
        expect(screen.queryByRole('button', {name: 'Добавить в корзину'})).toBeNull();
    });
});
