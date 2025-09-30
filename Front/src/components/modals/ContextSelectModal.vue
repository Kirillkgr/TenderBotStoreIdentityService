<template>
  <teleport to="body">
    <transition name="fade-scale">
      <div v-if="visible" aria-modal="true" class="ctx-overlay" role="dialog" @click="close">
        <div class="ctx-card" @click.stop>
          <header class="ctx-header">
            <h3>Выбор контекста</h3>
            <button aria-label="Закрыть" class="ctx-close" type="button" @click="close">×</button>
          </header>

          <div v-if="!memberships.length" class="ctx-empty">
            У вас пока нет доступных рабочих пространств. Обратитесь к администратору.
          </div>

          <ul v-else class="ctx-list">
            <li v-for="m in memberships" :key="m.membershipId" class="ctx-item">
              <div class="ctx-info">
                <div class="ctx-line"><b>Master:</b> <span>{{ m.masterName || m.masterId || '—' }}</span></div>
                <div class="ctx-line"><b>Бренд:</b> <span>{{ m.brandName || m.brandId || '—' }}</span></div>
                <div class="ctx-line"><b>Локация:</b> <span>{{ m.locationName || m.locationId || '—' }}</span></div>
                <div class="ctx-line ctx-meta">
                  <span class="badge">{{ m.role || 'USER' }}</span>
                  <span class="badge badge--muted">{{ m.status || 'ACTIVE' }}</span>
                </div>
              </div>
              <div class="ctx-actions">
                <button class="btn-primary" type="button" @click="choose(m)">Выбрать</button>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import {computed} from 'vue';
import {useAuthStore} from '../../store/auth';

const props = defineProps({
  visible: {type: Boolean, default: false},
});
const emit = defineEmits(['close']);

const authStore = useAuthStore();
const memberships = computed(() => authStore.memberships || []);

function close() {
  emit('close');
}

async function choose(m) {
  try {
    await authStore.selectMembership(m);
  } finally {
    close();
  }
}
</script>

<style scoped>
.ctx-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100000;
}

.ctx-card {
  background: #1f1f1f;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  width: min(92vw, 680px);
  max-height: 85vh;
  overflow: auto;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
}

.ctx-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.ctx-close {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 1.25rem;
  cursor: pointer;
}

.ctx-empty {
  padding: 16px;
  opacity: 0.85;
}

.ctx-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.ctx-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.06);
}

.ctx-info {
  display: grid;
  gap: 4px;
}

.ctx-line {
  font-size: 0.95rem;
}

.ctx-meta {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.badge {
  background: #2e7d32;
  color: #fff;
  font-size: 0.75rem;
  padding: 2px 6px;
  border-radius: 6px;
}

.badge--muted {
  background: #424242;
}

.ctx-actions {
  display: flex;
  align-items: center;
}

.btn-primary {
  background: #3498db;
  border: 1px solid #3498db;
  color: #fff;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.btn-primary:hover {
  background: #2980b9;
  border-color: #2980b9;
}

/* simple transition */
.fade-scale-enter-active, .fade-scale-leave-active {
  transition: all .15s ease;
}

.fade-scale-enter-from, .fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.98);
}
</style>
