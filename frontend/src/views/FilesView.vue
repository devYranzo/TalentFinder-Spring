
<script setup>
import { onMounted, onUnmounted, ref } from 'vue';
import { useFileManager } from '@/composables/useFiles';

import FilesToolbar from '@/components/files/FilesToolbar.vue';
import FilesBreadcrumb from '@/components/files/FilesBreadcrumb.vue';
import FolderGrid from '@/components/files/FolderGrid.vue';
import DocumentGrid from '@/components/files/DocumentGrid.vue';
import EmptyExplorer from '@/components/files/EmptyExplorer.vue';
import FolderContent from '@/components/files/FolderContent.vue';
import CreateFolderModal from '@/components/files/CreateFolderModal.vue';
import UploadDocumentModal from '@/components/files/UploadDocumentModal.vue';

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

// Modales
const showUploadModal = ref(false);
const showCreateFolderModal = ref(false);

// Formulario de creación de carpetas
const newFolderName = ref('');

// Mensajes
const statusMessage = ref('');
const statusType = ref('success');
let statusTimeout = null;

// Nombre de la carpeta actual
const activeFolderName = () => {
  if (activeFolderId.value === null) {
    return 'Home';
  }

  return folderList.value.find(
      folder => folder.id === activeFolderId.value
  )?.name || 'Carpeta';
};

// Documentos de la carpeta actual
const displayedDocuments = () => {
  if (activeFolderId.value === null) {
    return generalDocuments.value;
  }

  return folderDocuments.value[activeFolderId.value] || [];
};

// Navegación
const goHome = () => {
  activeFolderId.value = null;
};

const openFolder = (folderId) => {
  activeFolderId.value = folderId;
};

// Notificaciones
const showStatus = (message, type = 'success') => {
  statusMessage.value = message;
  statusType.value = type;

  clearTimeout(statusTimeout);

  statusTimeout = setTimeout(() => {
    statusMessage.value = '';
  }, 4000);
};

// Modal de creación de carpetas
const openCreateFolderModal = () => {
  newFolderName.value = '';
  showCreateFolderModal.value = true;
};

const closeCreateFolderModal = () => {
  if (isCreatingFolder.value) return;

  showCreateFolderModal.value = false;
  newFolderName.value = '';
};

const handleCreateFolder = async () => {
  const folderName = newFolderName.value.trim();

  if (!folderName) {
    showStatus('Introduce un nombre para la carpeta.', 'danger');
    return;
  }

  try {
    const result = await createFolder(folderName);

    activeFolderId.value = null;
    showCreateFolderModal.value = false;
    newFolderName.value = '';

    showStatus(`Carpeta "${result.name}" creada correctamente.`);
  } catch (error) {
    showStatus('No se pudo crear la carpeta.', 'danger');
  }
};

// Modal de subida
const openUploadModal = () => {
  selectedFile.value = null;
  selectedFolder.value = activeFolderId.value;
  showUploadModal.value = true;
};

const closeUploadModal = () => {
  if (isUploading.value) return;

  showUploadModal.value = false;
  resetUploadForm();
};

const resetUploadForm = () => {
  selectedFile.value = null;
  selectedFolder.value = activeFolderId.value;
};

// Actualizar archivo seleccionado
const updateSelectedFile = (file) => {
  selectedFile.value = file;
};

// Actualizar carpeta de destino
const updateSelectedFolder = (folderId) => {
  selectedFolder.value = folderId;
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
  <div class="container-fluid p-3 p-md-4 d-flex flex-column min-vh-100">

    <!-- CABECERA -->
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
      <div>
        <h1 class="h4 fw-bold mb-1">
          <i class="bi bi-folder2-open me-2"></i>
          Gestión de documentos
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

    <!-- ACCIONES -->
    <FilesToolbar
        :is-loading="isLoading"
        :show-create-button="activeFolderId === null"
        @refresh="handleRefresh"
        @create-folder="openCreateFolderModal"
        @upload="openUploadModal"
    />

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

    <!-- EXPLORADOR -->
    <div
        v-else
        class="island island-sm p-3 ms-0 flex-grow-1 d-flex flex-column"
    >

      <!-- BREADCRUMB -->
      <FilesBreadcrumb
          :folder-id="activeFolderId"
          :folder-name="activeFolderName()"
          @home="goHome"
      />

      <!-- HOME -->
      <template v-if="activeFolderId === null">

        <!-- EXPLORADOR VACÍO -->
        <EmptyExplorer
            v-if="folderList.length === 0 && generalDocuments.length === 0"
            @create-folder="openCreateFolderModal"
            @upload="openUploadModal"
        />

        <!-- CONTENIDO HOME -->
        <template v-else>

          <!-- CARPETAS -->
          <FolderGrid
              :folders="folderList"
              @open="openFolder"
              @create="openCreateFolderModal"
          />

          <!-- DOCUMENTOS GENERALES -->
          <DocumentGrid
              :documents="generalDocuments"
              empty-message="No hay documentos generales."
              @open="handleOpenPDF"
          />

        </template>
      </template>

      <!-- CONTENIDO DE CARPETA -->
      <FolderContent
          v-else
          :folder-name="activeFolderName()"
          :documents="displayedDocuments()"
          @upload="openUploadModal"
          @open-document="handleOpenPDF"
      />

    </div>

    <!-- MODAL CREAR CARPETA -->
    <CreateFolderModal
        :show="showCreateFolderModal"
        :folder-name="newFolderName"
        :is-creating="isCreatingFolder"
        @update:folder-name="newFolderName = $event"
        @close="closeCreateFolderModal"
        @submit="handleCreateFolder"
    />

    <!-- MODAL SUBIR DOCUMENTO -->
    <UploadDocumentModal
        :show="showUploadModal"
        :folder-list="folderList"
        :selected-file="selectedFile"
        :selected-folder="selectedFolder"
        :is-uploading="isUploading"
        :is-creating-folder="isCreatingFolder"
        @update:selected-file="updateSelectedFile"
        @update:selected-folder="updateSelectedFolder"
        @invalid-file="showStatus($event, 'danger')"
        @close="closeUploadModal"
        @submit="handleUploadFile"
    />

  </div>
</template>
