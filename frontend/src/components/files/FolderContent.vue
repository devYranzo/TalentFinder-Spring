<script setup>
import DocumentGrid from './DocumentGrid.vue';

defineProps({
  folderName: {
    type: String,
    default: 'Carpeta'
  },
  documents: {
    type: Array,
    default: () => []
  },
  hasSearch: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  'upload',
  'open-document'
]);
</script>

<template>
  <section>

    <!-- CABECERA DE CARPETA -->
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
      <div>
        <h2 class="h5 fw-bold mb-1">
          <i class="bi bi-folder-fill text-warning me-2"></i>
          {{ folderName }}
        </h2>

        <span class="text-secondary small">
          {{ documents.length }}
          {{ documents.length === 1 ? 'documento' : 'documentos' }}
        </span>
      </div>

      <button
          class="btn btn-sm btn-outline-primary"
          type="button"
          @click="emit('upload')"
      >
        <i class="bi bi-plus-lg me-1"></i>
        Añadir documento
      </button>
    </div>

    <!-- DOCUMENTOS -->
    <DocumentGrid
        v-if="documents.length > 0"
        :documents="documents"
        @open="emit('open-document', $event)"
    />

    <!-- SIN RESULTADOS DE BÚSQUEDA -->
    <div
        v-else-if="hasSearch"
        class="text-center text-secondary py-5"
    >
      <i class="bi bi-search display-4"></i>

      <h3 class="h6 fw-bold mt-3">
        No se encontraron documentos
      </h3>

      <p class="small">
        Prueba con otro nombre o término de búsqueda.
      </p>
    </div>

    <!-- CARPETA VACÍA -->
    <div
        v-else
        class="card border-0 bg-transparent text-center py-5"
    >
      <div class="card-body">
        <i class="bi bi-folder2-open display-3 text-secondary"></i>

        <h3 class="h6 fw-bold mt-3">
          Esta carpeta está vacía
        </h3>

        <p class="text-secondary small">
          Todavía no contiene documentos.
        </p>

        <button
            class="btn btn-primary btn-sm"
            type="button"
            @click="emit('upload')"
        >
          <i class="bi bi-cloud-arrow-up me-1"></i>
          Subir primer documento
        </button>
      </div>
    </div>

  </section>
</template>
