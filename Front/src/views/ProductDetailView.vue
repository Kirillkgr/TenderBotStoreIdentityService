<template>
  <div class="product-detail-container">
    <div v-if="productStore.loading">Загрузка...</div>
    <div v-else-if="!productStore.selectedProduct">Товар не найден.</div>
    <div v-else class="product-content">
      <div class="product-images">
        <img :src="mainImage || '/images/placeholder.png'" alt="Основное изображение товара" class="main-image" />
        <div class="thumbnail-images">
          <img
            v-for="(image, index) in productStore.selectedProduct.images"
            :key="index"
            :src="image"
            alt="Дополнительное изображение"
            @click="mainImage = image"
            :class="{ active: mainImage === image }"
          />
        </div>
      </div>
      <div class="product-info">
        <h1>{{ productStore.selectedProduct.name }}</h1>
        <p class="description">{{ productStore.selectedProduct.description }}</p>
        <p class="price">{{ productStore.selectedProduct.price }} ₽</p>
        <div class="rating">Рейтинг: {{ productStore.selectedProduct.rating }} ★</div>
        <div class="sizes">
          <span>Размеры:</span>
          <button v-for="size in productStore.selectedProduct.sizes" :key="size" class="size-btn">{{ size }}</button>
        </div>
        <button @click="handleAddToCart" class="add-to-cart-btn">Добавить в корзину</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useProductStore } from '../store/product';
import { useCartStore } from '../store/cart';
import { useToast } from 'vue-toastification';

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
</script>

<style scoped>
.product-detail-container {
  max-width: 1000px;
  margin: auto;
  padding: 2rem;
}

.product-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  align-items: start;
}

.main-image {
  width: 100%;
  border-radius: 8px;
}

.thumbnail-images {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.thumbnail-images img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  cursor: pointer;
  border-radius: 4px;
  border: 2px solid transparent;
}

.thumbnail-images img.active {
  border-color: #42b983;
}

.product-info {
  text-align: left;
}

.price {
  font-size: 2rem;
  font-weight: bold;
  color: #42b983;
  margin: 1.5rem 0;
}

.sizes, .rating {
  margin-bottom: 1.5rem;
}

.size-btn {
  margin-right: 0.5rem;
  padding: 0.5rem 1rem;
}

.add-to-cart-btn {
  padding: 0.8rem 2rem;
  font-size: 1.1rem;
  width: 100%; /* Растягиваем кнопку на мобильных */
}

/* Адаптация для мобильных устройств */
@media (max-width: 768px) {
  .product-content {
    grid-template-columns: 1fr; /* Одна колонка */
  }

  .product-detail-container {
    padding: 1rem;
  }

  .price {
    font-size: 1.8rem;
  }

  h1 {
    font-size: 1.5rem;
  }
}
</style>
