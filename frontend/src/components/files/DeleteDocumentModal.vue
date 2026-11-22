
<script setup>
defineProps({
  show: {
    type: Boolean,
    default: false
  },
  document: {
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
      aria-labelledby="deleteDocumentModalTitle"
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
          <h5 id="deleteDocumentModalTitle" class="modal-title fw-semibold">
            <i class="bi bi-trash me-2 text-danger"></i>
            Eliminar documento
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
              ¿Estás seguro de que quieres eliminar este documento?
            </h3>

            <p class="text-secondary small mb-3">
              Esta acción no se puede deshacer.
            </p>
          </div>

          <div
              v-if="document"
              class="alert alert-light border d-flex align-items-center gap-2 mb-0"
          >
            <i class="bi bi-file-earmark-pdf-fill fs-3 text-danger"></i>

            <span class="small fw-semibold text-break">
              {{ document.filename }}
            </span>
          </div>

        </div>

        <!-- FOOTER -->
        <div class="modal-footer">
          <button
              :disabled="isDeleting"
              class="btn btn-secondary"
              type="button"
              @click="emit('close')"
          >
            Cancelar
          </button>

          <button
              :disabled="isDeleting || !document"
              class="btn btn-outline-danger"
              type="button"
              @click="emit('confirm')"
          >
            <span
                v-if="isDeleting"
                class="spinner-border spinner-border-sm me-2"
            ></span>

            <i v-else class="bi bi-trash me-1"></i>

            {{ isDeleting ? 'Eliminando...' : 'Eliminar' }}
          </button>
        </div>

      </div>
    </div>
  </div>
</template>
