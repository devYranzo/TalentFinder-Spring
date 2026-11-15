<script setup lang="ts">
import SearchBar from "@/components/SearchBar.vue";
import { onMounted } from "vue";
import { useSearch } from "@/composables/useSearch.ts";
import { useMotorStatus } from "@/composables/useMotorStatus.ts";
import ResultCard from "@/components/ResultCard.vue";

const { isReady, init } = useMotorStatus();

const { query, respuesta, loading, copiado, canSearch, buscar, copiarAlPortapapeles } =
    useSearch(isReady);

onMounted(async () => {
  await init();
});
</script>

<template>
  <div
      class="container-fluid d-flex flex-column align-items-center flex-grow-1"
      :class="respuesta ? 'justify-content-end' : 'justify-content-center'"
  >
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