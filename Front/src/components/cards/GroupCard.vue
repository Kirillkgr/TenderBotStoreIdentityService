<template>
  <div class="group-card" :title="group.name" @click="$emit('open', group)">
    <div class="group-image">
      <img v-if="group.imageUrl" :src="group.imageUrl" :alt="group.name" />
      <div v-else class="image-placeholder">{{ group.name?.charAt(0)?.toUpperCase() }}</div>
    </div>
    <div class="group-title">{{ group.name }}</div>
  </div>
</template>

<script setup>
const props = defineProps({
  group: { type: Object, required: true },
});
const emit = defineEmits(['open']);
</script>

<style scoped>
.group-card {
  background: var(--card);
  border: 1px solid var(--card-border);
  border-radius: 16px;
  box-shadow: 0 6px 16px var(--shadow-color);
  overflow: hidden;
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease;
  width: 200px;
}
.group-card:hover { transform: translateY(-3px); box-shadow: 0 10px 22px var(--shadow-color); border-color: var(--border); }
.group-image { aspect-ratio: 1 / 1; background: var(--surface, var(--input-bg)); display: flex; align-items: center; justify-content: center; }
.group-image img { width: 100%; height: 100%; object-fit: cover; }
.image-placeholder { font-size: 28px; font-weight: 800; color: var(--muted); letter-spacing: .5px; }
.group-title { text-align: center; padding: 12px 10px; color: var(--text); font-weight: 700; }
</style>
