
<script setup>
defineProps({
  isLoading: {
    type: Boolean,
    default: false
  },
  showCreateButton: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits([
  'refresh',
  'create-folder',
  'upload'
]);
</script>

<template>
  <div class="d-flex gap-2 mb-3">
    <button
        :disabled="isLoading"
        class="btn btn-outline-secondary"
        title="Actualizar"
        type="button"
        @click="emit('refresh')"
    >
      <span
          v-if="isLoading"
          class="spinner-border spinner-border-sm"
      ></span>

      <i v-else class="bi bi-arrow-clockwise"></i>
    </button>

    <button
        v-if="showCreateButton"
        class="btn btn-outline-primary"
        type="button"
        @click="emit('create-folder')"
    >
      <i class="bi bi-folder-plus me-1"></i>
      Nueva carpeta
    </button>

    <button
        class="btn btn-primary"
        type="button"
        @click="emit('upload')"
    >
      <i class="bi bi-cloud-arrow-up me-1"></i>
      Subir archivo
    </button>
  </div>
</template>
