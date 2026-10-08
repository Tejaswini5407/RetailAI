import React, { useState, useEffect } from 'react';
import { salesApi } from '../api/salesApi';
import { storeApi } from '../api/storeApi';
import { productApi } from '../api/productApi';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import Pagination from '../components/common/Pagination';

const Sales = () => {
  const [sales, setSales] = useState([]);
  const [stores, setStores] = useState([]);
  const [products, setProducts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filter state
  const [filterStoreId, setFilterStoreId] = useState('');
  const [filterProductId, setFilterProductId] = useState('');

  // Form state
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    storeId: '',
    productId: '',
    unitsSold: '',
    unitPrice: '',
    saleDate: new Date().toISOString().split('T')[0]
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
      console.error('Failed to load stores/products dropdown:', err);
    }
  };

  const fetchSales = async (pageNo = page, sId = filterStoreId, pId = filterProductId) => {
    setLoading(true);
    setError(null);
    try {
      const data = await salesApi.getSales({
        page: pageNo,
        size: 10,
        storeId: sId || undefined,
        productId: pId || undefined
      });
      setSales(data.content || []);
      setPage(data.number || 0);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load sales records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDropdownData();
    fetchSales(0);
  }, []);

  const handleFilter = (e) => {
    e.preventDefault();
    fetchSales(0, filterStoreId, filterProductId);
  };

  const handleResetFilter = () => {
    setFilterStoreId('');
    setFilterProductId('');
    fetchSales(0, '', '');
  };

  const handleOpenAddModal = () => {
    setFormData({
      storeId: stores.length > 0 ? stores[0].id : '',
      productId: products.length > 0 ? products[0].id : '',
      unitsSold: '',
      unitPrice: '',
      saleDate: new Date().toISOString().split('T')[0]
    });
    setFormError(null);
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFormError(null);
    try {
      const payload = {
        storeId: parseInt(formData.storeId, 10),
        productId: parseInt(formData.productId, 10),
        unitsSold: parseInt(formData.unitsSold, 10),
        unitPrice: formData.unitPrice ? parseFloat(formData.unitPrice) : null,
        saleDate: formData.saleDate
      };
      await salesApi.recordSale(payload);
      setShowModal(false);
      fetchSales(page);
    } catch (err) {
      setFormError(err.response?.data?.message || err.message || 'Failed to record sale transaction.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="sales-page flex-column gap-4">
      <div className="flex-between flex-align-center">
        <div>
          <h1 className="text-2xl font-bold">Sales Transactions</h1>
          <p className="text-muted text-sm">Record & view daily point-of-sale store transactions</p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenAddModal}>
          + Record Sale
        </button>
      </div>

      <ErrorMessage message={error} onRetry={() => fetchSales(page)} />

      {/* Filter Card */}
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
        <LoadingSpinner text="Loading sales records..." />
      ) : sales.length === 0 ? (
        <div className="card p-4 text-center text-muted">
          No sales transactions recorded matching criteria. Click "+ Record Sale" to create a record.
        </div>
      ) : (
        <div className="card">
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Transaction ID</th>
                  <th>Store Code</th>
                  <th>Product SKU</th>
                  <th>Units Sold</th>
                  <th>Unit Price</th>
                  <th>Sale Date</th>
                </tr>
              </thead>
              <tbody>
                {sales.map((sale) => (
                  <tr key={sale.id}>
                    <td>#{sale.id}</td>
                    <td><strong>{sale.storeCode || `Store #${sale.storeId}`}</strong></td>
                    <td>{sale.sku || `Product #${sale.productId}`}</td>
                    <td><span className="font-semibold text-primary">{sale.unitsSold ?? sale.quantitySold} units</span></td>
                    <td>{sale.unitPrice ? `$${parseFloat(sale.unitPrice).toFixed(2)}` : 'N/A'}</td>
                    <td>{sale.saleDate}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={(p) => fetchSales(p)}
          />
        </div>
      )}

      {/* Record Sale Modal */}
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
            <h2 className="text-xl font-bold mb-3">Record Sales Transaction</h2>

            <ErrorMessage message={formError} />

            <form onSubmit={handleSubmit}>
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

              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Units Sold</label>
                  <input
                    type="number"
                    min="1"
                    className="form-control"
                    placeholder="e.g. 15"
                    value={formData.unitsSold}
                    onChange={(e) => setFormData({ ...formData, unitsSold: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Unit Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    className="form-control"
                    placeholder="e.g. 4.50"
                    value={formData.unitPrice}
                    onChange={(e) => setFormData({ ...formData, unitPrice: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Sale Date</label>
                <input
                  type="date"
                  className="form-control"
                  value={formData.saleDate}
                  onChange={(e) => setFormData({ ...formData, saleDate: e.target.value })}
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
                  {submitting ? 'Recording...' : 'Record Transaction'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Sales;
