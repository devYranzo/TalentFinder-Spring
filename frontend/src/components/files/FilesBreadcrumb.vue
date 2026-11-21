
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
  <nav aria-label="breadcrumb" class="mb-4">
    <ol class="breadcrumb mb-0">
      <li
          class="breadcrumb-item"
          :class="{ active: folderId === null }"
      >
        <button
            v-if="folderId !== null"
            class="btn btn-link p-0 text-decoration-none"
            type="button"
            @click="emit('home')"
        >
          <i class="bi bi-house-door me-1"></i>
          Home
        </button>

        <span v-else>
          <i class="bi bi-house-door me-1"></i>
          Home
        </span>
      </li>

      <li
          v-if="folderId !== null"
          aria-current="page"
          class="breadcrumb-item active"
      >
        {{ folderName }}
      </li>
    </ol>
  </nav>
</template>
