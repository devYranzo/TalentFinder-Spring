
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

// Estado de navegación
const activeFolderId = ref(null);

// Estado del buscador y ordenación
const searchTerm = ref('');
const sortOrder = ref('asc');

// Estado de modales
const showCreateFolderModal = ref(false);
const showUploadModal = ref(false);
const folderName = ref('');

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
  searchTerm.value = '';

  await refreshFiles();
};

const goHome = () => {
  activeFolderId.value = null;
  searchTerm.value = '';
};

const handleRefresh = async () => {
  await refreshFiles();
};

// Apertura y cierre de modales
const openCreateFolderModal = () => {
  folderName.value = '';
  showCreateFolderModal.value = true;
};

const closeCreateFolderModal = () => {
  showCreateFolderModal.value = false;
};

const openUploadModal = () => {
  selectedFolder.value = activeFolderId.value;
  showUploadModal.value = true;
};

const closeUploadModal = () => {
  showUploadModal.value = false;
};

// Creación de carpeta
const handleCreateFolder = async () => {
  if (!folderName.value.trim()) {
    return;
  }

  await createFolder(folderName.value);

  folderName.value = '';
  showCreateFolderModal.value = false;

  await refreshFiles();
};

// Subida de documentos
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

  await refreshFiles();
};

// Clic en documento
const handleOpenDocument = (document) => {
  openPDF(document);
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
        <h1 class="h4 fw-bold mb-1">
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
        @home="goHome"
    />

    <div
        class="island island-sm p-2 ps-0 ms-0 flex-grow-1 d-flex flex-column"
    >
      <!-- Vista principal -->
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
          />

          <DocumentGrid
              :documents="filteredDocuments"
              :empty-message="documentEmptyMessage"
              @open="handleOpenDocument"
          />

        </template>
      </template>

      <!-- Contenido de carpeta -->
      <template v-else>

        <FolderContent
            :folder-name="activeFolderName"
            :documents="filteredDocuments"
            :has-search="hasSearch"
            @upload="openUploadModal"
            @open-document="handleOpenDocument"
        />

      </template>
    </div>

    <!-- Modal crear carpeta -->
    <CreateFolderModal
        :show="showCreateFolderModal"
        :folder-name="folderName"
        :is-creating="isCreatingFolder"
        @update:folder-name="folderName = $event"
        @close="closeCreateFolderModal"
        @submit="handleCreateFolder"
    />

    <!-- Modal subir documento -->
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

  </div>
</template>
