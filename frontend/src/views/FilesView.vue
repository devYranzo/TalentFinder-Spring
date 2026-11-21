
<script setup>
import { onMounted, onUnmounted, ref } from 'vue';
import { useFileManager } from '@/composables/useFiles';

const {
  folderList,
  generalDocuments,
  folderDocuments,
  selectedFile,
  selectedFolder,
  isUploading,
  isCreatingFolder,
  isLoading,
  refreshFiles,
  uploadFile,
  createFolder,
  openPDF
} = useFileManager();

// Navegación
// null = Home
const activeFolderId = ref(null);

// Modal de subida
const showUploadModal = ref(false);
const fileInput = ref(null);

// Creación de carpetas
const createFolderMode = ref(false);
const newFolderName = ref('');

// Mensajes
const statusMessage = ref('');
const statusType = ref('success');
let statusTimeout = null;

// Documentos de la carpeta actual
const displayedDocuments = () => {
  if (activeFolderId.value === null) {
    return generalDocuments.value;
  }

  return folderDocuments.value[activeFolderId.value] || [];
};

// Nombre de la carpeta actual
const activeFolderName = () => {
  if (activeFolderId.value === null) {
    return 'Home';
  }

  return folderList.value.find(
      folder => folder.id === activeFolderId.value
  )?.name || 'Carpeta';
};

// Navegar a Home
const goHome = () => {
  activeFolderId.value = null;
};

// Abrir carpeta mediante doble clic
const openFolder = (folderId) => {
  activeFolderId.value = folderId;
};

// Mostrar notificaciones
const showStatus = (message, type = 'success') => {
  statusMessage.value = message;
  statusType.value = type;

  clearTimeout(statusTimeout);

  statusTimeout = setTimeout(() => {
    statusMessage.value = '';
  }, 4000);
};

// Formatear tamaño
const formatSize = (bytes) => {
  if (!bytes) return '0 KB';

  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  return `${(bytes / 1024).toFixed(1)} KB`;
};

// Abrir modal de subida
const openUploadModal = () => {
  selectedFolder.value = activeFolderId.value;
  showUploadModal.value = true;
};

// Cerrar modal
const closeUploadModal = () => {
  if (isUploading.value) return;

  showUploadModal.value = false;
  resetUploadForm();
};

