import React from 'react';

const Pagination = ({ page = 0, totalPages = 1, totalElements = 0, onPageChange }) => {
  if (totalPages <= 1 && totalElements === 0) return null;

  return (
    <div className="pagination-container flex-between flex-align-center mt-3 pt-3 border-top">
      <div className="text-muted text-sm">
        Showing page {page + 1} of {Math.max(1, totalPages)} ({totalElements} total items)
      </div>
      <div className="flex-gap-2">
        <button
          className="btn btn-outline btn-sm"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          &laquo; Previous
        </button>
        <button
          className="btn btn-outline btn-sm"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
        >
          Next &raquo;
        </button>
      </div>
    </div>
  );
};

export default Pagination;
