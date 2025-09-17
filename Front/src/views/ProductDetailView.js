import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useProductStore } from '../store/product';
import { useCartStore } from '../store/cart';
import { useToast } from 'vue-toastification';

export default {
  setup() {
    const route = useRoute();
    const productStore = useProductStore();
    const cartStore = useCartStore();
    const toast = useToast();
    const mainImage = ref(null);

    onMounted(async () => {
      const productId = route.params.id;
      await productStore.fetchProductDetails(productId);
      if (productStore.selectedProduct && productStore.selectedProduct.images?.length > 0) {
        mainImage.value = productStore.selectedProduct.images[0];
      }
    });

    async function handleAddToCart() {
      const product = productStore.selectedProduct;
      if (!product) return;
      try {
        await cartStore.addItem(product.id, 1);
        toast.success(`'${product.name}' добавлен в корзину!`);
      } catch (error) {
        toast.error('Не удалось добавить товар. Возможно, нужно войти в систему.');
      }
    }

    return {
      productStore,
      mainImage,
      handleAddToCart,
    };
  },
};
