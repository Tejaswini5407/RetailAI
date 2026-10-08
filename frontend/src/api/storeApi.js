import apiClient from './axios';

export const storeApi = {
  getStores: async (page = 0, size = 10) => {
    const response = await apiClient.get(`/api/v1/stores?page=${page}&size=${size}`);
    return response.data;
  },

  getStoreById: async (id) => {
    const response = await apiClient.get(`/api/v1/stores/${id}`);
    return response.data;
  },

  createStore: async (storeData) => {
    const response = await apiClient.post('/api/v1/stores', storeData);
    return response.data;
  },

  updateStore: async (id, storeData) => {
    const response = await apiClient.put(`/api/v1/stores/${id}`, storeData);
    return response.data;
  },

  deleteStore: async (id) => {
    const response = await apiClient.delete(`/api/v1/stores/${id}`);
    return response.data;
  }
};
