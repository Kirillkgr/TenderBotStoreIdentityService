import { ref, onMounted } from 'vue';
import { getBrands, getTagGroupsByBrandId, createBrand, createGroup, createProduct } from '../services/brandService';
import { useToast } from 'vue-toastification';

import CreateBrandModal from '../components/modals/CreateBrandModal.vue';
import CreateGroupModal from '../components/modals/CreateGroupModal.vue';
import CreateProductModal from '../components/modals/CreateProductModal.vue';

export default {
  components: {
    CreateBrandModal,
    CreateGroupModal,
    CreateProductModal,
  },
  setup() {
    const brands = ref([]);
    const selectedBrand = ref('');
    const tagGroups = ref([]);
    const selectedGroup = ref(null);
    const loading = ref(false);
    const error = ref(null);
    const toast = useToast();

    const showCreateBrandModal = ref(false);
    const showCreateGroupModal = ref(false);
    const showCreateProductModal = ref(false);

    const fetchBrands = async () => {
      loading.value = true;
      error.value = null;
      try {
        const response = await getBrands();
        brands.value = response.data;
      } catch (err) {
        console.error('Ошибка при загрузке брендов:', err);
        error.value = 'Не удалось загрузить список брендов.';
        toast.error(error.value);
      } finally {
        loading.value = false;
      }
    };

    onMounted(fetchBrands);

    const onBrandSelect = async () => {
      selectedGroup.value = null;
      if (!selectedBrand.value) {
        tagGroups.value = [];
        return;
      }
      
      loading.value = true;
      error.value = null;
      try {
        const response = await getTagGroupsByBrandId(selectedBrand.value);
        tagGroups.value = response.data;
      } catch (err) {
        console.error(`Ошибка при загрузке групп тегов для бренда ${selectedBrand.value}:`, err);
        error.value = 'Не удалось загрузить группы тегов.';
        tagGroups.value = [];
      } finally {
        loading.value = false;
      }
    };

    const handleCreateBrand = async (brandData) => {
      try {
        await createBrand(brandData);
        toast.success(`Бренд "${brandData.brandName}" успешно создан!`);
        showCreateBrandModal.value = false;
        await fetchBrands();
      } catch (err) {
        console.error('Ошибка при создании бренда:', err);
        toast.error('Не удалось создать бренд.');
      }
    };

    const handleCreateGroup = async (groupData) => {
      try {
        await createGroup(groupData);
        toast.success(`Группа "${groupData.groupName}" успешно создана!`);
        showCreateGroupModal.value = false;
        if (selectedBrand.value) {
          await onBrandSelect();
        }
      } catch (err) {
        console.error('Ошибка при создании группы:', err);
        toast.error('Не удалось создать группу.');
      }
    };

    const handleCreateProduct = async (productData) => {
      try {
        await createProduct(productData);
        toast.success(`Продукт "${productData.name}" успешно создан!`);
        showCreateProductModal.value = false;
      } catch (err) {
        console.error('Ошибка при создании продукта:', err);
        toast.error('Не удалось создать продукт.');
      }
    };

    const handleCreateProduct = async (productData) => {
      try {
        await createProduct(productData);
        toast.success(`Продукт "${productData.name}" успешно создан!`);
        showCreateProductModal.value = false;
      } catch (err) {
        console.error('Ошибка при создании продукта:', err);
        toast.error('Не удалось создать продукт.');
      }
    };

    return {
      brands,
      selectedBrand,
      tagGroups,
      selectedGroup,
      loading,
      error,
      showCreateBrandModal,
      showCreateGroupModal,
      showCreateProductModal,
      onBrandSelect,
      handleCreateBrand,
      handleCreateGroup,
      handleCreateProduct,
    };
      tagGroups,
      selectedGroup,
      loading,
      error,
      showCreateBrandModal,
      showCreateGroupModal,
      showCreateProductModal,
      fetchBrands,
      onBrandSelect,
      handleCreateBrand,
      handleCreateGroup,
      handleCreateProduct,
    };
  },
};
