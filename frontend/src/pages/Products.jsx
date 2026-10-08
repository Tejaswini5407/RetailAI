import React, { useState, useEffect } from 'react';
import { productApi } from '../api/productApi';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import Pagination from '../components/common/Pagination';

const Products = () => {
  const [products, setProducts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Form State
  const [showModal, setShowModal] = useState(false);
  const [editProductId, setEditProductId] = useState(null);
  const [formData, setFormData] = useState({ sku: '', name: '', category: '', department: '' });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  const fetchProducts = async (pageNo = page) => {
    setLoading(true);
    setError(null);
    try {
      const data = await productApi.getProducts({ page: pageNo, size: 10 });
      setProducts(data.content || []);
      setPage(data.number || 0);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load product catalog.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts(0);
  }, []);

  const handleOpenAddModal = () => {
    setEditProductId(null);
    setFormData({ sku: '', name: '', category: '', department: '' });
    setFormError(null);
    setShowModal(true);
  };

  const handleOpenEditModal = (product) => {
    setEditProductId(product.id);
    setFormData({
      sku: product.sku || '',
      name: product.name || '',
      category: product.category || '',
      department: product.department || ''
    });
    setFormError(null);
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFormError(null);
    try {
      if (editProductId) {
        await productApi.updateProduct(editProductId, formData);
      } else {
        await productApi.createProduct(formData);
      }
      setShowModal(false);
      fetchProducts(page);
    } catch (err) {
      setFormError(err.response?.data?.message || err.message || 'Failed to save product.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this product?')) return;
    try {
      await productApi.deleteProduct(id);
      fetchProducts(page);
    } catch (err) {
      alert(err.response?.data?.message || err.message || 'Failed to delete product.');
    }
  };

  return (
    <div className="products-page flex-column gap-4">
      <div className="flex-between flex-align-center">
        <div>
          <h1 className="text-2xl font-bold">Product Catalog</h1>
          <p className="text-muted text-sm">Manage product SKUs, categories, and department assignments</p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAddModal}>
          + Add New Product
        </button>
      </div>

      <ErrorMessage message={error} onRetry={() => fetchProducts(page)} />

      {loading ? (
        <LoadingSpinner text="Loading products..." />
      ) : products.length === 0 ? (
        <div className="card p-4 text-center text-muted">
          No products found. Click "+ Add New Product" to create your first SKU.
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>SKU</th>
                  <th>Product Name</th>
                  <th>Category</th>
                  <th>Department</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {products.map((product) => (
                  <tr key={product.id}>
                    <td>#{product.id}</td>
                    <td><strong>{product.sku}</strong></td>
                    <td>{product.name}</td>
                    <td><span className="badge badge-secondary">{product.category || 'General'}</span></td>
                    <td><span className="badge badge-info">{product.department || 'Retail'}</span></td>
                    <td className="text-right">
                      <button
                        className="btn btn-outline btn-sm me-2"
                        onClick={() => handleOpenEditModal(product)}
                        style={{ marginRight: '0.5rem' }}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => handleDelete(product.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={(p) => fetchProducts(p)}
          />
        </div>
      )}

      {/* Add / Edit Product Modal */}
      {showModal && (
        <div
          className="modal-overlay flex-center"
          style={{
            position: 'fixed',
            top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            zIndex: 1000
          }}
        >
          <div className="card p-4" style={{ width: '100%', maxWidth: '500px' }}>
            <h2 className="text-xl font-bold mb-3">
              {editProductId ? 'Edit Product' : 'Add New Product'}
            </h2>

            <ErrorMessage message={formError} />

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label">SKU (Product Code)</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="e.g. SKU-1001"
                  value={formData.sku}
                  onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Product Name</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Organic Milk 1L"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>

              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Category</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Dairy"
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Department</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Grocery"
                    value={formData.department}
                    onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="flex-between mt-4">
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setShowModal(false)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Saving...' : editProductId ? 'Update Product' : 'Create Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Products;
