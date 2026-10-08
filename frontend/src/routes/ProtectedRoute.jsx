import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from '../components/common/LoadingSpinner';

const ProtectedRoute = ({ allowedRoles = null }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex-center screen-height">
        <LoadingSpinner text="Authenticating session..." />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return (
      <div className="container p-4">
        <div className="card alert-card error flex-column gap-2">
          <h2>Access Denied</h2>
          <p>
            Your current role (<strong>{user.role}</strong>) does not have sufficient permission to access this module.
          </p>
          <p>Required role(s): {allowedRoles.join(', ')}</p>
        </div>
      </div>
    );
  }

  return <Outlet />;
};

export default ProtectedRoute;
