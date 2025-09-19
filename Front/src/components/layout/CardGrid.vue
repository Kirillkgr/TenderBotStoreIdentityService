<template>
  <div class="card-grid" :style="gridStyle">
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  min: { type: Number, default: 200 },
  max: { type: Number, default: 260 },
  gap: { type: Number, default: 24 },
});

const gridStyle = computed(() => ({
  display: 'grid',
  gridTemplateColumns: `repeat(auto-fit, minmax(${props.min}px, ${props.max}px))`,
  justifyContent: 'center',
  gap: `${props.gap}px`,
}));
</script>

<style scoped>
.card-grid {
  width: 100%;
}
</style>
