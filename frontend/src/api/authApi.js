import apiClient from './axios';

export const authApi = {
  login: async (credentials) => {
    const response = await apiClient.post('/api/v1/auth/login', credentials);
    return response.data;
  },

  register: async (registrationData) => {
    const response = await apiClient.post('/api/v1/auth/register', registrationData);
    return response.data;
  },

  getCurrentUser: async () => {
    const response = await apiClient.get('/api/v1/me');
    return response.data;
  }
};
