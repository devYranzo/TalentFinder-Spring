<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types'

interface NavItem {
  to: string
  label: string
  icon: string
  roles: Role[]
}

const ALL: Role[] = ['admin', 'recruiter', 'viewer']

const navItems: NavItem[] = [
  { to: '/search', label: 'Buscador', icon: 'bi-search', roles: ALL },
  { to: '/projects', label: 'Proyectos', icon: 'bi-briefcase', roles: ['admin', 'recruiter'] },
  { to: '/files', label: 'Gestor de CVs', icon: 'bi-folder2-open', roles: ['admin', 'recruiter'] },
  { to: '/analytics', label: 'Analítica', icon: 'bi-bar-chart', roles: ALL },
  { to: '/indexing', label: 'Indexación', icon: 'bi-database', roles: ['admin'] },
  { to: '/users', label: 'Usuarios', icon: 'bi-people', roles: ['admin'] },
  { to: '/settings', label: 'Organización', icon: 'bi-building-gear', roles: ['admin'] },
]

const roleLabel: Record<Role, string> = {
  admin: 'Administrador',
  recruiter: 'Reclutador',
  viewer: 'Lector',
}

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const sidebarOpen = ref(false)
const visibleItems = computed(() => navItems.filter((item) => auth.hasRole(item.roles)))

// Cierra el menú móvil al navegar
watch(
    () => route.fullPath,
    () => {
      sidebarOpen.value = false
    },
)

async function onLogout() {
  await auth.logout()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell" data-bs-theme="dark">
    <aside class="app-sidebar glass island" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <span class="text-primary">Talent</span> Finder
      </div>

      <nav class="sidebar-nav">
        <ul class="nav flex-column gap-1">
          <li v-for="item in visibleItems" :key="item.to" class="nav-item">
            <RouterLink :to="item.to" class="sidebar-link">
              <i class="bi" :class="item.icon"></i>
              <span>{{ item.label }}</span>
            </RouterLink>
          </li>
        </ul>
      </nav>

      <div class="sidebar-footer">
        <RouterLink to="/profile" class="sidebar-link mb-1">
          <i class="bi bi-person-circle"></i>
          <span class="d-flex flex-column lh-sm overflow-hidden">
            <span class="text-truncate">{{ auth.fullName }}</span>
            <small v-if="auth.role" class="opacity-75">{{ roleLabel[auth.role] }}</small>
          </span>
        </RouterLink>

        <button type="button" class="sidebar-link" @click="onLogout">
          <i class="bi bi-box-arrow-right"></i>
          <span>Cerrar sesión</span>
        </button>
      </div>
    </aside>

    <div v-if="sidebarOpen" class="app-backdrop" @click="sidebarOpen = false"></div>

    <div class="app-main">
      <header class="app-topbar glass island island-sm d-lg-none d-flex align-items-center gap-3 px-3 py-2">
        <button type="button" class="btn btn-outline-light" aria-label="Abrir menú" @click="sidebarOpen = true">
          <i class="bi bi-list"></i>
        </button>
        <span class="fw-bold text-white"><span class="text-primary">Talent</span> Finder</span>
      </header>

      <main class="p-3 p-lg-4">
        <RouterView />
      </main>
    </div>
  </div>
</template>