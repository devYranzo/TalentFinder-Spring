
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
  isRenaming: {
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
      aria-labelledby="renameFolderModalTitle"
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
          <h5 id="renameFolderModalTitle" class="modal-title fw-semibold">
            <i class="bi bi-pencil me-2 text-primary"></i>
            Renombrar carpeta
          </h5>

          <button
              :disabled="isRenaming"
              class="btn-close"
              type="button"
              @click="emit('close')"
          ></button>
        </div>

        <!-- CUERPO -->
        <form @submit.prevent="emit('submit')">
          <div class="modal-body">

            <label
                for="renameFolderName"
                class="form-label fw-semibold"
            >
              Nombre de la carpeta
            </label>

            <input
                id="renameFolderName"
                :value="folderName"
                :disabled="isRenaming"
                class="form-control"
                maxlength="100"
                autofocus
                required
                type="text"
                placeholder="Nombre de la carpeta"
                @input="emit('update:folderName', $event.target.value)"
            />

          </div>

          <!-- FOOTER -->
          <div class="modal-footer">
            <button
                :disabled="isRenaming"
                class="btn btn-outline-secondary"
                type="button"
                @click="emit('close')"
            >
              Cancelar
            </button>

            <button
                :disabled="!folderName.trim() || isRenaming"
                class="btn btn-primary"
                type="submit"
            >
              <span
                  v-if="isRenaming"
                  class="spinner-border spinner-border-sm me-2"
              ></span>

              <i v-else class="bi bi-check-lg me-1"></i>

              {{ isRenaming ? 'Guardando...' : 'Guardar cambios' }}
            </button>
          </div>
        </form>

      </div>
    </div>
  </div>
</template>
