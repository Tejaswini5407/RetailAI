import apiClient from './axios';

export const productApi = {
  getProducts: async (page = 0, size = 10) => {
    const response = await apiClient.get(`/api/v1/products?page=${page}&size=${size}`);
    return response.data;
  },

  getProductById: async (id) => {
    const response = await apiClient.get(`/api/v1/products/${id}`);
    return response.data;
  },

  createProduct: async (productData) => {
    const response = await apiClient.post('/api/v1/products', productData);
    return response.data;
  },

  updateProduct: async (id, productData) => {
    const response = await apiClient.put(`/api/v1/products/${id}`, productData);
    return response.data;
  },

  deleteProduct: async (id) => {
    const response = await apiClient.delete(`/api/v1/products/${id}`);
    return response.data;
  }
};
