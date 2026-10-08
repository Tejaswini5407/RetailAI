import apiClient from './axios';

export const forecastApi = {
  generateForecast: async (forecastData) => {
    const response = await apiClient.post('/api/v1/forecasts', forecastData);
    return response.data;
  },

  getForecastHistory: async () => {
    const response = await apiClient.get('/api/v1/forecasts/history');
    return response.data;
  },

  getForecastAccuracy: async () => {
    const response = await apiClient.get('/api/v1/forecasts/accuracy');
    return response.data;
  },

  getForecastExplanation: async (storeId, productId) => {
    const response = await apiClient.get(`/api/v1/forecasts/explanation?storeId=${storeId}&productId=${productId}`);
    return response.data;
  }
};
