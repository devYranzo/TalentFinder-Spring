<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from 'vue'
import {Modal} from 'bootstrap'
import usersApi from '@/api/users'

const props = defineProps<{
  userId: number
  userName: string
}>()

const emit = defineEmits<{
  close: []
  success: []
}>()

const newPassword = ref('')
const confirmPassword = ref('')

const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

const modalElement = ref<HTMLElement | null>(null)

let modalInstance: Modal | null = null

onMounted(() => {
  if (modalElement.value) {
    modalInstance = new Modal(modalElement.value)

    modalElement.value.addEventListener('hidden.bs.modal', handleHidden)
    modalInstance.show()
  }
})

onBeforeUnmount(() => {
  modalElement.value?.removeEventListener('hidden.bs.modal', handleHidden)
  modalInstance?.dispose()
})

function handleHidden() {
  emit('close')
}

function show() {
  modalInstance?.show()
}

function hide() {
  modalInstance?.hide()
}

function validateForm(): boolean {
  errorMessage.value = ''

  if (!newPassword.value) {
    errorMessage.value = 'La nueva contraseña es obligatoria.'
    return false
  }

  if (newPassword.value.length < 6) {
    errorMessage.value = 'La contraseña debe tener al menos 6 caracteres.'
    return false
  }

  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'Las contraseñas no coinciden.'
    return false
  }

  return true
}

async function handleSubmit() {
  if (!validateForm()) return

  isLoading.value = true
  errorMessage.value = ''

  try {
    await usersApi.editUser({
      id: props.userId,
      password: newPassword.value
    })

    successMessage.value = `La contraseña de ${props.userName} se ha cambiado correctamente.`

    newPassword.value = ''
    confirmPassword.value = ''

    emit('success')
  } catch (error: unknown) {
    const responseError = error as {
      response?: {
        data?: {
          detail?: string
        }
      }
    }

    errorMessage.value =
        responseError.response?.data?.detail ??
        'No se pudo cambiar la contraseña.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div
      ref="modalElement"
      class="modal fade"
      tabindex="-1"
      aria-labelledby="passwordModalLabel"
      aria-hidden="true"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content glass border-0 rounded-4 shadow">

        <div class="modal-header border-0 px-4 pt-4">
          <div>
            <h5 id="passwordModalLabel" class="modal-title fw-bold">
              Cambiar contraseña
            </h5>

            <p class="text-body-secondary small mb-0">
              {{ userName }}
            </p>
          </div>

          <button
              type="button"
              class="btn-close"
              data-bs-dismiss="modal"
              aria-label="Cerrar"
              :disabled="isLoading"
          ></button>
        </div>

        <div class="modal-body px-4 py-3">

          <div
              v-if="successMessage"
              class="alert alert-success d-flex align-items-center gap-2"
              role="alert"
          >
            <i class="bi bi-check-circle-fill"></i>
            <span>{{ successMessage }}</span>
          </div>

          <div
              v-if="errorMessage"
              class="alert alert-danger d-flex align-items-center gap-2"
              role="alert"
          >
            <i class="bi bi-exclamation-triangle-fill"></i>
            <span>{{ errorMessage }}</span>
          </div>

          <template v-if="!successMessage">

            <div class="alert alert-info border-0 rounded-3">
              <div class="d-flex gap-2">
                <i class="bi bi-info-circle-fill"></i>
                <small>
                  Establece una nueva contraseña para este usuario.
                </small>
              </div>
            </div>

            <form @submit.prevent="handleSubmit">

              <div class="mb-3">
                <label for="newPassword" class="form-label fw-semibold">
                  Nueva contraseña
                </label>

                <div class="input-group">
                  <input
                      id="newPassword"
                      v-model="newPassword"
                      :type="showNewPassword ? 'text' : 'password'"
                      class="form-control"
                      placeholder="Nueva contraseña"
                      autocomplete="new-password"
                      minlength="6"
                      required
                      :disabled="isLoading"
                  >

                  <button
                      type="button"
                      class="btn btn-outline-secondary"
                      @click="showNewPassword = !showNewPassword"
                      :disabled="isLoading"
                      :aria-label="showNewPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                  >
                    <i :class="showNewPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                  </button>
                </div>

                <div class="form-text">
                  Mínimo 6 caracteres.
                </div>
              </div>

              <div class="mb-2">
                <label for="confirmPassword" class="form-label fw-semibold">
                  Confirmar contraseña
                </label>

                <div class="input-group">
                  <input
                      id="confirmPassword"
                      v-model="confirmPassword"
                      :type="showConfirmPassword ? 'text' : 'password'"
                      class="form-control"
                      placeholder="Repite la contraseña"
                      autocomplete="new-password"
                      required
                      :disabled="isLoading"
                  >

                  <button
                      type="button"
                      class="btn btn-outline-secondary"
                      @click="showConfirmPassword = !showConfirmPassword"
                      :disabled="isLoading"
                      :aria-label="showConfirmPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                  >
                    <i :class="showConfirmPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                  </button>
                </div>
              </div>

            </form>
          </template>

        </div>

        <div class="modal-footer border-0 px-4 pb-4">

          <button
              type="button"
              class="btn btn-light"
              @click="hide"
              :disabled="isLoading"
          >
            {{ successMessage ? 'Cerrar' : 'Cancelar' }}
          </button>

          <button
              v-if="!successMessage"
              type="button"
              class="btn btn-primary px-4"
              @click="handleSubmit"
              :disabled="isLoading"
          >
                        <span
                            v-if="isLoading"
                            class="spinner-border spinner-border-sm me-2"
                            role="status"
                        ></span>

            {{ isLoading ? 'Cambiando...' : 'Cambiar contraseña' }}
          </button>

        </div>

      </div>
    </div>
  </div>
</template>
