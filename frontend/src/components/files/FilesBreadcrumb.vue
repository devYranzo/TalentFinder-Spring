
<script setup>
defineProps({
  folderId: {
    type: [Number, String],
    default: null
  },
  folderName: {
    type: String,
    default: 'Carpeta'
  }
});

const emit = defineEmits(['home']);
</script>

<template>
  <nav aria-label="breadcrumb" class="mb-1 fs-5">
    <ol class="breadcrumb mb-0">

      <li class="breadcrumb-item">
        <a
            href="#"
            class="text-decoration-none text-white"
            @click.prevent="emit('home')"
        >
          <i class="bi bi-house-door me-1"></i>
          Home
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
</template>
