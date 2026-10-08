import apiClient from './axios';

export const userApi = {
  getUsers: async (params = {}) => {
    const { page = 0, size = 10 } = params;
    const response = await apiClient.get(`/api/v1/users?page=${page}&size=${size}`);
    return response.data;
  },

  getUserById: async (id) => {
    const response = await apiClient.get(`/api/v1/users/${id}`);
    return response.data;
  },

  createUser: async (userData) => {
    const response = await apiClient.post('/api/v1/users', userData);
    return response.data;
  },

  updateUser: async (id, userData) => {
    const response = await apiClient.put(`/api/v1/users/${id}`, userData);
    return response.data;
  },

  deleteUser: async (id) => {
    const response = await apiClient.delete(`/api/v1/users/${id}`);
    return response.data;
  }
};
