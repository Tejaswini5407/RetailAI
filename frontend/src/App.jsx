import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './routes/ProtectedRoute';

import Layout from './components/layout/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Stores from './pages/Stores';
import Products from './pages/Products';
import Sales from './pages/Sales';
import Inventory from './pages/Inventory';
import Assessment from './pages/Assessment';
import Forecasts from './pages/Forecasts';
import Users from './pages/Users';
import Profile from './pages/Profile';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public Authentication Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected Application Routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<Dashboard />} />
              <Route path="/stores" element={<Stores />} />
              <Route path="/products" element={<Products />} />
              <Route path="/sales" element={<Sales />} />
              <Route path="/inventory" element={<Inventory />} />
              <Route path="/assessment" element={<Assessment />} />
              <Route path="/forecasts" element={<Forecasts />} />
              <Route path="/profile" element={<Profile />} />

              {/* Admin-only Route */}
              <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
                <Route path="/users" element={<Users />} />
              </Route>
            </Route>
          </Route>

          {/* Fallback Route */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
