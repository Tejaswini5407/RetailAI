import apiClient from './axios';

export const inventoryApi = {
  getInventory: async (params = {}) => {
    const { page = 0, size = 10, storeId, productId } = params;
    let query = `/api/v1/inventory?page=${page}&size=${size}`;
    if (storeId) query += `&storeId=${storeId}`;
    if (productId) query += `&productId=${productId}`;

    const response = await apiClient.get(query);
    return response.data;
  },

  getInventoryById: async (id) => {
    const response = await apiClient.get(`/api/v1/inventory/${id}`);
    return response.data;
  },

  createInventory: async (inventoryData) => {
    const response = await apiClient.post('/api/v1/inventory', inventoryData);
    return response.data;
  },

  updateInventory: async (id, inventoryData) => {
    const response = await apiClient.put(`/api/v1/inventory/${id}`, inventoryData);
    return response.data;
  },

  assessInventory: async (assessmentData) => {
    const response = await apiClient.post('/api/v1/inventory/assess', assessmentData);
    return response.data;
  },

  assessForecastInventory: async (combinedData) => {
    const response = await apiClient.post('/api/v1/inventory/forecast-assessment', combinedData);
    return response.data;
  }
};
