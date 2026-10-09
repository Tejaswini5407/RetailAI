import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('retailai_token') || null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [errorDetails, setErrorDetails] = useState(null);

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
    setErrorDetails(null);
    try {
      const data = await authApi.login(credentials);
      const jwtToken = data.accessToken || data.token;
      localStorage.setItem('retailai_token', jwtToken);
      setToken(jwtToken);

      // Fetch user profile from /me endpoint
      const meData = await authApi.getCurrentUser();
      setUser(meData);
      return meData;
    } catch (err) {
      const responseData = err.response?.data;
      const errorCode = responseData?.code;
      const httpStatus = err.response?.status;
      const msg = responseData?.message || err.message || 'Login failed. Please check credentials.';
      const isOrgNotFound = errorCode === 'ORGANIZATION_NOT_FOUND' || httpStatus === 404;

      const details = {
        code: errorCode,
        status: httpStatus,
        message: msg,
        isOrgNotFound
      };

      setError(msg);
      setErrorDetails(details);
      throw err;
    }
  };

  const register = async (registrationData) => {
    setError(null);
    setErrorDetails(null);
    try {
      const data = await authApi.register(registrationData);
      const jwtToken = data.accessToken || data.token;
      localStorage.setItem('retailai_token', jwtToken);
      setToken(jwtToken);

      const meData = await authApi.getCurrentUser();
      setUser(meData);
      return meData;
    } catch (err) {
      const responseData = err.response?.data;
      const msg = responseData?.message || err.message || 'Registration failed.';
      setError(msg);
      setErrorDetails({ code: responseData?.code, status: err.response?.status, message: msg });
      throw err;
    }
  };

  const logout = () => {
    localStorage.removeItem('retailai_token');
    localStorage.removeItem('retailai_user');
    setToken(null);
    setUser(null);
    setError(null);
    setErrorDetails(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, error, errorDetails, login, register, logout, setError, setErrorDetails }}>
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
