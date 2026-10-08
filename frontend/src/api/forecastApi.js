import apiClient from './axios';

export const forecastApi = {
  generateForecast: async (forecastData) => {
    const response = await apiClient.post('/api/v1/forecasts', forecastData);
    return response.data;
  },
};
