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

export const productApi = {
  getProducts: async (pageOrParams = 0, sizeArg = 10) => {
    const { page, size } = normalizePageParams(pageOrParams, sizeArg);
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
