<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { getErrorMessage } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

// Solo rutas internas: evita open redirect (?redirect=https://sitio-malo.com o //sitio-malo.com)
function safeRedirect(): string {
  const target = route.query.redirect
  if (typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')) {
    return target
  }
  return '/search'
}

async function onSubmit() {
  if (loading.value) return

  loading.value = true
  error.value = ''

  try {
    await auth.login(email.value.trim(), password.value)
    await router.replace(safeRedirect())
  } catch (e) {
    error.value =
        axios.isAxiosError(e) && e.response?.status === 401
            ? 'Email o contraseña incorrectos.'
            : getErrorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center p-3">
    <!-- m-0 anula el margen de .island para que w-100 no se salga de la pantalla -->
    <div class="glass island m-0 w-100 p-4 p-md-5" style="max-width: 420px">
      <h1 class="h3 text-center fw-bold text-white mb-1">
        <span class="text-primary">Talent</span> Finder
      </h1>
      <p class="text-center text-white-50 mb-4">Inicia sesión para continuar</p>

      <div v-if="error" class="alert alert-danger py-2" role="alert">
        {{ error }}
      </div>

      <form novalidate @submit.prevent="onSubmit">
        <div class="mb-3">
          <label for="email" class="form-label">Email</label>
          <input
              id="email"
              v-model="email"
              type="email"
              class="form-control"
              autocomplete="username"
              required
              autofocus
          />
        </div>

        <div class="mb-4">
          <label for="password" class="form-label">Contraseña</label>
          <input
              id="password"
              v-model="password"
              type="password"
              class="form-control"
              autocomplete="current-password"
              required
          />
        </div>

        <button type="submit" class="btn btn-primary w-100" :disabled="loading || !email || !password">
          <span v-if="loading" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
          Entrar
        </button>
      </form>

      <p class="text-center text-white-50 small mt-4 mb-0">
        ¿Tu empresa aún no está registrada?
        <RouterLink :to="{ name: 'register' }">Crear cuenta</RouterLink>
      </p>
    </div>
  </div>
</template>