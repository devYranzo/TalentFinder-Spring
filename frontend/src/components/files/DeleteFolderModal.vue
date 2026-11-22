
<script setup>
defineProps({
  show: {
    type: Boolean,
    default: false
  },
  folder: {
    type: Object,
    default: null
  },
  isDeleting: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  'close',
  'confirm'
]);
</script>

<template>
  <div
      v-if="show"
      aria-labelledby="deleteFolderModalTitle"
      aria-modal="true"
      class="modal fade show d-block"
      role="dialog"
      style="background-color: rgba(0,0,0,.5)"
      tabindex="-1"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content">

        <!-- CABECERA -->
        <div class="modal-header">
          <h5 id="deleteFolderModalTitle" class="modal-title fw-semibold">
            <i class="bi bi-trash me-2 text-danger"></i>
            Eliminar carpeta
          </h5>

          <button
              :disabled="isDeleting"
              class="btn-close"
              type="button"
              @click="emit('close')"
          ></button>
        </div>

        <!-- CUERPO -->
        <div class="modal-body">

          <div class="text-center py-3">
            <i class="bi bi-exclamation-triangle display-3 text-warning"></i>

            <h3 class="h6 fw-bold mt-3">
              ¿Estás seguro de que quieres eliminar esta carpeta?
            </h3>

            <p class="text-secondary small">
              Esta acción puede afectar a los documentos que contiene.
            </p>
          </div>

          <div
              v-if="folder"
              class="alert alert-light border d-flex align-items-center gap-2 mb-0"
          >
            <i class="bi bi-folder-fill fs-3 text-warning"></i>

            <span class="small fw-semibold text-break">
              {{ folder.name }}
            </span>
          </div>

        </div>

        <!-- FOOTER -->
        <div class="modal-footer">
          <button
              :disabled="isDeleting"
              class="btn btn-outline-secondary"
              type="button"
              @click="emit('close')"
          >
            Cancelar
          </button>

          <button
              :disabled="isDeleting || !folder"
              class="btn btn-danger"
              type="button"
              @click="emit('confirm')"
          >
            <span
                v-if="isDeleting"
                class="spinner-border spinner-border-sm me-2"
            ></span>

            <i v-else class="bi bi-trash me-1"></i>

            {{ isDeleting ? 'Eliminando...' : 'Eliminar carpeta' }}
          </button>
        </div>

      </div>
    </div>
  </div>
</template>
