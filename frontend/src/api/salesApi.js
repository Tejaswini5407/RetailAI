import apiClient from './axios';

export const salesApi = {
  getSales: async (params = {}) => {
    let page = 0;
    let size = 10;
    let storeId, productId, startDate, endDate;

    if (typeof params === 'number' || typeof params === 'string') {
      page = params;
    } else if (typeof params === 'object' && params !== null) {
      page = params.page !== undefined ? params.page : 0;
      size = params.size !== undefined ? params.size : 10;
      storeId = params.storeId;
      productId = params.productId;
      startDate = params.startDate;
      endDate = params.endDate;
    }

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
  },

  recordSale: async (saleData) => {
    const response = await apiClient.post('/api/v1/sales', saleData);
    return response.data;
  }
};
