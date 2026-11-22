
<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'

import usersApi, {
  type User,
  type UserForm,
} from '@/api/users'

import UserModal from '@/components/users/UserModal.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const users = ref<User[]>([])

const isLoading = ref(true)
const isSaving = ref(false)
const isDeleting = ref<number | null>(null)

const errorMessage = ref('')
const searchQuery = ref('')

const isEdit = ref(false)

const userModalRef = ref<InstanceType<typeof UserModal> | null>(null)

const form = ref<UserForm>({
  id: null,
  email: '',
  firstName: '',
  lastName: '',
  password: '',
  role: 'RECRUITER',
  active: true,
})

const currentUserId = computed(() => authStore.profile?.id ?? null)

const filteredUsers = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  if (!query) return users.value

  return users.value.filter(user => {
    const fullName = `${user.firstName} ${user.lastName}`.toLowerCase()

    return (
        fullName.includes(query) ||
        user.email.toLowerCase().includes(query) ||
        user.role.toLowerCase().includes(query)
    )
  })
})

const totalUsers = computed(() => users.value.length)

const activeUsers = computed(() =>
    users.value.filter(user => user.active).length
)

const adminUsers = computed(() =>
    users.value.filter(user => user.role === 'ADMIN').length
)

function isCurrentUser(userId: number): boolean {
  return currentUserId.value !== null && currentUserId.value === userId
}

function getInitials(user: User): string {
  const first = user.firstName?.charAt(0) ?? ''
  const last = user.lastName?.charAt(0) ?? ''

  return `${first}${last}`.toUpperCase()
}

function getRoleName(role: string): string {
  switch (role) {
    case 'ADMIN':
      return 'Administrador'
    case 'RECRUITER':
      return 'Recruiter'
    default:
      return role
  }
}

function getRoleClass(role: string): string {
  return role === 'ADMIN'
      ? 'bg-primary-subtle text-primary'
      : 'bg-info-subtle text-info-emphasis'
}

async function loadUsers() {
  try {
    isLoading.value = true
    errorMessage.value = ''

    users.value = await usersApi.getAllUsers()
  } catch (error: unknown) {
    console.error('Error cargando usuarios:', error)
    errorMessage.value = 'No se pudieron cargar los usuarios.'
  } finally {
    isLoading.value = false
  }
}

async function openCreate() {
  isEdit.value = false

  form.value = {
    id: null,
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    role: 'RECRUITER',
    active: true,
  }

  await nextTick()
  userModalRef.value?.show()
}

async function openEdit(user: User) {
  if (isCurrentUser(user.id)) return

  isEdit.value = true

  form.value = {
    id: user.id,
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    password: '',
    role: user.role,
    active: user.active,
  }

  await nextTick()
  userModalRef.value?.show()
}

async function handleSave() {
  if (isSaving.value) return

  isSaving.value = true

  try {
    if (isEdit.value && form.value.id !== null) {
      await usersApi.editUser(form.value.id, {
        firstName: form.value.firstName.trim(),
        lastName: form.value.lastName.trim(),
        role: form.value.role,
      })
    } else {
      await usersApi.createUser({
        email: form.value.email.trim(),
        firstName: form.value.firstName.trim(),
        lastName: form.value.lastName.trim(),
        password: form.value.password,
        role: form.value.role,
      })
    }

    userModalRef.value?.hide()

    await loadUsers()
  } catch (error: unknown) {
    console.error('Error guardando usuario:', error)

    const responseError = error as {
      response?: {
        data?: {
          detail?: string
          message?: string
        }
      }
    }

    window.alert(
        responseError.response?.data?.detail ??
        responseError.response?.data?.message ??
        'No se pudo guardar el usuario.'
    )
  } finally {
    isSaving.value = false
  }
}

async function handleDelete(user: User) {
  if (isCurrentUser(user.id)) return

  const confirmed = window.confirm(
      `¿Estás seguro de que quieres eliminar a ${user.firstName} ${user.lastName}?`
  )

  if (!confirmed) return

  try {
    isDeleting.value = user.id

    await usersApi.deleteUser(user.id)

    await loadUsers()
  } catch (error: unknown) {
    console.error('Error eliminando usuario:', error)

    const responseError = error as {
      response?: {
        data?: {
          detail?: string
          message?: string
        }
      }
    }

    window.alert(
        responseError.response?.data?.detail ??
        responseError.response?.data?.message ??
        'No se pudo eliminar el usuario.'
    )
  } finally {
    isDeleting.value = null
  }
}

