<template>
  <teleport to="body">
    <transition name="modal-fade">
      <div v-if="modelValue" :class="{ 'modal-overlay--nooverlay': noOverlay }" class="modal-overlay"
           @click.self="onOverlay">
        <div ref="windowRef" :class="{ square, 'pe-auto': noOverlay }"
             :style="{ top: position.top + 'px', left: position.left + 'px' }" class="modal-window">
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
import {nextTick, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue';

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, default: '' },
  width: { type: Number, default: 800 },
  square: { type: Boolean, default: false },
  primaryHeader: { type: Boolean, default: false },
  closeOnOverlay: {type: Boolean, default: true},
  noOverlay: {type: Boolean, default: false},
  lockBodyScroll: {type: Boolean, default: true},
});
const emit = defineEmits(['update:modelValue']);

const windowRef = ref(null);
const position = reactive({ top: 80, left: 0 });
let drag = null;

function clampToViewport(left, top) {
  const w = window.innerWidth;
  const h = window.innerHeight;
  const EDGE = 8; // небольшой постоянный отступ от краёв, чтобы не было "прилипания" и дрожания
  const modalW = (windowRef.value?.offsetWidth) || Math.min(w * 0.96, props.width || 800);
  const modalH = (windowRef.value?.offsetHeight) || 400;
  const minLeft = EDGE;
  const minTop = EDGE;
  const maxLeft = Math.max(EDGE, w - modalW - EDGE);
  const maxTop = Math.max(EDGE, h - modalH - EDGE);
  return {
    left: Math.min(Math.max(minLeft, left), maxLeft),
    top: Math.min(Math.max(minTop, top), maxTop),
  };
}

function center() {
  try {
    const w = window.innerWidth;
    const h = window.innerHeight;
    const modalW = (windowRef.value?.offsetWidth) || Math.min(w * 0.96, props.width || 800);
    const modalH = (windowRef.value?.offsetHeight) || 400;
    const left = (w - modalW) / 2;
    const top = (h - modalH) / 2;
    const p = clampToViewport(left, top);
    position.left = p.left;
    position.top = p.top;
  } catch (_) {
    position.left = 0;
    position.top = 0;
  }
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
  const rawTop = drag.startTop + (e.clientY - drag.startY);
  const rawLeft = drag.startLeft + (e.clientX - drag.startX);
  const p = clampToViewport(rawLeft, rawTop);
  position.top = p.top;
  position.left = p.left;
}
function onDragEnd() {
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', onDragEnd);
  drag = null;
}

function close() {
  emit('update:modelValue', false);
}

function onOverlay() {
  if (props.closeOnOverlay) close();
}

watch(() => props.modelValue, async (open) => {
  if (open) {
    await nextTick();
    center();
    if (props.lockBodyScroll) document.body.style.overflow = 'hidden';
  } else {
    if (props.lockBodyScroll) document.body.style.overflow = '';
  }
});

onMounted(async () => {
  await nextTick();
  center();
  window.addEventListener('resize', () => {
    // При изменении размера окна просто зажимаем текущую позицию в границах
    const p = clampToViewport(position.left, position.top);
    position.left = p.left;
    position.top = p.top;
  });
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

.modal-overlay.modal-overlay--nooverlay {
  background: transparent;
  pointer-events: none; /* не блокируем клики по странице */
}
.modal-window {
  position: absolute;
  width: min(100vw, v-bind(width)px);
  max-width: 1000px;
  background: var(--card, #1e1e1e);
  color: var(--text, #fff);
  border: 1px solid var(--card-border, #333);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,.08);
}

.pe-auto {
  pointer-events: auto;
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
.modal-close {
  background: #ff5a5f;
  border: none;
  color: #fff;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0,0,0,.25);
}
.modal-header.band-primary .modal-close { color: #fff; }
.modal-close:hover { filter: brightness(1.05); }
.modal-close:focus-visible {
  outline: 2px solid rgba(255,90,95,.6);
  outline-offset: 2px;
}
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
