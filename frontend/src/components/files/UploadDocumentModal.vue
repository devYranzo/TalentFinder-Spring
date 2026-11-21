
<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  folderList: {
    type: Array,
    default: () => []
  },
  selectedFile: {
    type: Object,
    default: null
  },
  selectedFolder: {
    type: [Number, String],
    default: null
  },
  isUploading: {
    type: Boolean,
    default: false
  },
  isCreatingFolder: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  'update:selectedFile',
  'update:selectedFolder',
  'close',
  'submit'
]);

const fileInput = ref(null);

const formatSize = (bytes) => {
  if (!bytes) return '0 KB';

  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  return `${(bytes / 1024).toFixed(1)} KB`;
};

const onFileSelected = (event) => {
  const file = event.target.files?.[0] || null;

  if (file && file.type !== 'application/pdf') {
    emit('update:selectedFile', null);
    event.target.value = '';
    emit('invalid-file', 'Solo se permiten archivos PDF.');
    return;
  }

  if (file && file.size > 10 * 1024 * 1024) {
    emit('update:selectedFile', null);
    event.target.value = '';
    emit('invalid-file', 'El archivo no puede superar los 10 MB.');
    return;
  }

  emit('update:selectedFile', file);
};

watch(() => props.show, (visible) => {
  if (!visible && fileInput.value) {
    fileInput.value.value = '';
  }
});
</script>

<template>
  <div
      v-if="show"
      aria-labelledby="uploadModalTitle"
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
          <h5 id="uploadModalTitle" class="modal-title fw-semibold">
            <i class="bi bi-cloud-arrow-up me-2 text-primary"></i>
            Subir documento
          </h5>

          <button
              :disabled="isUploading || isCreatingFolder"
              class="btn-close"
              type="button"
              @click="emit('close')"
          ></button>
        </div>

        <!-- CUERPO -->
        <div class="modal-body">

          <!-- CARPETA DESTINO -->
          <div class="mb-3">
            <label class="form-label fw-semibold">
              Carpeta de destino
            </label>

            <select
                :value="selectedFolder"
                :disabled="isUploading"
                class="form-select"
                @change="emit('update:selectedFolder', $event.target.value === '' ? null : Number($event.target.value))"
            >
              <option value="">
                General
              </option>

              <option
                  v-for="folder in folderList"
                  :key="folder.id"
                  :value="folder.id"
              >
                {{ folder.name }}
              </option>
            </select>
          </div>

          <!-- ARCHIVO -->
          <div class="mb-3">
            <label class="form-label fw-semibold">
              Archivo PDF
            </label>

            <input
                ref="fileInput"
                :disabled="isUploading"
                accept=".pdf,application/pdf"
                class="form-control"
                type="file"
                @change="onFileSelected"
            />

            <div class="form-text">
              Solo archivos PDF. Tamaño máximo: 10 MB.
            </div>
          </div>

          <!-- ARCHIVO SELECCIONADO -->
          <div
              v-if="selectedFile"
              class="alert alert-info d-flex align-items-center gap-3 mb-0"
          >
            <i class="bi bi-file-earmark-pdf-fill fs-3 text-danger"></i>

            <div class="overflow-hidden">
              <div class="fw-semibold text-truncate">
                {{ selectedFile.name }}
              </div>

              <small>
                {{ formatSize(selectedFile.size) }}
              </small>
            </div>
          </div>

        </div>

        <!-- FOOTER -->
        <div class="modal-footer">
          <button
              :disabled="isUploading || isCreatingFolder"
              class="btn btn-outline-secondary"
              type="button"
              @click="emit('close')"
          >
            Cancelar
          </button>

          <button
              :disabled="!selectedFile || isUploading || isCreatingFolder"
              class="btn btn-primary"
              type="button"
              @click="emit('submit')"
          >
            <span
                v-if="isUploading"
                class="spinner-border spinner-border-sm me-2"
            ></span>

            <i v-else class="bi bi-upload me-1"></i>

            {{ isUploading ? 'Subiendo...' : 'Subir documento' }}
          </button>
        </div>

      </div>
    </div>
  </div>
</template>
