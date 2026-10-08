import React from 'react';

const ErrorMessage = ({ message, onRetry }) => {
  if (!message) return null;

  return (
    <div className="alert alert-danger flex-between flex-align-center mb-3">
      <span>{message}</span>
      {onRetry && (
        <button className="btn btn-outline-danger btn-sm" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
};

export default ErrorMessage;
