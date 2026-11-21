
<script setup>
defineProps({
  show: {
    type: Boolean,
    default: false
  },
  folderName: {
    type: String,
    default: ''
  },
  isCreating: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  'update:folderName',
  'close',
  'submit'
]);
</script>

<template>
  <div
      v-if="show"
      aria-labelledby="createFolderModalTitle"
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
          <h5 id="createFolderModalTitle" class="modal-title fw-semibold">
            <i class="bi bi-folder-plus me-2 text-primary"></i>
            Crear nueva carpeta
          </h5>

          <button
              :disabled="isCreating"
              class="btn-close"
              type="button"
              @click="emit('close')"
          ></button>
        </div>

        <!-- CUERPO -->
        <form @submit.prevent="emit('submit')">
          <div class="modal-body">
            <div class="mb-3">
              <label
                  for="newFolderName"
                  class="form-label fw-semibold"
              >
                Nombre de la carpeta
              </label>

              <input
                  id="newFolderName"
                  :value="folderName"
                  :disabled="isCreating"
                  autofocus
                  class="form-control"
                  maxlength="100"
                  placeholder="Ej. Candidatos seleccionados"
                  type="text"
                  @input="emit('update:folderName', $event.target.value)"
              />

              <div class="form-text">
                Introduce un nombre para identificar y organizar tus documentos.
              </div>
            </div>
          </div>

          <!-- FOOTER -->
          <div class="modal-footer">
            <button
                :disabled="isCreating"
                class="btn btn-outline-secondary"
                type="button"
                @click="emit('close')"
            >
              Cancelar
            </button>

            <button
                :disabled="!folderName.trim() || isCreating"
                class="btn btn-primary"
                type="submit"
            >
              <span
                  v-if="isCreating"
                  class="spinner-border spinner-border-sm me-2"
              ></span>

              <i v-else class="bi bi-folder-plus me-1"></i>

              {{ isCreating ? 'Creando...' : 'Crear carpeta' }}
            </button>
          </div>
        </form>

      </div>
    </div>
  </div>
</template>
