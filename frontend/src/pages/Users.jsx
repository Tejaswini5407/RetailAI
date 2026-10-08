import React, { useState, useEffect } from 'react';
import { userApi } from '../api/userApi';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import Badge from '../components/common/Badge';
import Pagination from '../components/common/Pagination';

const Users = () => {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Form State
  const [showModal, setShowModal] = useState(false);
  const [editUserId, setEditUserId] = useState(null);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    role: 'STORE_MANAGER'
  });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  const fetchUsers = async (pageNo = page) => {
    setLoading(true);
    setError(null);
    try {
      const data = await userApi.getUsers({ page: pageNo, size: 10 });
      setUsers(data.content || []);
      setPage(data.number || 0);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load team users.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers(0);
  }, []);

  const handleOpenAddModal = () => {
    setEditUserId(null);
    setFormData({ email: '', password: '', role: 'STORE_MANAGER' });
    setFormError(null);
    setShowModal(true);
  };

  const handleOpenEditModal = (user) => {
    setEditUserId(user.id);
    setFormData({ email: user.email, password: '', role: user.role });
    setFormError(null);
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFormError(null);
    try {
      if (editUserId) {
        await userApi.updateUser(editUserId, {
          role: formData.role
        });
      } else {
        await userApi.createUser(formData);
      }
      setShowModal(false);
      fetchUsers(page);
    } catch (err) {
      setFormError(err.response?.data?.message || err.message || 'Failed to save user.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to remove this user from your organization?')) return;
    try {
      await userApi.deleteUser(id);
      fetchUsers(page);
    } catch (err) {
      alert(err.response?.data?.message || err.message || 'Failed to delete user.');
    }
  };

  return (
    <div className="users-page flex-column gap-4">
      <div className="flex-between flex-align-center">
        <div>
          <h1 className="text-2xl font-bold">User Management</h1>
          <p className="text-muted text-sm">Manage team members and assigned roles within your organization</p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAddModal}>
          + Create Team Member
        </button>
      </div>

      <ErrorMessage message={error} onRetry={() => fetchUsers(page)} />

      {loading ? (
        <LoadingSpinner text="Loading organization team members..." />
      ) : users.length === 0 ? (
        <div className="card p-4 text-center text-muted">
          No team members found in this organization.
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>Email Address</th>
                  <th>Assigned Role</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>#{u.id}</td>
                    <td><strong>{u.email}</strong></td>
                    <td>
                      <Badge value={u.role} />
                    </td>
                    <td className="text-right">
                      <button
                        className="btn btn-outline btn-sm me-2"
                        onClick={() => handleOpenEditModal(u)}
                        style={{ marginRight: '0.5rem' }}
                      >
                        Change Role
                      </button>
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => handleDelete(u.id)}
                      >
                        Delete User
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
            onPageChange={(p) => fetchUsers(p)}
          />
        </div>
      )}

      {/* Add / Edit User Modal */}
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
              {editUserId ? 'Update User Role' : 'Create Team Member'}
            </h2>

            <ErrorMessage message={formError} />

            <form onSubmit={handleSubmit}>
              {!editUserId && (
                <>
                  <div className="form-group">
                    <label className="form-label">Email Address</label>
                    <input
                      type="email"
                      className="form-control"
                      placeholder="colleague@organization.com"
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Temporary Password</label>
                    <input
                      type="password"
                      className="form-control"
                      placeholder="••••••••"
                      value={formData.password}
                      onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                      required
                    />
                  </div>
                </>
              )}

              <div className="form-group">
                <label className="form-label">Assigned Role</label>
                <select
                  className="form-control"
                  value={formData.role}
                  onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                  required
                >
                  <option value="STORE_MANAGER">STORE_MANAGER</option>
                  <option value="INVENTORY_MANAGER">INVENTORY_MANAGER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
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
                  {submitting ? 'Saving...' : editUserId ? 'Update Role' : 'Create User'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Users;
