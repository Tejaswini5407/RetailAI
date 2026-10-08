import apiClient from './axios';

export const salesApi = {
  getSales: async (params = {}) => {
    const { page = 0, size = 10, storeId, productId, startDate, endDate } = params;
    let query = `/api/v1/sales?page=${page}&size=${size}`;
    if (storeId) query += `&storeId=${storeId}`;
    if (productId) query += `&productId=${productId}`;
    if (startDate) query += `&startDate=${startDate}`;
    if (endDate) query += `&endDate=${endDate}`;
    
    const response = await apiClient.get(query);
    return response.data;
  },

  getSaleById: async (id) => {
    const response = await apiClient.get(`/api/v1/sales/${id}`);
    return response.data;
  },

  createSale: async (saleData) => {
    const response = await apiClient.post('/api/v1/sales', saleData);
    return response.data;
  }
};
