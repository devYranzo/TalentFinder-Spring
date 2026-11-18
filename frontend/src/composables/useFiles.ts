
import { ref } from 'vue';
import filemanagerService from '@/api/files';

export function useFileManager() {
    // Carpetas
    const folderList = ref([]);

    // Documentos generales (sin carpeta)
    const generalDocuments = ref([]);

    // Documentos organizados por ID de carpeta
    const folderDocuments = ref({});

    // Estado de selección
    const selectedFile = ref(null);
    const selectedFolder = ref(null);

    // Estados de carga
    const isUploading = ref(false);
    const isCreatingFolder = ref(false);
    const isLoading = ref(false);

    // Obtener carpetas
    const fetchFolders = async () => {
        try {
            const response = await filemanagerService.getFolders();

            folderList.value = response.folders || [];

            return response;
        } catch (error) {
            console.error('Error al obtener carpetas:', error);
            throw error;
        }
    };

    // Recuperar todos los documentos, incluyendo todas sus páginas
    const fetchAllDocuments = async (options = {}) => {
        const firstPage = await filemanagerService.listFiles({
            ...options,
            page: 0,
            size: 20
        });

        let documents = [...firstPage.items];

        for (let page = 1; page < firstPage.totalPages; page++) {
            const response = await filemanagerService.listFiles({
                ...options,
                page,
                size: 20
            });

            documents.push(...response.items);
        }

        return documents;
    };

    // Obtener documentos generales y los de todas las carpetas
    const fetchFiles = async () => {
        isLoading.value = true;

        try {
            // Documentos sin carpeta
            const general = await fetchAllDocuments({
                unfiled: true
            });

            generalDocuments.value = general;

            // Documentos de cada carpeta
            const results = await Promise.all(
                folderList.value.map(async (folder) => {
                    const documents = await fetchAllDocuments({
                        folderId: folder.id
                    });

                    return {
                        folderId: folder.id,
                        documents
                    };
                })
            );

            const documentsByFolder = {};

            results.forEach(({ folderId, documents }) => {
                documentsByFolder[folderId] = documents;
            });

            folderDocuments.value = documentsByFolder;

            return {
                generalDocuments: generalDocuments.value,
                folderDocuments: folderDocuments.value
            };
        } catch (error) {
            console.error('Error al obtener documentos:', error);
            throw error;
        } finally {
            isLoading.value = false;
        }
    };

    // Actualizar carpetas y documentos
    const refreshFiles = async () => {
        await fetchFolders();
        await fetchFiles();
    };

    // Subir documento
    const uploadFile = async (file, folderId = null) => {
        if (!file) {
            throw new Error('Por favor, selecciona un archivo.');
        }

        isUploading.value = true;

        try {
            const response = await filemanagerService.uploadFile(
                file,
                folderId
            );

            await refreshFiles();

            return response;
        } catch (error) {
            console.error('Error al subir el documento:', error);
            throw error;
        } finally {
            isUploading.value = false;
        }
    };

    // Crear carpeta
    const createFolder = async (folderName) => {
        if (!folderName || !folderName.trim()) {
            throw new Error('Por favor, ingresa un nombre para la carpeta.');
        }

        isCreatingFolder.value = true;

        try {
            const response = await filemanagerService.createFolder(
                folderName.trim()
            );

            await refreshFiles();

            return response;
        } catch (error) {
            console.error('Error al crear la carpeta:', error);
            throw error;
        } finally {
            isCreatingFolder.value = false;
        }
    };

    // Visualizar PDF
    const openPDF = async (document) => {
        try {
            await filemanagerService.viewFile(
                document.id,
                document.filename
            );
        } catch (error) {
            console.error('Error al abrir el PDF:', error);
            throw error;
        }
    };

    // Mover documento
    const moveFile = async (documentId, folderId = null) => {
        try {
            const response = await filemanagerService.moveFile(
                documentId,
                folderId
            );

            await refreshFiles();

            return response;
        } catch (error) {
            console.error('Error al mover el documento:', error);
            throw error;
        }
    };

    // Eliminar documento
    const deleteFile = async (documentId) => {
        try {
            await filemanagerService.deleteFile(documentId);
            await refreshFiles();
        } catch (error) {
            console.error('Error al eliminar el documento:', error);
            throw error;
        }
    };

    // Renombrar carpeta
    const renameFolder = async (folderId, name) => {
        try {
            const response = await filemanagerService.renameFolder(
                folderId,
                name
            );

            await refreshFiles();

            return response;
        } catch (error) {
            console.error('Error al renombrar la carpeta:', error);
            throw error;
        }
    };

    // Eliminar carpeta
    const deleteFolder = async (folderId) => {
        try {
            await filemanagerService.deleteFolder(folderId);
            await refreshFiles();
        } catch (error) {
            console.error('Error al eliminar la carpeta:', error);
            throw error;
        }
    };

    return {
        folderList,
        generalDocuments,
        folderDocuments,

        selectedFile,
        selectedFolder,

        isUploading,
        isCreatingFolder,
        isLoading,

        fetchFolders,
        fetchFiles,
        refreshFiles,

        uploadFile,
        createFolder,
        openPDF,

        moveFile,
        deleteFile,
        renameFolder,
        deleteFolder
    };
}