onMounted(loadUsers)
</script>

<template>
  <div class="container-fluid py-3">

    <!-- Cabecera -->
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
      <div>
        <h3 class="fw-bold mb-1">
          Gestión de usuarios
        </h3>

        <p class="text-body-secondary mb-0">
          Administra los accesos, permisos y usuarios del sistema.
        </p>
      </div>

      <button
          type="button"
          class="btn btn-primary rounded-3 px-4"
          @click="openCreate"
      >
        <i class="bi bi-person-plus-fill me-2"></i>
        Nuevo usuario
      </button>
    </div>

    <!-- Estadísticas -->
    <div class="row g-3 mb-4">

      <div class="col-12 col-md-4">
        <div class="glass rounded-4 p-3 h-100">
          <div class="d-flex align-items-center justify-content-between">
            <div>
              <p class="text-body-secondary small fw-semibold mb-2">
                Total usuarios
              </p>

              <h3 class="fw-bold mb-0">
                {{ totalUsers }}
              </h3>
            </div>

            <div class="bg-primary-subtle text-primary rounded-3 p-3">
              <i class="bi bi-people-fill fs-4"></i>
            </div>
          </div>
        </div>
      </div>

      <div class="col-12 col-md-4">
        <div class="glass rounded-4 p-3 h-100">
          <div class="d-flex align-items-center justify-content-between">
            <div>
              <p class="text-body-secondary small fw-semibold mb-2">
                Usuarios activos
              </p>

              <h3 class="fw-bold mb-0">
                {{ activeUsers }}
              </h3>
            </div>

            <div class="bg-success-subtle text-success rounded-3 p-3">
              <i class="bi bi-person-check-fill fs-4"></i>
            </div>
          </div>
        </div>
      </div>

      <div class="col-12 col-md-4">
        <div class="glass rounded-4 p-3 h-100">
          <div class="d-flex align-items-center justify-content-between">
            <div>
              <p class="text-body-secondary small fw-semibold mb-2">
                Administradores
              </p>

              <h3 class="fw-bold mb-0">
                {{ adminUsers }}
              </h3>
            </div>

            <div class="bg-info-subtle text-info-emphasis rounded-3 p-3">
              <i class="bi bi-shield-lock-fill fs-4"></i>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- Listado -->
    <div>

      <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h5 class="fw-bold mb-1">
            Usuarios registrados
          </h5>

          <p class="text-body-secondary small mb-0">
            {{ filteredUsers.length }} usuarios encontrados
          </p>
        </div>

        <div class="input-group w-auto">
          <span class="input-group-text bg-transparent">
            <i class="bi bi-search"></i>
          </span>

          <input
              v-model="searchQuery"
              type="search"
              class="form-control bg-transparent"
              placeholder="Buscar usuario..."
              aria-label="Buscar usuarios"
          >

          <button
              v-if="searchQuery"
              class="btn btn-outline-secondary"
              type="button"
              @click="searchQuery = ''"
              aria-label="Limpiar búsqueda"
          >
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
      </div>

      <!-- Error -->
      <div
          v-if="errorMessage"
          class="alert alert-danger d-flex align-items-center justify-content-between"
          role="alert"
      >
        <span>
          <i class="bi bi-exclamation-triangle-fill me-2"></i>
          {{ errorMessage }}
        </span>

        <button
            type="button"
            class="btn btn-sm btn-outline-danger"
            @click="loadUsers"
        >
          Reintentar
        </button>
      </div>

      <!-- Tabla -->
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">

          <thead>
          <tr class="text-body-secondary small">
            <th scope="col" class="border-0 py-3 bg-transparent">
              USUARIO
            </th>

            <th scope="col" class="border-0 py-3 bg-transparent">
              CORREO ELECTRÓNICO
            </th>

            <th scope="col" class="border-0 py-3 bg-transparent">
              ROL
            </th>

            <th scope="col" class="border-0 py-3 bg-transparent">
              ESTADO
            </th>

            <th scope="col" class="border-0 py-3 bg-transparent">
              ACCIONES
            </th>
          </tr>
          </thead>

          <tbody>

          <!-- Cargando -->
          <tr v-if="isLoading">
            <td colspan="5" class="text-center py-5 bg-transparent">
              <div
                  class="spinner-border text-primary"
                  role="status"
              ></div>

              <p class="text-body-secondary small mt-3 mb-0">
                Cargando usuarios...
              </p>
            </td>
          </tr>

          <!-- Usuarios -->
          <template v-else-if="!errorMessage">
            <tr
                v-for="user in filteredUsers"
                :key="user.id"
            >
              <!-- Nombre -->
              <td class="py-3 bg-transparent">
                <div class="d-flex align-items-center gap-3">
                  <div
                      class="bg-primary-subtle text-primary rounded-circle d-flex align-items-center justify-content-center fw-bold flex-shrink-0 p-2"
                  >
                    {{ getInitials(user) }}
                  </div>

                  <div>
                    <div class="fw-semibold">
                      {{ user.firstName }} {{ user.lastName }}
                    </div>

                    <span
                        v-if="isCurrentUser(user.id)"
                        class="badge bg-secondary-subtle text-secondary mt-1"
                    >
                        Tú
                      </span>
                  </div>
                </div>
              </td>

              <!-- Email -->
              <td class="bg-transparent">
                  <span class="text-body-secondary">
                    {{ user.email }}
                  </span>
              </td>

              <!-- Rol -->
              <td class="bg-transparent">
                  <span
                      class="badge rounded-pill px-3 py-2"
                      :class="getRoleClass(user.role)"
                  >
                    {{ getRoleName(user.role) }}
                  </span>
              </td>

              <!-- Estado -->
              <td class="bg-transparent">
                  <span
                      v-if="user.active"
                      class="badge rounded-pill bg-success-subtle text-success px-3 py-2"
                  >
                    <i class="bi bi-circle-fill me-1 small"></i>
                    Activo
                  </span>

                <span
                    v-else
                    class="badge rounded-pill bg-danger-subtle text-danger px-3 py-2"
                >
                    <i class="bi bi-circle-fill me-1 small"></i>
                    Inactivo
                  </span>
              </td>

              <!-- Acciones -->
              <td class="bg-transparent">
                <div class="d-inline-flex gap-1">
                  <button
                      type="button"
                      class="btn btn-sm btn-outline-secondary"
                      title="Editar usuario"
                      :aria-label="`Editar usuario ${user.firstName}`"
                      :disabled="isCurrentUser(user.id)"
                      @click="openEdit(user)"
                  >
                    <i class="bi bi-pencil-fill"></i>
                  </button>

                  <button
                      type="button"
                      class="btn btn-sm btn-outline-danger"
                      title="Eliminar usuario"
                      :aria-label="`Eliminar usuario ${user.firstName}`"
                      :disabled="isCurrentUser(user.id) || isDeleting === user.id"
                      @click="handleDelete(user)"
                  >
                      <span
                          v-if="isDeleting === user.id"
                          class="spinner-border spinner-border-sm"
                          role="status"
                      ></span>

                    <i
                        v-else
                        class="bi bi-trash3-fill"
                    ></i>
                  </button>
                </div>
              </td>
            </tr>

            <!-- Sin resultados -->
            <tr v-if="filteredUsers.length === 0">
              <td colspan="5" class="text-center py-5 bg-transparent">
                <i class="bi bi-people fs-1 text-body-secondary"></i>

                <h6 class="fw-semibold mt-3">
                  No se encontraron usuarios
                </h6>

                <p class="text-body-secondary small mb-0">
                  {{
                    searchQuery
                        ? 'Prueba con otro término de búsqueda.'
                        : 'Todavía no hay usuarios registrados.'
                  }}
                </p>
              </td>
            </tr>
          </template>

          </tbody>
        </table>
      </div>

    </div>

    <!-- Modal crear / editar -->
    <UserModal
        ref="userModalRef"
        :form-data="form"
        :is-edit="isEdit"
        :is-saving="isSaving"
        @save="handleSave"
    />

  </div>
</template>
