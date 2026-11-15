import { createRouter, createWebHistory } from 'vue-router'

import AppLayout from '@/layouts/AppLayout.vue'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** Accesible sin sesión */
    public?: boolean
    /** Si hay sesión, redirige a la app (login) */
    guestOnly?: boolean
    /** Roles permitidos. Sin definir = cualquier usuario autenticado */
    roles?: Role[]
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: 'Iniciar sesión', public: true, guestOnly: true },
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: { name: 'search' } },
        {
          path: 'search',
          name: 'search',
          component: () => import('@/views/SearchView.vue'),
          meta: { title: 'Buscador' },
        },
        {
          path: 'projects',
          name: 'projects',
          component: () => import('@/views/ProjectsView.vue'),
          meta: { title: 'Proyectos', roles: ['ADMIN', 'RECRUITER'] },
        },
        {
          path: 'files',
          name: 'files',
          component: () => import('@/views/FilesView.vue'),
          meta: { title: 'Gestor de CVs', roles: ['ADMIN', 'RECRUITER'] },
        },
        {
          path: 'analytics',
          name: 'analytics',
          component: () => import('@/views/AnalyticsView.vue'),
          meta: { title: 'Analítica', roles: ['ADMIN', 'RECRUITER'] },
        },
        {
          path: 'indexing',
          name: 'indexing',
          component: () => import('@/views/IndexingView.vue'),
          meta: { title: 'Indexación', roles: ['ADMIN'] },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UsersView.vue'),
          meta: { title: 'Usuarios', roles: ['ADMIN'] },
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('@/views/SettingsView.vue'),
          meta: { title: 'Ajustes de organización', roles: ['ADMIN'] },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProjectsView.vue'),
          meta: { title: 'Mi perfil' },
        },
        {
          path: '403',
          name: 'forbidden',
          component: () => import('@/views/ErrorView.vue'),
          props: { code: 403 },
          meta: { title: 'Sin permisos' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/ErrorView.vue'),
      props: { code: 404 },
      meta: { title: 'No encontrado' },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // Primera navegación: preguntamos al backend si hay sesión activa
  if (!auth.loaded) {
    await auth.fetchMe()
  }

  if (to.meta.public) {
    if (to.meta.guestOnly && auth.isAuthenticated) {
      return { name: 'search' }
    }
    return true
  }

  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && !auth.hasRole(to.meta.roles)) {
    return { name: 'forbidden' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · Talent Finder` : 'Talent Finder'
})

export default router