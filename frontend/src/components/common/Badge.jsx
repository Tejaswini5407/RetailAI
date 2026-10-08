import React from 'react';

const Badge = ({ type = 'status', value }) => {
  if (!value) return null;

  const valUpper = String(value).toUpperCase();

  const getBadgeClass = () => {
    switch (valUpper) {
      // Inventory Statuses (Phase 5 parity)
      case 'CRITICAL':
        return 'badge-danger';
      case 'REORDER':
        return 'badge-warning';
      case 'WATCH':
        return 'badge-info';
      case 'HEALTHY':
        return 'badge-success';

      // Risk Levels
      case 'HIGH':
        return 'badge-danger';
      case 'MEDIUM':
        return 'badge-warning';
      case 'LOW':
        return 'badge-success';

      // User Roles
      case 'ADMIN':
        return 'badge-primary';
      case 'STORE_MANAGER':
        return 'badge-secondary';
      case 'INVENTORY_MANAGER':
        return 'badge-info';

      default:
        return 'badge-default';
    }
  };

  return <span className={`badge ${getBadgeClass()}`}>{valUpper}</span>;
};

export default Badge;
