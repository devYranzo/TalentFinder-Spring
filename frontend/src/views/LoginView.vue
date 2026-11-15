<script setup lang="ts">
import {reactive, ref} from 'vue'
import axios from 'axios'
import {useRoute, useRouter} from 'vue-router'

import {getErrorMessage} from '@/api/errors'
import {useAuthStore} from '@/stores/auth'
import AuthLayout from "@/components/AuthLayout.vue";

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const form = reactive({
  email: '',
  password: '',
});

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

async function handleLogin() {
  if (loading.value) return

  loading.value = true
  error.value = ''

  try {
    await auth.login(form.email.trim(), form.password.trim())
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
  <AuthLayout title-primary="Talent" title-secondary="Finder" subtitle="Smarter hiring starts here">
    <h1 class="text-white mb-4 fs-4">Bienvenido de nuevo</h1>

    <form @submit.prevent="handleLogin">
      <div class="mb-3">
        <label for="email" class="form-label small fw-bold">Correo electrónico</label>
        <div class="input-group">
          <span class="input-group-text border-end-0 bg-transparent">
            <i class="bi bi-envelope text-muted" aria-hidden="true"></i>
          </span>
          <input
              type="email"
              class="form-control border-start-0 bg-transparent"
              v-model="form.email"
              id="email"
              autocomplete="username email"
              required
              autofocus
          />
        </div>
      </div>

      <div class="mb-4">
        <label for="password" class="form-label small fw-bold">Contraseña</label>
        <div class="input-group">
          <span class="input-group-text border-end-0 bg-transparent">
            <i class="bi bi-lock text-muted" aria-hidden="true"></i>
          </span>
          <input
              type="password"
              v-model="form.password"
              class="form-control border-start-0 bg-transparent"
              id="password"
              autocomplete="current-password"
              required
          />
        </div>
      </div>

      <div class="d-grid">
        <button
            type="submit"
            :disabled="loading"
            class="btn btn-primary btn-lg"
            :aria-busy="loading"
        >
          {{ loading ? 'Iniciando sesión...' : 'Iniciar sesión' }}
        </button>
        <!--        <div class="d-block mt-3 text-primary small">-->
        <!--          <span class="text-muted me-1">¿No tienes cuenta?</span>-->
        <!--          <router-link to="/register" class="text-decoration-none">-->
        <!--            <b>Regístrate aquí</b>-->
        <!--          </router-link>-->
        <!--        </div>-->
      </div>

      <div v-if="error" class="alert alert-danger mt-3 mb-0" role="alert">
        <i class="bi bi-exclamation-circle me-2" aria-hidden="true"></i>
        <strong>Error:</strong> {{ error }}
      </div>
    </form>
  </AuthLayout>
</template>