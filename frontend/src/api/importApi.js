import apiClient from './axios';

export const importApi = {
  previewImport: async (formData, type = 'PRODUCTS') => {
    const response = await apiClient.post(`/api/v1/imports/preview?type=${type}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  confirmImport: async (confirmData) => {
    const response = await apiClient.post('/api/v1/imports/confirm', confirmData);
    return response.data;
  },

  getImportHistory: async () => {
    const response = await apiClient.get('/api/v1/imports/history');
    return response.data;
  },

  downloadTemplate: async (type) => {
    const response = await apiClient.get(`/api/v1/imports/templates/${type}`, {
      responseType: 'blob',
    });
    return response;
  },

  downloadTemplateUrl: (type) => {
    const baseURL = apiClient.defaults.baseURL || 'http://localhost:8080';
    return `${baseURL}/api/v1/imports/templates/${type}`;
  }
};
