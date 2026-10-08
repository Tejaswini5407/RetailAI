import React, { useState, useEffect } from 'react';
import { storeApi } from '../api/storeApi';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import Pagination from '../components/common/Pagination';

const Stores = () => {
  const [stores, setStores] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Form State
  const [showModal, setShowModal] = useState(false);
  const [editStoreId, setEditStoreId] = useState(null);
  const [formData, setFormData] = useState({ code: '', name: '', location: '' });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  const fetchStores = async (pageNo = page) => {
    setLoading(true);
    setError(null);
    try {
      const data = await storeApi.getStores({ page: pageNo, size: 10 });
      setStores(data.content || []);
      setPage(data.number || 0);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load store locations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStores(0);
  }, []);

  const handleOpenAddModal = () => {
    setEditStoreId(null);
    setFormData({ code: '', name: '', location: '' });
    setFormError(null);
    setShowModal(true);
  };

  const handleOpenEditModal = (store) => {
    setEditStoreId(store.id);
    setFormData({ code: store.code, name: store.name, location: store.location || '' });
    setFormError(null);
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFormError(null);
    try {
      if (editStoreId) {
        await storeApi.updateStore(editStoreId, formData);
      } else {
        await storeApi.createStore(formData);
      }
      setShowModal(false);
      fetchStores(page);
    } catch (err) {
      setFormError(err.response?.data?.message || err.message || 'Failed to save store.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this store?')) return;
    try {
      await storeApi.deleteStore(id);
      fetchStores(page);
    } catch (err) {
      alert(err.response?.data?.message || err.message || 'Failed to delete store.');
    }
  };

  return (
    <div className="stores-page flex-column gap-4">
      <div className="flex-between flex-align-center">
        <div>
          <h1 className="text-2xl font-bold">Store Locations</h1>
          <p className="text-muted text-sm">Manage physical stores for your organization</p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAddModal}>
          + Add New Store
        </button>
      </div>

      <ErrorMessage message={error} onRetry={() => fetchStores(page)} />

      {loading ? (
        <LoadingSpinner text="Loading stores..." />
      ) : stores.length === 0 ? (
        <div className="card p-4 text-center text-muted">
          No store locations found. Click "+ Add New Store" to register your first store.
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Store Code</th>
                  <th>Store Name</th>
                  <th>Location</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {stores.map((store) => (
                  <tr key={store.id}>
                    <td>{store.id}</td>
                    <td><strong>{store.code}</strong></td>
                    <td>{store.name}</td>
                    <td>{store.location || 'N/A'}</td>
                    <td className="text-right">
                      <button
                        className="btn btn-outline btn-sm me-2"
                        onClick={() => handleOpenEditModal(store)}
                        style={{ marginRight: '0.5rem' }}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => handleDelete(store.id)}
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
            onPageChange={(p) => fetchStores(p)}
          />
        </div>
      )}

      {/* Add / Edit Store Modal */}
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
              {editStoreId ? 'Edit Store Location' : 'Add New Store Location'}
            </h2>

            <ErrorMessage message={formError} />

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label">Store Code</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="e.g. STORE-001"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Store Name</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Downtown Supercenter"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Location / Address</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="New York, NY"
                  value={formData.location}
                  onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                />
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
                  {submitting ? 'Saving...' : editStoreId ? 'Update Store' : 'Create Store'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Stores;
