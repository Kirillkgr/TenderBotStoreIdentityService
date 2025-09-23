import {onMounted, ref} from 'vue';
import {useProductStore} from '../store/product';
import ProductCard from '../components/ProductCard.vue';

export default {
  components: {
    ProductCard,
  },
  setup() {
    const productStore = useProductStore();
    const selectedCategoryId = ref(null);

    onMounted(() => {
      productStore.fetchCategories();
      productStore.fetchProducts();
    });

    function selectCategory(categoryId) {
      selectedCategoryId.value = categoryId;
      productStore.fetchProducts(categoryId);
    }

    return {
      productStore,
      selectedCategoryId,
      selectCategory,
    };
  },
};
