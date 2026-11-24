
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useFileManager } from '@/composables/useFiles.ts';

import FilesToolbar from '@/components/files/FilesToolbar.vue';
import FilesBreadcrumb from '@/components/files/FilesBreadcrumb.vue';
import FolderGrid from '@/components/files/FolderGrid.vue';
import DocumentGrid from '@/components/files/DocumentGrid.vue';
import EmptyExplorer from '@/components/files/EmptyExplorer.vue';
import FolderContent from '@/components/files/FolderContent.vue';

import CreateFolderModal from '@/components/files/CreateFolderModal.vue';
import UploadDocumentModal from '@/components/files/UploadDocumentModal.vue';
import MoveDocumentModal from '@/components/files/MoveDocumentModal.vue';
import DeleteDocumentModal from '@/components/files/DeleteDocumentModal.vue';
import RenameFolderModal from '@/components/files/RenameFolderModal.vue';
import DeleteFolderModal from '@/components/files/DeleteFolderModal.vue';

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
  openPDF,
  moveFile,
  deleteFile,
  renameFolder,
  deleteFolder
} = useFileManager();

// Estado de navegación
const activeFolderId = ref(null);
const lastOpenedFolderId = ref(null);

// Comprueba si existe una última carpeta válida a la que volver
const canGoForward = computed(() => {
  if (activeFolderId.value !== null || lastOpenedFolderId.value === null) {
    return false;
  }

  return (folderList.value ?? []).some(
      folder => String(folder.id) === String(lastOpenedFolderId.value)
  );
});

// Estado del buscador y ordenación
const searchTerm = ref('');
const sortOrder = ref('asc');

// Estado de modales
const showCreateFolderModal = ref(false);
const showUploadModal = ref(false);
const showMoveModal = ref(false);
const showDeleteModal = ref(false);
const showRenameFolderModal = ref(false);
const showDeleteFolderModal = ref(false);

// Datos de formularios
const folderName = ref('');
const renameFolderName = ref('');

// Elementos seleccionados
const movingDocument = ref(null);
const deletingDocument = ref(null);
const renamingFolder = ref(null);
const deletingFolder = ref(null);

// Estados de operaciones
const isMoving = ref(false);
const isDeleting = ref(false);
const isRenamingFolder = ref(false);
const isDeletingFolder = ref(false);

