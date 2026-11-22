
<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  document: {
    type: Object,
    default: null
  },
  folders: {
    type: Array,
    default: () => []
  },
  currentFolderId: {
    type: [Number, String],
    default: null
  },
  isMoving: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  'close',
  'submit'
]);

const selectedFolderId = ref(null);

watch(() => props.show, (visible) => {
  if (visible) {
    selectedFolderId.value = null;
  }
});

const handleSubmit = () => {
  emit('submit', selectedFolderId.value);
};

const isCurrentFolder = (folderId) => {
  if (folderId === null) {
    return props.currentFolderId === null;
  }

  return String(folderId) === String(props.currentFolderId);
};
</script>

<template>
  <div
      v-if="show"
      aria-labelledby="moveDocumentModalTitle"
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
          <h5 id="moveDocumentModalTitle" class="modal-title fw-semibold">
            <i class="bi bi-folder-symlink me-2 text-primary"></i>
            Mover documento
          </h5>

          <button
              :disabled="isMoving"
              class="btn-close"
              type="button"
              @click="emit('close')"
          ></button>
        </div>

        <!-- CUERPO -->
        <div class="modal-body">

          <p class="mb-3">
            Selecciona dónde quieres guardar el documento:
          </p>

          <div
              v-if="document"
              class="alert alert-light border d-flex align-items-center gap-2 mb-3"
          >
            <i class="bi bi-file-earmark-pdf-fill fs-3 text-danger"></i>

            <span class="small fw-semibold text-break">
              {{ document.filename }}
            </span>
          </div>

          <label
              for="moveDocumentFolder"
              class="form-label fw-semibold"
          >
            Carpeta de destino
          </label>

          <select
              id="moveDocumentFolder"
              v-model="selectedFolderId"
              class="form-select"
              :disabled="isMoving"
          >
            <option
                :value="null"
                :disabled="isCurrentFolder(null)"
            >
              General
              {{ isCurrentFolder(null) ? '(ubicación actual)' : '' }}
            </option>

            <option
                v-for="folder in folders"
                :key="folder.id"
                :value="folder.id"
                :disabled="isCurrentFolder(folder.id)"
            >
              {{ folder.name }}
              {{ isCurrentFolder(folder.id) ? '(ubicación actual)' : '' }}
            </option>
          </select>

          <div class="form-text">
            El documento dejará de aparecer en su ubicación actual.
          </div>

        </div>

        <!-- FOOTER -->
        <div class="modal-footer">
          <button
              :disabled="isMoving"
              class="btn btn-outline-secondary"
              type="button"
              @click="emit('close')"
          >
            Cancelar
          </button>

          <button
              :disabled="isMoving || isCurrentFolder(selectedFolderId)"
              class="btn btn-primary"
              type="button"
              @click="handleSubmit"
          >
            <span
                v-if="isMoving"
                class="spinner-border spinner-border-sm me-2"
            ></span>

            <i v-else class="bi bi-folder-symlink me-1"></i>

            {{ isMoving ? 'Moviendo...' : 'Mover documento' }}
          </button>
        </div>

      </div>
    </div>
  </div>
</template>
