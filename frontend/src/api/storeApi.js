import apiClient from './axios';

const normalizePageParams = (pageOrParams = 0, sizeArg = 10) => {
  let page = 0;
  let size = 10;
  if (typeof pageOrParams === 'object' && pageOrParams !== null) {
    page = pageOrParams.page !== undefined ? pageOrParams.page : 0;
    size = pageOrParams.size !== undefined ? pageOrParams.size : 10;
  } else if (typeof pageOrParams === 'number' || typeof pageOrParams === 'string') {
    page = pageOrParams;
    size = sizeArg;
  }
  return { page, size };
};

export const storeApi = {
  getStores: async (pageOrParams = 0, sizeArg = 10) => {
    const { page, size } = normalizePageParams(pageOrParams, sizeArg);
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
