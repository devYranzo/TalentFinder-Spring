
<script setup>
const emit = defineEmits([
  'create-folder',
  'upload'
]);
</script>

<template>
  <div
      class="flex-grow-1 d-flex flex-column justify-content-center align-items-center text-center py-5"
  >
    <i class="bi bi-folder2-open display-1 text-secondary mb-3"></i>

    <h2 class="h5 fw-bold mb-2">
      Tu explorador está vacío
    </h2>

    <p class="text-secondary mb-4">
      Todavía no tienes documentos ni carpetas.
      Empieza organizando tus currículums.
    </p>

    <div class="d-flex flex-wrap justify-content-center gap-2">
      <button
          class="btn btn-outline-primary"
          type="button"
          @click="emit('create-folder')"
      >
        <i class="bi bi-folder-plus me-1"></i>
        Crear carpeta
      </button>

      <button
          class="btn btn-primary"
          type="button"
          @click="emit('upload')"
      >
        <i class="bi bi-cloud-arrow-up me-1"></i>
        Subir documento
      </button>
    </div>
  </div>
</template>
