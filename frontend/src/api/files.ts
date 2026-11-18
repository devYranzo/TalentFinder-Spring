import api from './client';

export default {
    // Obtener carpetas y número de documentos generales
    async getFolders() {
        const response = await api.get('/folders');
        return response.data;
    },

    // Listar documentos generales o de una carpeta concreta
    async listFiles({
                        folderId = null,
                        unfiled = false,
                        page = 0,
                        size = 20
                    } = {}) {
        const params = {
            page,
            size
        };

        if (unfiled) {
            params.unfiled = true;
        } else if (folderId !== null && folderId !== undefined) {
            params.folderId = folderId;
        }

        const response = await api.get('/documents', {params});
        return response.data;
    },

    // Subir documento
    async uploadFile(file, folderId = null) {
        const formData = new FormData();

        formData.append('file', file);

        if (folderId !== null && folderId !== undefined) {
            formData.append('folderId', folderId);
        }

        const response = await api.post('/documents', formData);

        return response.data;
    },

    // Crear carpeta
    async createFolder(folderName) {
        const response = await api.post('/folders', {
            name: folderName
        });

        return response.data;
    },

    // Descargar y visualizar documento PDF
    async viewFile(documentId, filename) {
        const newWindow = window.open('about:blank', '_blank');

        try {
            const response = await api.get(`/documents/${documentId}/file`, {
                responseType: 'blob'
            });

            const blobUrl = window.URL.createObjectURL(
                new Blob([response.data], {
                    type: 'application/pdf'
                })
            );

            if (newWindow) {
                newWindow.location.href = blobUrl;
            } else {
                const link = document.createElement('a');
                link.href = blobUrl;
                link.download = filename || 'document.pdf';
                link.click();
            }

            setTimeout(() => {
                window.URL.revokeObjectURL(blobUrl);
            }, 60000);

            return response.data;
        } catch (error) {
            if (newWindow) newWindow.close();

            console.error('Error al visualizar el PDF:', error);
            throw error;
        }
    },

    // Mover documento a una carpeta o a General
    async moveFile(documentId, folderId = null) {
        const response = await api.patch(`/documents/${documentId}/folder`, {
            folderId
        });

        return response.data;
    },

    // Eliminar documento
    async deleteFile(documentId) {
        await api.delete(`/documents/${documentId}`);
    },

    // Eliminar carpeta
    async deleteFolder(folderId) {
        await api.delete(`/folders/${folderId}`);
    },

    // Renombrar carpeta
    async renameFolder(folderId, name) {
        const response = await api.patch(`/folders/${folderId}`, {
            name
        });

        return response.data;
    }
};
