<template>
  <div
    class="modal-backdrop"
    v-if="isModalVisible"
    :style="backdropStyle"
    @click.self="handleBackdropClick"
  >
    <div
      class="modal"
      ref="modal"
      :class="{ 'is-dragging': isDragging }"
      :style="modalStyle"
      @mousedown.capture="emitFocus"
    >
      <div class="modal-header" @mousedown="startDrag" @touchstart.prevent="startDrag">

        <slot name="header">
          <h3>Вход</h3>
        </slot>
      </div>
      <div class="modal-body">
        <slot name="content"></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue';

const emit = defineEmits(['close','focus']);

const close = () => {
  emit('close');
  // Reset position when the modal is closed, so it reopens in the center.
  position.value = { x: 0, y: 0 };
};

const props = defineProps({
  isModalVisible: {
    type: Boolean,
    default: false
  },
  // Optional dimensions; if not provided, fallback to CSS defaults
  width: {
    type: [String, Number],
    default: null
  },
  height: {
    type: [String, Number],
    default: null
  },
  // Allow consumer to disable closing by clicking on backdrop area
  closeOnBackdrop: {
    type: Boolean,
    default: true
  },
  // Allow consumer to disable closing with ESC
  closeOnEsc: {
    type: Boolean,
    default: true
  },
  // z-index to support stacked modals
  zIndex: {
    type: Number,
    default: 9999
  },
  // Make backdrop fully transparent (no dimming)
  transparentBackdrop: {
    type: Boolean,
    default: false
  },
  // Visual offset for stacked modals (px)
  offsetX: {
    type: Number,
    default: 0
  },
  offsetY: {
    type: Number,
    default: 0
  }
});

const modal = ref(null);
// This is the proven working logic from the radical test, adapted for the modal.
const isDragging = ref(false);
const position = ref({ x: 0, y: 0 });
let startPos = { x: 0, y: 0 };

const modalStyle = computed(() => {
  const { x, y } = position.value;
  const scale = isDragging.value ? 1.02 : 1;
  const style = {
    transform: `translate(calc(-50% + ${x + (props.offsetX || 0)}px), calc(-50% + ${y + (props.offsetY || 0)}px)) scale(${scale})`,
    zIndex: String(props.zIndex)
  };
  if (props.width != null) {
    style.width = typeof props.width === 'number' ? `${props.width}px` : props.width;
  }
  if (props.height != null) {
    style.height = typeof props.height === 'number' ? `${props.height}px` : props.height;
  }
  return style;
});

const backdropStyle = computed(() => ({
  zIndex: String(props.zIndex - 1),
  backgroundColor: props.transparentBackdrop ? 'transparent' : 'var(--overlay)',
  pointerEvents: props.closeOnBackdrop ? 'auto' : 'none'
}));

function handleBackdropClick() {
  if (props.closeOnBackdrop) close();
}

function emitFocus() {
  emit('focus');
}

const onDrag = (event) => {
  if (!isDragging.value || !modal.value) return;

  const moveEvent = event.touches ? event.touches[0] : event;
  let newX = moveEvent.clientX - startPos.x;
  let newY = moveEvent.clientY - startPos.y;

  const modalRect = modal.value.getBoundingClientRect();
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;

  // Boundaries are calculated relative to the center-based transform
  const minX = -(viewportWidth / 2) + (modalRect.width / 2);
  const maxX = (viewportWidth / 2) - (modalRect.width / 2);
  const minY = -(viewportHeight / 2) + (modalRect.height / 2);
  const maxY = (viewportHeight / 2) - (modalRect.height / 2);

  // Clamp the position
  position.value.x = Math.max(minX, Math.min(newX, maxX));
  position.value.y = Math.max(minY, Math.min(newY, maxY));
};

const stopDrag = () => {
  if (!isDragging.value) return;
  isDragging.value = false;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
  document.removeEventListener('touchmove', onDrag);
  document.removeEventListener('touchend', stopDrag);
};

const startDrag = (event) => {
  const isTouchEvent = !!event.touches;
  if (!isTouchEvent && event.button !== 0) return;

  isDragging.value = true;
  const moveEvent = isTouchEvent ? event.touches[0] : event;

  // This logic is now identical to the working red square
  startPos.x = moveEvent.clientX - position.value.x;
  startPos.y = moveEvent.clientY - position.value.y;

  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
  document.addEventListener('touchmove', onDrag, { passive: false });
  document.addEventListener('touchend', stopDrag);
};

onUnmounted(() => {
  stopDrag();
  window.removeEventListener('keydown', onKeyDown);
});

function onKeyDown(e) {
  if (e.key === 'Escape' && props.closeOnEsc) {
    e.stopPropagation();
    close();
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeyDown);
});
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: var(--overlay);
  z-index: 9998; /* will be overridden by inline style */
  /* This is the new centering method */
  display: flex;
  align-items: center;
  justify-content: center;
  /* allow clicks to pass through backdrop so user can open additional modals */
  pointer-events: none;
}

.modal {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 9999; /* will be overridden by inline style */

  width: 380px;
  max-width: 95vw;
  border-radius: 18px;

  /* Themed surface */
  background-color: var(--card);
  border: 1px solid var(--card-border);
  box-shadow: 0 20px 48px var(--shadow-color);
  color: var(--text);
  
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  user-select: none;
  /* re-enable events for the modal content */
  pointer-events: auto;
}

.modal.is-dragging {
  cursor: grabbing;
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.25);
  opacity: 0.9;
  transition: none;
}

.modal-header {
  padding: 20px 24px 16px;
  text-align: center;
  flex-shrink: 0;
  cursor: move;
  position: relative;
}

.modal-header > *,
.modal-body > * {
  cursor: default;
}

.modal-body {
  padding: 20px;
  flex-grow: 1;
  text-align: center; /* Center content like the form */
}



.modal-header h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
}



.modal-body {
  padding: 0 24px 24px;
  overflow: visible; /* внешний скролл выключен, прокрутка только у внутренних областей */
  flex-grow: 1;
}
</style>
