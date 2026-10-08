import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('retailai_token') || null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('retailai_token');
      if (storedToken) {
        try {
          const meData = await authApi.getCurrentUser();
          setUser(meData);
          setToken(storedToken);
        } catch (err) {
          console.error('Failed to validate session token:', err);
          localStorage.removeItem('retailai_token');
          localStorage.removeItem('retailai_user');
          setToken(null);
          setUser(null);
        }
      }
      setLoading(false);
    };

    initAuth();

    const handleAuthExpired = () => {
      setUser(null);
      setToken(null);
    };

    window.addEventListener('retailai_auth_expired', handleAuthExpired);
    return () => window.removeEventListener('retailai_auth_expired', handleAuthExpired);
  }, []);

  const login = async (credentials) => {
    setError(null);
    try {
      const data = await authApi.login(credentials);
      localStorage.setItem('retailai_token', data.token);
      setToken(data.token);

      // Fetch user profile from /me endpoint
      const meData = await authApi.getCurrentUser();
      setUser(meData);
      return meData;
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Login failed. Please check credentials.';
      setError(msg);
      throw new Error(msg);
    }
  };

  const register = async (registrationData) => {
    setError(null);
    try {
      const data = await authApi.register(registrationData);
      localStorage.setItem('retailai_token', data.token);
      setToken(data.token);

      const meData = await authApi.getCurrentUser();
      setUser(meData);
      return meData;
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Registration failed.';
      setError(msg);
      throw new Error(msg);
    }
  };

  const logout = () => {
    localStorage.removeItem('retailai_token');
    localStorage.removeItem('retailai_user');
    setToken(null);
    setUser(null);
    setError(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, error, login, register, logout, setError }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
