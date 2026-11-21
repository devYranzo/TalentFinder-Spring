<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {RouterLink, RouterView, useRoute, useRouter} from 'vue-router'

import {useAuthStore} from '@/stores/auth'
import type {Role} from '@/types'

interface NavItem {
  to: string
  label: string
  icon: string
  roles: Role[]
}

const ALL: Role[] = ['ADMIN', 'RECRUITER']

const navItems: NavItem[] = [
  {to: '/search', label: 'Buscador', icon: 'bi-search', roles: ALL},
  {to: '/projects', label: 'Proyectos', icon: 'bi-briefcase', roles: ['ADMIN', 'RECRUITER']},
  {to: '/files', label: 'Gestor de CVs', icon: 'bi-folder2-open', roles: ['ADMIN', 'RECRUITER']},
  {to: '/analytics', label: 'Analítica', icon: 'bi-bar-chart', roles: ALL},
  {to: '/indexing', label: 'Indexación', icon: 'bi-database', roles: ['ADMIN']},
  {to: '/users', label: 'Usuarios', icon: 'bi-people', roles: ['ADMIN']},
  {to: '/settings', label: 'Organización', icon: 'bi-building-gear', roles: ['ADMIN']},
]

const roleLabel: Record<Role, string> = {
  ADMIN: 'Administrador',
  RECRUITER: 'Reclutador',
}

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const sidebarOpen = ref(false)
const loggingOut = ref(false)
const visibleItems = computed(() => navItems.filter((item) => auth.hasRole(item.roles)))

// Cierra el menú móvil al navegar
watch(
    () => route.fullPath,
    () => {
      sidebarOpen.value = false
    },
)

async function onLogout() {
  if (loggingOut.value) return
  loggingOut.value = true

  try {
    await auth.logout()
  } catch {
    // Si la petición falla, el store ya limpió el estado local (finally en logout()).
    // Aun así llevamos al usuario al login.
  } finally {
    loggingOut.value = false
    await router.replace({name: 'login'})
  }
}
</script>

<template>
  <div class="d-flex min-vh-100">
    <div class="d-none d-lg-block flex-shrink-0 w-sidebar"></div>

    <!-- Fondo oscuro del menú en móvil -->
    <div
        v-if="sidebarOpen"
        class="d-lg-none position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-75 z-2"
        @click="sidebarOpen = false"
    ></div>

    <aside
        class="position-fixed top-0 bottom-0 start-0 z-3 w-sidebar"
        :class="sidebarOpen ? 'd-flex' : 'd-none d-lg-flex'"
    >
      <div class="glass island flex-grow-1 d-flex flex-column">
        <div class="fs-3 fw-bold text-white px-4 pt-4 pb-3">
          <span class="text-primary">Talent</span> Finder
        </div>

        <ul class="nav nav-pills flex-column flex-nowrap gap-1 px-3 flex-grow-1 overflow-y-auto">
          <li v-for="item in visibleItems" :key="item.to" class="nav-item">
            <RouterLink :to="item.to" class="nav-link" active-class="active">
              <i class="bi me-2" :class="item.icon"></i>{{ item.label }}
            </RouterLink>
          </li>
        </ul>

        <div class="nav nav-pills flex-nowrap gap-1 p-3 border-top border-light border-opacity-10">
          <RouterLink
              to="/profile"
              class="nav-link flex-grow-1 d-flex align-items-center text-truncate"
              active-class="active"
              :title="auth.role ? roleLabel[auth.role] : ''"
          >
            <i class="bi bi-person-circle me-2"></i>
            <span class="text-truncate">{{ auth.fullName }}</span>
          </RouterLink>

          <button type="button" class="nav-link" :disabled="loggingOut" @click="onLogout">
            <i class="bi bi-box-arrow-right"></i>
          </button>
        </div>
      </div>
    </aside>

    <div class="flex-grow-1 min-w-0">
      <header
          class="glass island island-sm d-lg-none d-flex align-items-center gap-3 px-3 py-2 position-sticky top-0 z-1">
        <button type="button" class="btn btn-outline-light" aria-label="Abrir menú" @click="sidebarOpen = true">
          <i class="bi bi-list"></i>
        </button>
        <span class="fw-bold text-white"><span class="text-primary">Talent</span> Finder</span>
      </header>

      <main class="flex-grow-1 d-flex flex-column min-vh-100">
        <RouterView/>
      </main>
    </div>
  </div>
</template>