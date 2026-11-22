
<script setup>
defineProps({
  folderId: {
    type: [Number, String],
    default: null
  },
  folderName: {
    type: String,
    default: 'Carpeta'
  },
  canGoForward: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['home', 'forward']);
</script>

<template>
  <div class="d-flex align-items-center gap-3 mb-1">

    <!-- NAVEGACIÓN -->
    <div class="d-flex align-items-center gap-1 flex-shrink-0">

      <!-- VOLVER A HOME -->
      <button
          type="button"
          class="btn border-0 shadow-none"
          title="Volver a Home"
          aria-label="Volver a Home"
          :disabled="folderId === null"
          @click="emit('home')"
      >
        <i class="bi bi-arrow-left"></i>
      </button>

      <!-- VOLVER A LA ÚLTIMA CARPETA -->
      <button
          type="button"
          class="btn border-0 shadow-none"
          title="Volver a la última carpeta"
          aria-label="Volver a la última carpeta"
          :disabled="!canGoForward"
          @click="emit('forward')"
      >
        <i class="bi bi-arrow-right"></i>
      </button>

    </div>

    <!-- BREADCRUMB -->
    <nav aria-label="breadcrumb" class="fs-5">
      <ol class="breadcrumb mb-0">

        <li class="breadcrumb-item">
          <a
              href="#"
              class="text-decoration-none text-white"
              @click.prevent="emit('home')"
          >
            <i class="bi bi-house-fill me-1"></i>
            Inicio
          </a>
        </li>

        <li
            v-if="folderId !== null"
            class="breadcrumb-item active"
            aria-current="page"
        >
          <i class="bi bi-folder-fill me-1"></i>
          {{ folderName }}
        </li>

      </ol>
    </nav>

  </div>
</template>
