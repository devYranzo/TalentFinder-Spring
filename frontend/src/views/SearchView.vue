<script setup lang="ts">
import SearchBar from "@/components/SearchBar.vue";
import { onMounted } from "vue";
import { useAuthStore } from "@/stores/auth.ts";
import { useSearch } from "@/composables/useSearch.ts";
import { useMotorStatus } from "@/composables/useMotorStatus.ts";
import ResultCard from "@/components/ResultCard.vue";

const { isReady, loadingIngest, init } = useMotorStatus();

const authStore = useAuthStore();

const { query, respuesta, loading, copiado, canSearch, buscar, copiarAlPortapapeles } =
    useSearch(isReady);

onMounted(async () => {
  await init();
});
</script>

<template>
  <div class="search-view-container container-fluid d-flex flex-column justify-content-end align-items-center flex-grow-1">
    <div class="w-100">
      <ResultCard :respuesta="respuesta" :copiado="copiado" @copiar="copiarAlPortapapeles" />
      <SearchBar
          v-model="query"
          :is-ready="isReady"
          :loading="loading"
          :can-search="canSearch"
          @buscar="buscar"
      />
    </div>
  </div>
</template>

<style scoped>
.search-view-container {
  width: 100%;
}
</style>