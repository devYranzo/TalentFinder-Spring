
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
  'create',
  'rename',
  'delete'
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
          class="col folder-grid-col"
      >
        <div class="card h-100 w-100 glass overflow-visible">

          <div class="card-body d-flex align-items-center gap-2 p-2">

            <!-- ABRIR CARPETA -->
            <button
                type="button"
                class="btn bg-transparent border-0 shadow-none text-start d-flex align-items-center flex-grow-1 overflow-hidden p-1"
                :aria-label="'Abrir carpeta ' + folder.name"
                title="Haz doble clic para abrir"
                @dblclick="emit('open', folder.id)"
                @keydown.enter="emit('open', folder.id)"
            >
              <i class="bi bi-folder-fill text-warning fs-5"></i>

              <span class="fw-semibold ms-2 text-truncate">
                {{ folder.name }}
              </span>
            </button>

            <!-- MENÚ DE OPCIONES -->
            <div class="dropdown dropend flex-shrink-0">
              <button
                  type="button"
                  class="btn btn-sm shadow-none"
                  data-bs-toggle="dropdown"
                  aria-expanded="false"
                  :aria-label="'Opciones de ' + folder.name"
                  @click.stop
              >
                <i class="bi bi-three-dots-vertical"></i>
              </button>

              <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                <li>
                  <button
                      class="dropdown-item"
                      type="button"
                      @click="emit('rename', folder)"
                  >
                    <i class="bi bi-pencil me-2"></i>
                    Renombrar
                  </button>
                </li>

                <li>
                  <button
                      class="dropdown-item text-danger"
                      type="button"
                      @click="emit('delete', folder)"
                  >
                    <i class="bi bi-trash me-2"></i>
                    Eliminar
                  </button>
                </li>
              </ul>
            </div>

          </div>
        </div>
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

<style scoped>
.folder-grid-col:has(.dropdown-menu.show) {
  position: relative;
  z-index: 2;
}

.dropdown-menu {
  z-index: 1080;
}
</style>
