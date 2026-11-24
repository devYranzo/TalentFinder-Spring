
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Modal } from 'bootstrap'

const props = defineProps<{
  title: string
  message: string
  confirmText: string
  isDanger?: boolean
  isLoading?: boolean
}>()

const emit = defineEmits<{
  confirm: []
}>()

const modalElement = ref<HTMLElement | null>(null)
let modalInstance: Modal | null = null

function show() {
  modalInstance?.show()
}

function hide() {
  modalInstance?.hide()
}

defineExpose({
  show,
  hide,
})

onMounted(() => {
  if (modalElement.value) {
    modalInstance = new Modal(modalElement.value, {
      backdrop: 'static',
      keyboard: false,
    })
  }
})

onBeforeUnmount(() => {
  modalInstance?.dispose()
  modalInstance = null
})
</script>

<template>
  <div
      ref="modalElement"
      class="modal fade"
      tabindex="-1"
      aria-hidden="true"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content glass border-0">
        <div class="modal-header border-0">
          <h5 class="modal-title">
            {{ title }}
          </h5>

          <button
              type="button"
              class="btn-close"
              data-bs-dismiss="modal"
              aria-label="Cerrar"
              :disabled="isLoading"
          />
        </div>

        <div class="modal-body">
          <div class="d-flex align-items-start gap-3">
            <div
                class="rounded-circle p-3"
                :class="isDanger
                ? 'bg-danger-subtle text-danger'
                : 'bg-warning-subtle text-warning-emphasis'"
            >
              <i
                  class="bi"
                  :class="isDanger ? 'bi-exclamation-triangle' : 'bi-info-circle'"
              />
            </div>

            <p class="mb-0">
              {{ message }}
            </p>
          </div>
        </div>

        <div class="modal-footer border-0">
          <button
              type="button"
              class="btn btn-outline-secondary"
              data-bs-dismiss="modal"
              :disabled="isLoading"
          >
            Cancelar
          </button>

          <button
              type="button"
              class="btn"
              :class="isDanger ? 'btn-danger' : 'btn-warning'"
              :disabled="isLoading"
              @click="emit('confirm')"
          >
            <span
                v-if="isLoading"
                class="spinner-border spinner-border-sm me-2"
                aria-hidden="true"
            />

            {{ isLoading ? 'Procesando...' : confirmText }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
