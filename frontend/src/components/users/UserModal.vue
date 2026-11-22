
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Modal } from 'bootstrap'
import type { UserForm } from '@/api/users'

defineProps<{
  formData: UserForm
  isEdit: boolean
  isSaving?: boolean
}>()

const emit = defineEmits<{
  save: []
}>()

const modalElement = ref<HTMLElement | null>(null)

let modalInstance: Modal | null = null

onMounted(() => {
  if (modalElement.value) {
    modalInstance = new Modal(modalElement.value)
  }
})

onBeforeUnmount(() => {
  modalInstance?.dispose()
})

function show() {
  modalInstance?.show()
}

function hide() {
  modalInstance?.hide()
}

defineExpose({ show, hide })
</script>

<template>
  <div
      ref="modalElement"
      class="modal fade"
      tabindex="-1"
      aria-labelledby="userModalLabel"
      aria-hidden="true"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content glass border-0 rounded-4 shadow">

        <div class="modal-header border-0 px-4 pt-4">
          <div>
            <h5 id="userModalLabel" class="modal-title fw-bold">
              {{ isEdit ? 'Editar usuario' : 'Nuevo usuario' }}
            </h5>

            <p class="text-body-secondary small mb-0">
              {{
                isEdit
                    ? 'Modifica los datos y permisos del usuario'
                    : 'Introduce los datos del nuevo usuario'
              }}
            </p>
          </div>

          <button
              type="button"
              class="btn-close"
              data-bs-dismiss="modal"
              aria-label="Cerrar"
              :disabled="isSaving"
          ></button>
        </div>

        <form @submit.prevent="emit('save')">
          <div class="modal-body px-4 py-3">

            <div class="mb-3">
              <label for="firstNameInput" class="form-label fw-semibold">
                Nombre
              </label>

              <input
                  id="firstNameInput"
                  v-model="formData.firstName"
                  type="text"
                  class="form-control"
                  placeholder="Nombre"
                  autocomplete="given-name"
                  maxlength="100"
                  required
                  :disabled="isSaving"
              >
            </div>

            <div class="mb-3">
              <label for="lastNameInput" class="form-label fw-semibold">
                Apellidos
              </label>

              <input
                  id="lastNameInput"
                  v-model="formData.lastName"
                  type="text"
                  class="form-control"
                  placeholder="Apellidos"
                  autocomplete="family-name"
                  maxlength="100"
                  required
                  :disabled="isSaving"
              >
            </div>

            <div class="mb-3">
              <label for="emailInput" class="form-label fw-semibold">
                Correo electrónico
              </label>

              <input
                  id="emailInput"
                  v-model="formData.email"
                  type="email"
                  class="form-control"
                  placeholder="usuario@empresa.com"
                  autocomplete="email"
                  maxlength="254"
                  required
                  :disabled="isSaving || isEdit"
              >

              <div v-if="isEdit" class="form-text">
                El correo electrónico no se puede modificar.
              </div>
            </div>

            <div v-if="!isEdit" class="mb-3">
              <label for="passwordInput" class="form-label fw-semibold">
                Contraseña
              </label>

              <input
                  id="passwordInput"
                  v-model="formData.password"
                  type="password"
                  class="form-control"
                  placeholder="Introduce una contraseña"
                  autocomplete="new-password"
                  minlength="12"
                  maxlength="128"
                  required
                  :disabled="isSaving"
              >

              <div class="form-text">
                La contraseña debe tener entre 12 y 128 caracteres.
              </div>
            </div>

            <div class="mb-2">
              <label for="roleSelect" class="form-label fw-semibold">
                Rol del usuario
              </label>

              <select
                  id="roleSelect"
                  v-model="formData.role"
                  class="form-select"
                  required
                  :disabled="isSaving"
              >
                <option value="ADMIN">Administrador</option>
                <option value="RECRUITER">Recruiter</option>
              </select>

              <div class="form-text">
                El rol determina los permisos de acceso al sistema.
              </div>
            </div>

          </div>

          <div class="modal-footer border-0 px-4 pb-4">
            <button
                type="button"
                class="btn btn-light"
                data-bs-dismiss="modal"
                :disabled="isSaving"
            >
              Cancelar
            </button>

            <button
                type="submit"
                class="btn btn-primary px-4"
                :disabled="isSaving"
            >
              <span
                  v-if="isSaving"
                  class="spinner-border spinner-border-sm me-2"
                  role="status"
              ></span>

              {{
                isSaving
                    ? 'Guardando...'
                    : isEdit
                        ? 'Guardar cambios'
                        : 'Crear usuario'
              }}
            </button>
          </div>
        </form>

      </div>
    </div>
  </div>
</template>
