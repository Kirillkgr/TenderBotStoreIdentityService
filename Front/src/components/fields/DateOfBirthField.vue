<template>
  <div class="form-group">
    <label :for="id">Дата рождения</label>
    <Field
      :id="id"
      name="dateOfBirth"
      v-slot="{ field, meta, errors }"
    >
      <input
        v-bind="field"
        :value="field.value"
        @input="e => { field.value = e.target.value; emit('update:modelValue', e.target.value) }"
        type="date"
        class="form-control"
        :class="{
          'is-invalid': errors.length > 0,
          'is-valid': meta.touched && errors.length === 0 && field.value
        }"
        placeholder="ДД.ММ.ГГГГ"
        autocomplete="bday"
      />
    </Field>
    <ErrorMessage name="dateOfBirth" class="error-message" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { Field, ErrorMessage } from 'vee-validate';

const props = defineProps({
  id: {
    type: String,
    default: 'dob-field',
  },
  modelValue: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['update:modelValue']);

const displayValue = computed(() => {
  // Display in DD.MM.YYYY
  if (!props.modelValue) return '';
  const val = props.modelValue.replace(/[^\d]/g, '');
  if (val.length < 3) return val;
  if (val.length < 5) return `${val.slice(0, 2)}.${val.slice(2)}`;
  return `${val.slice(0, 2)}.${val.slice(2, 4)}.${val.slice(4, 8)}`;
});

function onInput(e, field) {
  let val = e.target.value.replace(/[^\d]/g, '');
  if (val.length > 8) val = val.slice(0, 8);
  let formatted = val;
  if (val.length > 4) formatted = `${val.slice(0, 2)}.${val.slice(2, 4)}.${val.slice(4, 8)}`;
  else if (val.length > 2) formatted = `${val.slice(0, 2)}.${val.slice(2)}`;
  e.target.value = formatted;
  field.value = formatted;
  emit('update:modelValue', formatted);
}

function onBlur(e, field) {
  // Trigger validation on blur
  field.blur();
}
</script>

<style scoped>
.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.form-control {
  padding: 8px;
  border-radius: 6px;
  border: 1px solid #444;
  background: #222;
  color: #fff;
}
.form-control.is-invalid {
  border-color: #ff3b30;
}
.form-control.is-valid {
  border-color: #34c759;
}
.error-message {
  color: #ff6b6b;
  font-size: 0.9em;
}
</style>
