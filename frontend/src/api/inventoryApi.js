import apiClient from './axios';

export const inventoryApi = {
  getInventory: async (params = {}) => {
    let page = 0;
    let size = 10;
    let storeId, productId;

    if (typeof params === 'number' || typeof params === 'string') {
      page = params;
    } else if (typeof params === 'object' && params !== null) {
      page = params.page !== undefined ? params.page : 0;
      size = params.size !== undefined ? params.size : 10;
      storeId = params.storeId;
      productId = params.productId;
    }

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
