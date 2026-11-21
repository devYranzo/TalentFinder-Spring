
<script setup>
defineProps({
  documents: {
    type: Array,
    default: () => []
  },
  emptyMessage: {
    type: String,
    default: 'No hay documentos.'
  }
});

const emit = defineEmits(['open']);
</script>

<template>
  <section>

    <div
        v-if="documents.length > 0"
        class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 row-cols-xl-6 g-3"
    >
      <div
          v-for="document in documents"
          :key="document.id"
          class="col"
      >
        <div class="card h-100 shadow-sm glass overflow-hidden">

          <!-- VISTA PREVIA -->
          <button
              type="button"
              class="card-body border-0 bg-transparent d-flex flex-column align-items-center justify-content-center p-4"
              :aria-label="'Ver documento ' + document.filename"
              @click="emit('open', document)"
          >
            <i class="bi bi-file-earmark-pdf-fill text-danger display-3"></i>
          </button>

          <!-- FOOTER -->
          <div class="card-footer bg-transparent border-top d-flex align-items-center gap-2 py-2 px-2">
            <span
                class="small fw-semibold text-truncate flex-grow-1"
                :title="document.filename"
            >
              <i class="bi bi-file-earmark-pdf-fill text-danger me-1"></i>
              {{ document.filename }}
            </span>

            <div class="dropdown flex-shrink-0">
              <button
                  type="button"
                  class="btn btn-sm bg-transparent border-0"
                  data-bs-toggle="dropdown"
                  aria-expanded="false"
                  :aria-label="'Opciones de ' + document.filename"
                  @click.stop
              >
                <i class="bi bi-three-dots-vertical"></i>
              </button>

              <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                <li>
                  <button
                      class="dropdown-item"
                      type="button"
                      @click="emit('open', document)"
                  >
                    <i class="bi bi-eye me-2"></i>
                    Ver documento
                  </button>
                </li>
              </ul>
            </div>
          </div>

        </div>
      </div>
    </div>

    <!-- SIN DOCUMENTOS -->
    <div
        v-else
        class="text-center text-secondary py-4"
    >
      <i class="bi bi-file-earmark display-5"></i>

      <p class="small mb-0 mt-2">
        {{ emptyMessage }}
      </p>
    </div>

  </section>
</template>