// Normaliza texto para ignorar mayúsculas y acentos
const normalizeText = (value) => {
  return String(value ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase('es')
      .trim();
};

const hasSearch = computed(() => {
  return normalizeText(searchTerm.value).length > 0;
});

// Filtrado y ordenación de carpetas
const filteredFolders = computed(() => {
  const search = normalizeText(searchTerm.value);

  const folders = [...(folderList.value ?? [])];

  const filtered = folders.filter(folder => {
    return normalizeText(folder.name).includes(search);
  });

  return filtered.sort((a, b) => {
    const comparison = String(a.name ?? '').localeCompare(
        String(b.name ?? ''),
        'es',
        { sensitivity: 'base' }
    );

    return sortOrder.value === 'asc'
        ? comparison
        : -comparison;
  });
});

// Documentos correspondientes a la vista actual
const currentDocuments = computed(() => {
  if (activeFolderId.value === null) {
    return generalDocuments.value ?? [];
  }

  return folderDocuments.value?.[activeFolderId.value] ?? [];
});

// Filtrado y ordenación de documentos
const filteredDocuments = computed(() => {
  const search = normalizeText(searchTerm.value);

  const filtered = (currentDocuments.value ?? []).filter(document => {
    return normalizeText(document.filename).includes(search);
  });

  return filtered.sort((a, b) => {
    const comparison = String(a.filename ?? '').localeCompare(
        String(b.filename ?? ''),
        'es',
        { sensitivity: 'base' }
    );

    return sortOrder.value === 'asc'
        ? comparison
        : -comparison;
  });
});

// Nombre de la carpeta abierta
const activeFolderName = computed(() => {
  if (activeFolderId.value === null || activeFolderId.value === undefined) {
    return '';
  }

  const folder = (folderList.value ?? []).find(
      item => String(item.id) === String(activeFolderId.value)
  );

  return folder?.name ?? '';
});

// Mensaje para documentos vacíos
const documentEmptyMessage = computed(() => {
  if (hasSearch.value) {
    return 'No se encontraron documentos.';
  }

  return activeFolderId.value === null
      ? 'No hay documentos generales.'
      : 'Esta carpeta está vacía.';
});

// Navegación
const openFolder = async (folderId) => {
  if (folderId === null || folderId === undefined) {
    return;
  }

  activeFolderId.value = folderId;
  lastOpenedFolderId.value = folderId;
  searchTerm.value = '';

  await refreshFiles();
};

const goHome = () => {
  activeFolderId.value = null;
  searchTerm.value = '';
};

const goForward = async () => {
  if (!canGoForward.value) {
    return;
  }

  const folderId = lastOpenedFolderId.value;

  await openFolder(folderId);
};

const handleRefresh = async () => {
  await refreshFiles();
};

// Crear carpeta
const openCreateFolderModal = () => {
  folderName.value = '';
  showCreateFolderModal.value = true;
};

const closeCreateFolderModal = () => {
  showCreateFolderModal.value = false;
};

const handleCreateFolder = async () => {
  if (!folderName.value.trim()) {
    return;
  }

  await createFolder(folderName.value);

  folderName.value = '';
  showCreateFolderModal.value = false;
};

// Subir documento
const openUploadModal = () => {
  selectedFolder.value = activeFolderId.value;
  showUploadModal.value = true;
};

const closeUploadModal = () => {
  showUploadModal.value = false;
};

const handleUpload = async () => {
  if (!selectedFile.value) {
    return;
  }

  await uploadFile(
      selectedFile.value,
      selectedFolder.value
  );

  selectedFile.value = null;
  selectedFolder.value = null;
  showUploadModal.value = false;
};

// Abrir documento
const handleOpenDocument = (document) => {
  openPDF(document);
};

// Mover documento
const openMoveModal = (document) => {
  movingDocument.value = document;
  showMoveModal.value = true;
};

const closeMoveModal = () => {
  if (isMoving.value) {
    return;
  }

  showMoveModal.value = false;
  movingDocument.value = null;
};

const handleMoveDocument = async (folderId) => {
  if (!movingDocument.value || isMoving.value) {
    return;
  }

  isMoving.value = true;

  try {
    await moveFile(
        movingDocument.value.id,
        folderId
    );

    showMoveModal.value = false;
    movingDocument.value = null;
  } catch (error) {
    console.error('Error al mover el documento:', error);
  } finally {
    isMoving.value = false;
  }
};

// Eliminar documento
const openDeleteModal = (document) => {
  deletingDocument.value = document;
  showDeleteModal.value = true;
};

const closeDeleteModal = () => {
  if (isDeleting.value) {
    return;
  }

  showDeleteModal.value = false;
  deletingDocument.value = null;
};

const handleDeleteDocument = async () => {
  if (!deletingDocument.value || isDeleting.value) {
    return;
  }

  isDeleting.value = true;

  try {
    await deleteFile(deletingDocument.value.id);

    showDeleteModal.value = false;
    deletingDocument.value = null;
  } catch (error) {
    console.error('Error al eliminar el documento:', error);
  } finally {
    isDeleting.value = false;
  }
};

// Renombrar carpeta
const openRenameFolderModal = (folder) => {
  renamingFolder.value = folder;
  renameFolderName.value = folder.name;
  showRenameFolderModal.value = true;
};

const closeRenameFolderModal = () => {
  if (isRenamingFolder.value) {
    return;
  }

  showRenameFolderModal.value = false;
  renamingFolder.value = null;
  renameFolderName.value = '';
};

const handleRenameFolder = async () => {
  if (
      !renamingFolder.value ||
      !renameFolderName.value.trim() ||
      isRenamingFolder.value
  ) {
    return;
  }

  isRenamingFolder.value = true;

  try {
    await renameFolder(
        renamingFolder.value.id,
        renameFolderName.value.trim()
    );

    showRenameFolderModal.value = false;
    renamingFolder.value = null;
    renameFolderName.value = '';
  } catch (error) {
    console.error('Error al renombrar la carpeta:', error);
  } finally {
    isRenamingFolder.value = false;
  }
};

// Eliminar carpeta
const openDeleteFolderModal = (folder) => {
  deletingFolder.value = folder;
  showDeleteFolderModal.value = true;
};

const closeDeleteFolderModal = () => {
  if (isDeletingFolder.value) {
    return;
  }

  showDeleteFolderModal.value = false;
  deletingFolder.value = null;
};

const handleDeleteFolder = async () => {
  if (!deletingFolder.value || isDeletingFolder.value) {
    return;
  }

  isDeletingFolder.value = true;

  try {
    await deleteFolder(deletingFolder.value.id);

    // Si se elimina la última carpeta guardada, se limpia el historial
    if (
        String(lastOpenedFolderId.value) ===
        String(deletingFolder.value.id)
    ) {
      lastOpenedFolderId.value = null;
    }

    // Si se elimina la carpeta abierta, se vuelve a Home
    if (
        String(activeFolderId.value) ===
        String(deletingFolder.value.id)
    ) {
      activeFolderId.value = null;
    }

    showDeleteFolderModal.value = false;
    deletingFolder.value = null;
  } catch (error) {
    console.error('Error al eliminar la carpeta:', error);
  } finally {
    isDeletingFolder.value = false;
  }
};

onMounted(() => {
  refreshFiles();
});
</script>

<template>
  <div class="container-fluid p-3 p-md-4 d-flex flex-column min-vh-100">

    <!-- CABECERA -->
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
      <div>
        <h1 class="h3 fw-bold mb-1">
          <i class="bi bi-folder2-open me-2"></i>
          Gestión de documentos
        </h1>
        <p class="text-secondary small mb-0">
          Organiza y consulta los currículums de tus candidatos.
        </p>
      </div>
    </div>

    <FilesToolbar
        :is-loading="isLoading"
        :show-create-button="activeFolderId === null"
        :search-term="searchTerm"
        :sort-order="sortOrder"
        @update:search-term="searchTerm = $event"
        @update:sort-order="sortOrder = $event"
        @refresh="handleRefresh"
        @create-folder="openCreateFolderModal"
        @upload="openUploadModal"
    />

    <FilesBreadcrumb
        :folder-id="activeFolderId"
        :folder-name="activeFolderName"
        :can-go-forward="canGoForward"
        @home="goHome"
        @forward="goForward"
    />

    <div
        class="island island-sm p-2 ps-0 ms-0 flex-grow-1 d-flex flex-column"
    >
      <!-- VISTA PRINCIPAL -->
      <template v-if="activeFolderId === null">

        <EmptyExplorer
            v-if="
            folderList.length === 0 &&
            generalDocuments.length === 0 &&
            !hasSearch
          "
            @create-folder="openCreateFolderModal"
            @upload="openUploadModal"
        />

        <template v-else>

          <FolderGrid
              :folders="filteredFolders"
              :has-search="hasSearch"
              @open="openFolder"
              @create="openCreateFolderModal"
              @rename="openRenameFolderModal"
              @delete="openDeleteFolderModal"
          />

          <DocumentGrid
              :documents="filteredDocuments"
              :empty-message="documentEmptyMessage"
              @open="handleOpenDocument"
              @move="openMoveModal"
              @delete="openDeleteModal"
          />

        </template>
      </template>

      <!-- CONTENIDO DE CARPETA -->
      <template v-else>

        <FolderContent
            :folder-name="activeFolderName"
            :documents="filteredDocuments"
            :has-search="hasSearch"
            @upload="openUploadModal"
            @open-document="handleOpenDocument"
            @move-document="openMoveModal"
            @delete-document="openDeleteModal"
        />

      </template>
    </div>

    <!-- MODAL CREAR CARPETA -->
    <CreateFolderModal
        :show="showCreateFolderModal"
        :folder-name="folderName"
        :is-creating="isCreatingFolder"
        @update:folder-name="folderName = $event"
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
        @close="closeUploadModal"
        @update:selected-file="selectedFile = $event"
        @update:selected-folder="selectedFolder = $event"
        @submit="handleUpload"
    />

    <!-- MODAL MOVER DOCUMENTO -->
    <MoveDocumentModal
        :show="showMoveModal"
        :document="movingDocument"
        :folders="folderList"
        :current-folder-id="movingDocument?.folderId ?? null"
        :is-moving="isMoving"
        @close="closeMoveModal"
        @submit="handleMoveDocument"
    />

    <!-- MODAL ELIMINAR DOCUMENTO -->
    <DeleteDocumentModal
        :show="showDeleteModal"
        :document="deletingDocument"
        :is-deleting="isDeleting"
        @close="closeDeleteModal"
        @confirm="handleDeleteDocument"
    />

    <!-- MODAL RENOMBRAR CARPETA -->
    <RenameFolderModal
        :show="showRenameFolderModal"
        :folder-name="renameFolderName"
        :is-renaming="isRenamingFolder"
        @update:folder-name="renameFolderName = $event"
        @close="closeRenameFolderModal"
        @submit="handleRenameFolder"
    />

    <!-- MODAL ELIMINAR CARPETA -->
    <DeleteFolderModal
        :show="showDeleteFolderModal"
        :folder="deletingFolder"
        :is-deleting="isDeletingFolder"
        @close="closeDeleteFolderModal"
        @confirm="handleDeleteFolder"
    />

  </div>
</template>
