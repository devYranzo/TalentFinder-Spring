
<script setup>
defineProps({
  isLoading: {
    type: Boolean,
    default: false
  },
  showCreateButton: {
    type: Boolean,
    default: true
  },
  searchTerm: {
    type: String,
    default: ''
  },
  sortOrder: {
    type: String,
    default: 'asc'
  }
});

const emit = defineEmits([
  'refresh',
  'create-folder',
  'upload',
  'update:search-term',
  'update:sort-order'
]);

const updateSearch = (event) => {
  emit('update:search-term', event.target.value);
};

const clearSearch = () => {
  emit('update:search-term', '');
};

const updateSort = (event) => {
  emit('update:sort-order', event.target.value);
};
</script>

<template>
  <div class="row align-items-center g-2 mb-4 flex-nowrap">

    <!-- Buscador -->
    <div class="col-3">
      <div class="input-group">
        <span class="input-group-text glass">
          <i class="bi bi-search"></i>
        </span>

        <input
            type="search"
            class="form-control glass p-2"
            :value="searchTerm"
            placeholder="Buscar..."
            aria-label="Buscar carpetas y documentos"
            @input="updateSearch"
        />

        <button
            v-if="searchTerm"
            class="btn btn-outline-secondary"
            type="button"
            aria-label="Limpiar búsqueda"
            @click="clearSearch"
        >
          <i class="bi bi-x-lg"></i>
        </button>
      </div>
    </div>

    <!-- Ordenación -->
    <div v-if="showCreateButton" class="col-auto">
      <select
          class="form-select"
          :value="sortOrder"
          aria-label="Ordenar carpetas"
          @change="updateSort"
      >
        <option value="asc">Nombre A-Z</option>
        <option value="desc">Nombre Z-A</option>
      </select>
    </div>

    <!-- Nueva carpeta -->
    <div v-if="showCreateButton" class="col-auto">
      <button
          class="btn btn-outline-primary text-nowrap"
          type="button"
          @click="emit('create-folder')"
      >
        <i class="bi bi-folder-plus me-1"></i>
        Nueva carpeta
      </button>
    </div>

    <!-- Subir archivo -->
    <div class="col-auto">
      <button
          class="btn btn-primary text-nowrap"
          type="button"
          @click="emit('upload')"
      >
        <i class="bi bi-upload me-1"></i>
        Subir archivo
      </button>
    </div>

  </div>
</template>
