import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const Sidebar = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const navItems = [
    { label: 'Dashboard', path: '/', icon: '📊' },
    { label: 'Data Import', path: '/import', icon: '📥' },
    { label: 'Stores', path: '/stores', icon: '🏪' },
    { label: 'Products', path: '/products', icon: '📦' },
    { label: 'Sales Records', path: '/sales', icon: '📈' },
    { label: 'Inventory', path: '/inventory', icon: '📋' },
    { label: 'Inventory Assessment', path: '/assessment', icon: '⚡' },
    { label: 'Demand Forecasts', path: '/forecasts', icon: '🔮' },
  ];

  if (isAdmin) {
    navItems.push({ label: 'User Management', path: '/users', icon: '👥' });
  }

  return (
    <aside className="app-sidebar p-3 bg-light border-right">
      <div className="sidebar-section-title text-xs text-muted text-uppercase mb-2 font-bold px-2">
        Navigation
      </div>
      <nav className="flex-column gap-1">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            className={({ isActive }) =>
              `nav-item px-3 py-2 rounded text-decoration-none flex-align-center gap-2 ${
                isActive ? 'nav-item-active font-semibold' : 'text-dark nav-item-hover'
              }`
            }
          >
            <span className="nav-icon">{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
};

export default Sidebar;
