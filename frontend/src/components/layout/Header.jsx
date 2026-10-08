import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import Badge from '../common/Badge';

const Header = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="app-header flex-between flex-align-center px-4 py-2 bg-dark text-white">
      <div className="flex-align-center gap-3">
        <Link to="/" className="brand-logo text-white text-decoration-none font-bold text-xl">
          Retail<span className="text-primary">AI</span>
        </Link>
        <span className="badge badge-outline-light text-xs">
          Tenant: {user?.organizationCode || 'N/A'}
        </span>
      </div>

      <div className="flex-align-center gap-3">
        <div className="user-profile-summary text-right">
          <div className="text-sm font-semibold">{user?.username}</div>
          <Badge value={user?.role} />
        </div>
        <Link to="/profile" className="btn btn-sm btn-outline-light">
          Profile
        </Link>
        <button onClick={handleLogout} className="btn btn-sm btn-danger">
          Logout
        </button>
      </div>
    </header>
  );
};

export default Header;
