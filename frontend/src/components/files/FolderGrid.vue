
<script setup>
defineProps({
  folders: {
    type: Array,
    default: () => []
  },
  hasSearch: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  'open',
  'create'
]);
</script>

<template>
  <section class="mb-4">

    <div
        v-if="folders.length > 0"
        class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 row-cols-xl-6 g-3"
    >
      <div
          v-for="folder in folders"
          :key="folder.id"
          class="col"
      >
        <button
            type="button"
            class="card h-100 w-100 glass text-start"
            :aria-label="'Abrir carpeta ' + folder.name"
            title="Haz doble clic para abrir"
            @click.stop
            @dblclick="emit('open', folder.id)"
            @keydown.enter="emit('open', folder.id)"
        >
          <div class="card-body d-flex align-items-center">
            <i class="bi bi-folder-fill text-warning"></i>

            <span class="fw-semibold ms-2 text-truncate">
              {{ folder.name }}
            </span>
          </div>
        </button>
      </div>
    </div>

    <!-- SIN RESULTADOS -->
    <div
        v-else-if="hasSearch"
        class="text-center text-secondary py-4"
    >
      <i class="bi bi-folder-x display-5"></i>

      <p class="small mb-0 mt-2">
        No se encontraron carpetas.
      </p>
    </div>

    <!-- SIN CARPETAS -->
    <div
        v-else
        class="text-center text-secondary py-5"
    >
      <i class="bi bi-folder2-open display-3"></i>

      <h3 class="h6 fw-bold mt-3">
        No hay carpetas
      </h3>

      <p class="small">
        Crea una carpeta para organizar tus documentos.
      </p>

      <button
          class="btn btn-outline-primary btn-sm"
          type="button"
          @click="emit('create')"
      >
        <i class="bi bi-folder-plus me-1"></i>
        Crear primera carpeta
      </button>
    </div>

  </section>
</template>
