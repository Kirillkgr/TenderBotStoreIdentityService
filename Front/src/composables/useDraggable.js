import { ref, onUnmounted } from 'vue';

export function useDraggable(target) {
  const position = ref({ x: 0, y: 0 });

  function onMouseDown(e) {
    e.preventDefault();
    const startX = e.clientX - position.value.x;
    const startY = e.clientY - position.value.y;

    function onMouseMove(e) {
      const newX = e.clientX - startX;
      const newY = e.clientY - startY;

      // Тут можно добавить логику для ограничения движения в пределах экрана,
      // но для начала сделаем простое перемещение.
      position.value = { x: newX, y: newY };
    }

    function onMouseUp() {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    }

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }

  onUnmounted(() => {
    // На всякий случай, если компонент размонтируется во время перетаскивания
    document.removeEventListener('mousemove', () => {});
    document.removeEventListener('mouseup', () => {});
  });

  return {
    position,
    onMouseDown,
  };
}