// Limpiar formulario
const resetUploadForm = () => {
  selectedFile.value = null;
  selectedFolder.value = activeFolderId.value;

  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

// Seleccionar archivo
const onFileSelected = (event) => {
  const file = event.target.files?.[0] || null;

  if (file && file.type !== 'application/pdf') {
    selectedFile.value = null;
    event.target.value = '';
    showStatus('Solo se permiten archivos PDF.', 'danger');
    return;
  }

  if (file && file.size > 10 * 1024 * 1024) {
    selectedFile.value = null;
    event.target.value = '';
    showStatus('El archivo no puede superar los 10 MB.', 'danger');
    return;
  }

  selectedFile.value = file;
};

// Subir documento
const handleUploadFile = async () => {
  if (!selectedFile.value) {
    showStatus('Selecciona un archivo PDF.', 'danger');
    return;
  }

  const filename = selectedFile.value.name;

  try {
    await uploadFile(
        selectedFile.value,
        selectedFolder.value
    );

    showUploadModal.value = false;
    resetUploadForm();

    showStatus(`Archivo "${filename}" subido correctamente.`);
  } catch (error) {
    showStatus('No se pudo subir el documento.', 'danger');
  }
};

// Alternar creación de carpetas
const toggleCreateFolderMode = () => {
  createFolderMode.value = !createFolderMode.value;
  newFolderName.value = '';
};

// Crear carpeta
const handleCreateFolder = async () => {
  if (!newFolderName.value.trim()) {
    showStatus('Introduce un nombre para la carpeta.', 'danger');
    return;
  }

  try {
    const result = await createFolder(newFolderName.value.trim());

    // Después de crearla, permanecemos en Home
    activeFolderId.value = null;

    newFolderName.value = '';
    createFolderMode.value = false;

    showStatus(`Carpeta "${result.name}" creada correctamente.`);
  } catch (error) {
    showStatus('No se pudo crear la carpeta.', 'danger');
  }
};

// Visualizar documento
const handleOpenPDF = async (document) => {
  try {
    await openPDF(document);
  } catch (error) {
    showStatus('No se pudo abrir el documento.', 'danger');
  }
};

// Actualizar explorador
const handleRefresh = async () => {
  try {
    await refreshFiles();
    showStatus('Explorador actualizado correctamente.');
  } catch (error) {
    showStatus('Error al actualizar los documentos.', 'danger');
  }
};

// Inicialización
onMounted(async () => {
  try {
    await refreshFiles();
  } catch (error) {
    showStatus('Error al cargar el explorador.', 'danger');
  }
});

onUnmounted(() => {
  clearTimeout(statusTimeout);
});
</script>

<template>
  <div class="container-fluid d-flex flex-column flex-grow-1 island mx-0 mb-0">

    <!-- CABECERA -->
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
      <div>
        <h1 class="h4 fw-bold mb-1">
          <i class="bi bi-folder2-open me-2"></i>
          Gestión de CVs
        </h1>
        <p class="text-secondary small mb-0">
          Organiza y consulta los currículums de tus candidatos.
        </p>
      </div>
    </div>

    <!-- MENSAJES -->
    <div
        v-if="statusMessage"
        :class="statusType === 'success' ? 'alert-success' : 'alert-danger'"
        class="alert alert-dismissible fade show"
        role="alert"
    >
      <i
          :class="statusType === 'success'
          ? 'bi bi-check-circle me-2'
          : 'bi bi-exclamation-circle me-2'"
      ></i>

      {{ statusMessage }}

      <button
          aria-label="Cerrar"
          class="btn-close"
          type="button"
          @click="statusMessage = ''"
      ></button>
    </div>

    <!-- FORMULARIO NUEVA CARPETA -->
    <div v-if="createFolderMode" class="card shadow-sm mb-4">
      <div class="card-body">
        <form
            class="row g-2 align-items-center"
            @submit.prevent="handleCreateFolder"
        >
          <div class="col-12 col-md-6">
            <input
                v-model="newFolderName"
                :disabled="isCreatingFolder"
                autofocus
                class="form-control"
                placeholder="Nombre de la nueva carpeta..."
                type="text"
            />
          </div>

          <div class="col-auto">
            <button
                :disabled="isCreatingFolder"
                class="btn btn-success"
                type="submit"
            >
              <span
                  v-if="isCreatingFolder"
                  class="spinner-border spinner-border-sm me-1"
              ></span>

              <i v-else class="bi bi-check-lg me-1"></i>
              Crear
            </button>
          </div>

          <div class="col-auto">
            <button
                :disabled="isCreatingFolder"
                class="btn btn-outline-secondary"
                type="button"
                @click="toggleCreateFolderMode"
            >
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- ACCIONES -->
    <div class="d-flex gap-2 mb-3">
      <button
          :disabled="isLoading"
          class="btn btn-outline-secondary"
          title="Actualizar"
          type="button"
          @click="handleRefresh"
      >
        <span
            v-if="isLoading"
            class="spinner-border spinner-border-sm"
        ></span>

        <i v-else class="bi bi-arrow-clockwise"></i>
      </button>

      <button
          v-if="activeFolderId === null"
          class="btn btn-outline-primary"
          type="button"
          @click="toggleCreateFolderMode"
      >
        <i class="bi bi-folder-plus me-1"></i>
        Nueva carpeta
      </button>

      <button
          class="btn btn-primary"
          type="button"
          @click="openUploadModal"
      >
        <i class="bi bi-cloud-arrow-up me-1"></i>
        Subir archivo
      </button>
    </div>

    <!-- CARGANDO -->
    <div
        v-if="isLoading"
        class="text-center py-5"
    >
      <div class="spinner-border text-primary"></div>

      <p class="text-secondary mt-3 mb-0">
        Cargando documentos...
      </p>
    </div>

    <!-- EXPLORADOR: UN ÚNICO CONTENEDOR -->
    <div
        class="island p-3 ps-0 ms-0 flex-grow-1"
    >
      <!-- BREADCRUMB -->
      <nav aria-label="breadcrumb" class=" mb-3 fs-5">
        <ol class="breadcrumb mb-0">
          <li
              class="breadcrumb-item"
              :class="{ active: activeFolderId === null }"
          >
            <a href="#" class=" text-white text-decoration-none" @click="goHome()">
              <i class="bi bi-house-door" />
              Inicio
            </a>
          </li>

          <li
              v-if="activeFolderId !== null"
              aria-current="page"
              class="breadcrumb-item active"
          >
            {{ activeFolderName() }}
          </li>
        </ol>
      </nav>

      <!-- HOME -->
      <template v-if="activeFolderId === null">

        <!-- CARPETAS -->
        <section class="mb-4">

          <!-- GRID DE CARPETAS -->
          <div
              v-if="folderList.length > 0"
              class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 row-cols-xl-6 g-3"
          >
            <div
                v-for="folder in folderList"
                :key="folder.id"
                class="col"
            >
              <button
                  type="button"
                  class="card h-100 w-100 bg-transparent text-start"
                  @click.stop
                  @dblclick="openFolder(folder.id)"
                  @keydown.enter="openFolder(folder.id)"
                  :aria-label="'Abrir carpeta ' + folder.name"
                  title="Haz doble clic para abrir"
              >
                <div class="card-body d-flex align-items-center">
                  <i class="bi bi-folder-fill text-warning"></i>

                  <span class="fw-semibold ms-2 text-truncate">
                    {{ folder.name }}
                  </span>
                </div>
              </button>
            </div>
          </div>

          <!-- SIN CARPETAS -->
          <div
              v-else
              class="text-center text-secondary py-5"
          >
            <i class="bi bi-folder2-open display-3"></i>

            <h3 class="h6 fw-bold mt-3">
              No hay carpetas
            </h3>

            <p class="small">
              Crea una carpeta para organizar tus documentos.
            </p>

            <button
                class="btn btn-outline-primary btn-sm"
                type="button"
                @click="toggleCreateFolderMode"
            >
              <i class="bi bi-folder-plus me-1"></i>
              Crear primera carpeta
            </button>
          </div>
        </section>

        <!-- DOCUMENTOS GENERALES -->
        <section>

          <!-- DOCUMENTOS -->
          <div
              v-if="generalDocuments.length > 0"
              class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 row-cols-xl-6 g-3"
          >
            <div
                v-for="document in generalDocuments"
                :key="document.id"
                class="col"
            >
              <div class="card h-100 shadow-sm bg-transparent overflow-hidden">

                <!-- VISTA PREVIA -->
                <button
                    type="button"
                    class="card-body border-0 bg-transparent d-flex flex-column align-items-center justify-content-center p-4"
                    @click="handleOpenPDF(document)"
                    :aria-label="'Ver documento ' + document.filename"
                >
                  <i class="bi bi-file-earmark-pdf-fill text-danger display-3"></i>
                </button>

                <!-- FOOTER -->
                <div class="card-footer bg-transparent border-top d-flex align-items-center gap-2 py-2 px-2">
                  <span
                      class="small fw-semibold text-truncate flex-grow-1"
                      :title="document.filename"
                  >
                    <i class="bi bi-file-earmark-pdf-fill text-danger me-1"></i>
                    {{ document.filename }}
                  </span>

                  <div class="dropdown flex-shrink-0">
                    <button
                        type="button"
                        class="btn btn-sm bg-transparent border-0"
                        data-bs-toggle="dropdown"
                        aria-expanded="false"
                        :aria-label="'Opciones de ' + document.filename"
                        @click.stop
                    >
                      <i class="bi bi-three-dots-vertical"></i>
                    </button>

                    <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                      <li>
                        <button
                            class="dropdown-item"
                            type="button"
                            @click="handleOpenPDF(document)"
                        >
                          <i class="bi bi-eye me-2"></i>
                          Ver documento
                        </button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- SIN DOCUMENTOS GENERALES -->
          <div
              v-else
              class="text-center text-secondary py-4"
          >
            <i class="bi bi-file-earmark display-4"></i>

            <p class="small mb-0 mt-2">
              No hay documentos generales.
            </p>
          </div>
        </section>

      </template>

      <!-- CONTENIDO DE UNA CARPETA -->
      <template v-else>

        <section>

          <!-- CABECERA DE CARPETA -->
          <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
            <div>
              <h2 class="h5 fw-bold mb-1">
                <i class="bi bi-folder-fill text-warning me-2"></i>
                {{ activeFolderName() }}
              </h2>

              <span class="text-secondary small">
                {{ displayedDocuments().length }}
                {{ displayedDocuments().length === 1 ? 'documento' : 'documentos' }}
              </span>
            </div>

            <button
                class="btn btn-sm btn-outline-primary"
                type="button"
                @click="openUploadModal"
            >
              <i class="bi bi-plus-lg me-1"></i>
              Añadir documento
            </button>
          </div>

          <!-- DOCUMENTOS DE LA CARPETA -->
          <div
              v-if="displayedDocuments().length > 0"
              class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 row-cols-xl-6 g-3"
          >
            <div
                v-for="document in displayedDocuments()"
                :key="document.id"
                class="col"
            >
              <div class="card h-100 shadow-sm glass overflow-hidden">

                <!-- VISTA PREVIA -->
                <button
                    type="button"
                    class="card-body border-0 bg-transparent d-flex flex-column align-items-center justify-content-center p-4"
                    @click="handleOpenPDF(document)"
                    :aria-label="'Ver documento ' + document.filename"
                >
                  <i class="bi bi-file-earmark-pdf-fill text-danger display-3"></i>
                </button>

                <!-- FOOTER -->
                <div class="card-footer bg-transparent border-top d-flex align-items-center gap-2 py-2 px-2">
                  <span
                      class="small fw-semibold text-truncate flex-grow-1"
                      :title="document.filename"
                  >
                    <i class="bi bi-file-earmark-pdf-fill text-danger me-1"></i>
                    {{ document.filename }}
                  </span>

                  <div class="dropdown flex-shrink-0">
                    <button
                        type="button"
                        class="btn btn-sm bg-transparent border-0"
                        data-bs-toggle="dropdown"
                        aria-expanded="false"
                        :aria-label="'Opciones de ' + document.filename"
                        @click.stop
                    >
                      <i class="bi bi-three-dots-vertical"></i>
                    </button>

                    <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                      <li>
                        <button
                            class="dropdown-item"
                            type="button"
                            @click="handleOpenPDF(document)"
                        >
                          <i class="bi bi-eye me-2"></i>
                          Ver documento
                        </button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- CARPETA VACÍA -->
          <div
              v-else
              class="card border-0 bg-transparent text-center py-5"
          >
            <div class="card-body">
              <i class="bi bi-folder2-open display-3 text-secondary"></i>

              <h3 class="h6 fw-bold mt-3">
                Esta carpeta está vacía
              </h3>

              <p class="text-secondary small">
                Todavía no contiene documentos.
              </p>

              <button
                  class="btn btn-primary btn-sm"
                  type="button"
                  @click="openUploadModal"
              >
                <i class="bi bi-cloud-arrow-up me-1"></i>
                Subir primer documento
              </button>
            </div>
          </div>
        </section>

      </template>
    </div>

    <!-- MODAL SUBIDA -->
    <div
        v-if="showUploadModal"
        aria-labelledby="uploadModalTitle"
        aria-modal="true"
        class="modal fade show d-block"
        role="dialog"
        style="background-color: rgba(0,0,0,.5)"
        tabindex="-1"
    >
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">

          <!-- CABECERA -->
          <div class="modal-header">
            <h5 id="uploadModalTitle" class="modal-title fw-semibold">
              <i class="bi bi-cloud-arrow-up me-2 text-primary"></i>
              Subir documento
            </h5>

            <button
                :disabled="isUploading || isCreatingFolder"
                class="btn-close"
                type="button"
                @click="closeUploadModal"
            ></button>
          </div>

          <!-- CUERPO -->
          <div class="modal-body">

            <!-- CARPETA DESTINO -->
            <div class="mb-3">
              <label class="form-label fw-semibold">
                Carpeta de destino
              </label>

              <select
                  v-model="selectedFolder"
                  :disabled="isUploading"
                  class="form-select"
              >
                <option :value="null">
                  General
                </option>

                <option
                    v-for="folder in folderList"
                    :key="folder.id"
                    :value="folder.id"
                >
                  {{ folder.name }}
                </option>
              </select>
            </div>

            <!-- ARCHIVO -->
            <div class="mb-3">
              <label class="form-label fw-semibold">
                Archivo PDF
              </label>

              <input
                  ref="fileInput"
                  :disabled="isUploading"
                  accept=".pdf,application/pdf"
                  class="form-control"
                  type="file"
                  @change="onFileSelected"
              />

              <div class="form-text">
                Solo archivos PDF. Tamaño máximo: 10 MB.
              </div>
            </div>

            <!-- ARCHIVO SELECCIONADO -->
            <div
                v-if="selectedFile"
                class="alert alert-info d-flex align-items-center gap-3 mb-0"
            >
              <i class="bi bi-file-earmark-pdf-fill fs-3 text-danger"></i>

              <div class="overflow-hidden">
                <div class="fw-semibold text-truncate">
                  {{ selectedFile.name }}
                </div>

                <small>
                  {{ formatSize(selectedFile.size) }}
                </small>
              </div>
            </div>
          </div>

          <!-- FOOTER -->
          <div class="modal-footer">
            <button
                :disabled="isUploading || isCreatingFolder"
                class="btn btn-outline-secondary"
                type="button"
                @click="closeUploadModal"
            >
              Cancelar
            </button>

            <button
                :disabled="!selectedFile || isUploading || isCreatingFolder"
                class="btn btn-primary"
                type="button"
                @click="handleUploadFile"
            >
              <span
                  v-if="isUploading"
                  class="spinner-border spinner-border-sm me-2"
              ></span>

              <i v-else class="bi bi-upload me-1"></i>

              {{ isUploading ? 'Subiendo...' : 'Subir documento' }}
            </button>
          </div>

        </div>
      </div>
    </div>

  </div>
</template>
