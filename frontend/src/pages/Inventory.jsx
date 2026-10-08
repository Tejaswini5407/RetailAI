import React, { useState, useEffect } from 'react';
import { inventoryApi } from '../api/inventoryApi';
import { storeApi } from '../api/storeApi';
import { productApi } from '../api/productApi';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import Pagination from '../components/common/Pagination';

const Inventory = () => {
  const [inventory, setInventory] = useState([]);
  const [stores, setStores] = useState([]);
  const [products, setProducts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [filterStoreId, setFilterStoreId] = useState('');
  const [filterProductId, setFilterProductId] = useState('');

  // Form State
  const [showModal, setShowModal] = useState(false);
  const [editInventoryId, setEditInventoryId] = useState(null);
  const [formData, setFormData] = useState({
    storeId: '',
    productId: '',
    quantityOnHand: '',
    quantityOnOrder: ''
  });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  const fetchDropdownData = async () => {
    try {
      const [storesRes, productsRes] = await Promise.all([
        storeApi.getStores({ page: 0, size: 100 }),
        productApi.getProducts({ page: 0, size: 100 })
      ]);
      setStores(storesRes.content || []);
      setProducts(productsRes.content || []);
    } catch (err) {
      console.error('Failed to load dropdowns:', err);
    }
  };

  const fetchInventory = async (pageNo = page, sId = filterStoreId, pId = filterProductId) => {
    setLoading(true);
    setError(null);
    try {
      const data = await inventoryApi.getInventory({
        page: pageNo,
        size: 10,
        storeId: sId || undefined,
        productId: pId || undefined
      });
      setInventory(data.content || []);
      setPage(data.number || 0);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load inventory records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDropdownData();
    fetchInventory(0);
  }, []);

  const handleFilter = (e) => {
    e.preventDefault();
    fetchInventory(0, filterStoreId, filterProductId);
  };

  const handleResetFilter = () => {
    setFilterStoreId('');
    setFilterProductId('');
    fetchInventory(0, '', '');
  };

  const handleOpenAddModal = () => {
    setEditInventoryId(null);
    setFormData({
      storeId: stores.length > 0 ? stores[0].id : '',
      productId: products.length > 0 ? products[0].id : '',
      quantityOnHand: '100',
      quantityOnOrder: '0'
    });
    setFormError(null);
    setShowModal(true);
  };

  const handleOpenEditModal = (item) => {
    setEditInventoryId(item.id);
    setFormData({
      storeId: item.storeId,
      productId: item.productId,
      quantityOnHand: String(item.quantityOnHand || 0),
      quantityOnOrder: String(item.quantityOnOrder || 0)
    });
    setFormError(null);
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFormError(null);
    try {
      if (editInventoryId) {
        await inventoryApi.updateInventory(editInventoryId, {
          quantityOnHand: parseInt(formData.quantityOnHand, 10),
          quantityOnOrder: parseInt(formData.quantityOnOrder, 10)
        });
      } else {
        await inventoryApi.createInventory({
          storeId: parseInt(formData.storeId, 10),
          productId: parseInt(formData.productId, 10),
          quantityOnHand: parseInt(formData.quantityOnHand, 10),
          quantityOnOrder: parseInt(formData.quantityOnOrder, 10)
        });
      }
      setShowModal(false);
      fetchInventory(page);
    } catch (err) {
      setFormError(err.response?.data?.message || err.message || 'Failed to save inventory record.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="inventory-page flex-column gap-4">
      <div className="flex-between flex-align-center">
        <div>
          <h1 className="text-2xl font-bold">Inventory Management</h1>
          <p className="text-muted text-sm">Monitor stock levels & active replenishment orders</p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAddModal}>
          + Register Inventory Record
        </button>
      </div>

      <ErrorMessage message={error} onRetry={() => fetchInventory(page)} />

      {/* Filters */}
      <div className="card p-3">
        <form onSubmit={handleFilter} className="flex-row gap-3 flex-align-center">
          <div className="flex-1">
            <label className="form-label text-xs">Filter by Store</label>
            <select
              className="form-control"
              value={filterStoreId}
              onChange={(e) => setFilterStoreId(e.target.value)}
            >
              <option value="">All Stores</option>
              {stores.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.storeCode || s.code} - {s.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex-1">
            <label className="form-label text-xs">Filter by Product</label>
            <select
              className="form-control"
              value={filterProductId}
              onChange={(e) => setFilterProductId(e.target.value)}
            >
              <option value="">All Products</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.sku} - {p.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex-align-center gap-2 mt-3">
            <button type="submit" className="btn btn-primary btn-sm">
              Filter
            </button>
            <button type="button" className="btn btn-outline btn-sm" onClick={handleResetFilter}>
              Reset
            </button>
          </div>
        </form>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading inventory data..." />
      ) : inventory.length === 0 ? (
        <div className="card p-4 text-center text-muted">
          No inventory records found. Click "+ Register Inventory Record" to create one.
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Inventory ID</th>
                  <th>Store Code</th>
                  <th>Product SKU</th>
                  <th>Quantity On Hand</th>
                  <th>Quantity On Order</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {inventory.map((item) => (
                  <tr key={item.id}>
                    <td>#{item.id}</td>
                    <td><strong>{item.storeCode || `Store #${item.storeId}`}</strong></td>
                    <td>{item.sku || `Product #${item.productId}`}</td>
                    <td>
                      <span className="font-semibold">{item.quantityOnHand} units</span>
                    </td>
                    <td>
                      <span>{item.quantityOnOrder || 0} units</span>
                    </td>
                    <td className="text-right">
                      <button
                        className="btn btn-outline btn-sm"
                        onClick={() => handleOpenEditModal(item)}
                      >
                        Update Stock
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
            onPageChange={(p) => fetchInventory(p)}
          />
        </div>
      )}

      {/* Add / Edit Inventory Modal */}
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
              {editInventoryId ? 'Update Stock Levels' : 'Register Inventory Record'}
            </h2>

            <ErrorMessage message={formError} />

            <form onSubmit={handleSubmit}>
              {!editInventoryId && (
                <>
                  <div className="form-group">
                    <label className="form-label">Store Location</label>
                    <select
                      className="form-control"
                      value={formData.storeId}
                      onChange={(e) => setFormData({ ...formData, storeId: e.target.value })}
                      required
                    >
                      <option value="" disabled>Select Store</option>
                      {stores.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.storeCode || s.code} - {s.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Product SKU</label>
                    <select
                      className="form-control"
                      value={formData.productId}
                      onChange={(e) => setFormData({ ...formData, productId: e.target.value })}
                      required
                    >
                      <option value="" disabled>Select Product</option>
                      {products.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.sku} - {p.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </>
              )}

              <div className="form-group">
                <label className="form-label">Quantity On Hand</label>
                <input
                  type="number"
                  min="0"
                  className="form-control"
                  placeholder="100"
                  value={formData.quantityOnHand}
                  onChange={(e) => setFormData({ ...formData, quantityOnHand: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Quantity On Order</label>
                <input
                  type="number"
                  min="0"
                  className="form-control"
                  placeholder="0"
                  value={formData.quantityOnOrder}
                  onChange={(e) => setFormData({ ...formData, quantityOnOrder: e.target.value })}
                  required
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
                  {submitting ? 'Saving...' : editInventoryId ? 'Update Levels' : 'Create Record'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Inventory;
