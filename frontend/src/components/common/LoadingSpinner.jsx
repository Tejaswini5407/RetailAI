import React from 'react';

const LoadingSpinner = ({ text = 'Loading data...' }) => {
  return (
    <div className="spinner-container flex-column flex-center p-4">
      <div className="spinner"></div>
      {text && <p className="text-muted mt-2">{text}</p>}
    </div>
  );
};

export default LoadingSpinner;
