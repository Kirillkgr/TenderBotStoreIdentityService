<template>
  <div class="modal-backdrop" v-if="isModalVisible" @click.self="close">
    <div class="modal" ref="modal" :class="{ 'is-dragging': isDragging }" :style="modalStyle">
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
import { ref, computed, onUnmounted } from 'vue';

const emit = defineEmits(['close']);

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
    transform: `translate(calc(-50% + ${x}px), calc(-50% + ${y}px)) scale(${scale})`
  };
  if (props.width != null) {
    style.width = typeof props.width === 'number' ? `${props.width}px` : props.width;
  }
  if (props.height != null) {
    style.height = typeof props.height === 'number' ? `${props.height}px` : props.height;
  }
  return style;
});

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
});
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5); /* Added for visual clarity */
  z-index: 9998;
  /* This is the new centering method */
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 9999;

  width: 380px;
  max-width: 95vw;
  border-radius: 18px;
  
  /* Minimalist Light Style */
  background-color: var(--surface-main);
  border: 1px solid var(--border-main);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  color: var(--text-primary);
  
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  user-select: none;
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

.close-btn {
  position: absolute;
  top: 10px;
  right: 14px;
  border: none;
  background: none;
  font-size: 28px;
  font-weight: 300;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s ease;
  line-height: 1;
  padding: 0;
}

.close-btn:hover {
  color: var(--text-primary);
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
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}



.modal-body {
  padding: 0 24px 24px;
  overflow-y: auto;
  flex-grow: 1;
}
</style>
