<template>
  <teleport to="body">
    <transition name="modal-fade">
      <div v-if="modelValue" class="modal-overlay" @click.self="close">
        <div class="modal-window" :class="{ square }" :style="{ top: position.top + 'px', left: position.left + 'px' }" ref="windowRef">
          <div class="modal-header" :class="{ square, 'band-primary': primaryHeader }" @mousedown="onDragStart">
            <slot name="title">
              <h3 class="modal-title">{{ title }}</h3>
            </slot>
            <button class="modal-close" @click="close" aria-label="Закрыть">×</button>
          </div>
          <div class="modal-body">
            <slot />
          </div>
          <div v-if="$slots.footer" class="modal-footer" :class="{ square }">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import {onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue';

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, default: '' },
  width: { type: Number, default: 800 },
  square: { type: Boolean, default: false },
  primaryHeader: { type: Boolean, default: false },
});
const emit = defineEmits(['update:modelValue']);

const windowRef = ref(null);
const position = reactive({ top: 80, left: 0 });
let drag = null;

function center() {
  const w = window.innerWidth;
  position.left = Math.max(16, (w - (props.width || 800)) / 2);
}

function onDragStart(e) {
  const rect = windowRef.value?.getBoundingClientRect();
  if (!rect) return;
  drag = {
    startX: e.clientX,
    startY: e.clientY,
    startTop: rect.top,
    startLeft: rect.left,
  };
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', onDragEnd);
}
function onDrag(e) {
  if (!drag) return;
  position.top = Math.max(16, drag.startTop + (e.clientY - drag.startY));
  position.left = Math.max(16, drag.startLeft + (e.clientX - drag.startX));
}
function onDragEnd() {
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', onDragEnd);
  drag = null;
}

function close() {
  emit('update:modelValue', false);
}

watch(() => props.modelValue, (open) => {
  if (open) {
    center();
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
});

onMounted(() => {
  center();
  window.addEventListener('resize', center);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', center);
  document.body.style.overflow = '';
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.55);
  display: grid;
  place-items: center;
  z-index: 10000;
}
.modal-window {
  position: absolute;
  width: min(96vw, v-bind(width)px);
  max-width: 1000px;
  background: var(--card, #1e1e1e);
  color: var(--text, #fff);
  border: 1px solid var(--card-border, #333);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,.08);
}
.modal-window.square { border-radius: 0; }
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: .75rem 1rem;
  cursor: move;
  user-select: none;
  background: var(--surface, #2a2a2a);
  border-bottom: 1px solid var(--border, #333);
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
}
.modal-header.band-primary { background: var(--primary, #3498db); color: #fff; }
.modal-header.square { border-top-left-radius: 0; border-top-right-radius: 0; }
.modal-title { margin: 0; font-size: 1.1rem; line-height: 1.2; font-weight: 600; }
.modal-close { background: transparent; border: none; color: #bbb; font-size: 20px; cursor: pointer; }
.modal-header.band-primary .modal-close { color: #fff; }
.modal-header.band-primary .modal-close:hover { color: var(--error, #ff5a5f); }
.modal-body { padding: 1rem; max-height: 80vh; overflow: auto; }
.modal-footer { padding: .75rem 1rem; border-top: 1px solid var(--border, #333); display: flex; gap: 16px; justify-content: flex-end; }
.modal-footer.square { border-bottom-left-radius: 0; border-bottom-right-radius: 0; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity .18s ease, transform .18s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-from .modal-window { transform: scale(.95); }
.modal-fade-enter-to .modal-window { transform: scale(1); }
.modal-fade-leave-from .modal-window { transform: scale(1); }
.modal-fade-leave-to .modal-window { transform: scale(.98); opacity: 0; }
</style>
