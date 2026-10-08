import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { storeApi } from '../api/storeApi';
import { productApi } from '../api/productApi';
import { salesApi } from '../api/salesApi';
import { inventoryApi } from '../api/inventoryApi';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import Badge from '../components/common/Badge';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  PieChart,
  Pie,
  Cell
} from 'recharts';

const Dashboard = () => {
  const [metrics, setMetrics] = useState({
    totalStores: 0,
    totalProducts: 0,
    totalSales: 0,
    criticalCount: 0,
    reorderCount: 0,
    watchCount: 0,
    healthyCount: 0,
  });
  const [criticalInventory, setCriticalInventory] = useState([]);
  const [statusChartData, setStatusChartData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [storesRes, productsRes, salesRes, inventoryRes] = await Promise.all([
        storeApi.getStores({ page: 0, size: 100 }),
        productApi.getProducts({ page: 0, size: 100 }),
        salesApi.getSales({ page: 0, size: 100 }),
        inventoryApi.getInventory({ page: 0, size: 100 })
      ]);

      const stores = storesRes.content || [];
      const products = productsRes.content || [];
      const sales = salesRes.content || [];
      const inventory = inventoryRes.content || [];

      let critical = 0;
      let reorder = 0;
      let watch = 0;
      let healthy = 0;

      const alertList = [];

      inventory.forEach((item) => {
        const status = (item.status || 'HEALTHY').toUpperCase();
        if (status === 'CRITICAL') critical++;
        else if (status === 'REORDER') reorder++;
        else if (status === 'WATCH') watch++;
        else healthy++;

        if (status === 'CRITICAL' || status === 'REORDER') {
          alertList.push(item);
        }
      });

      setMetrics({
        totalStores: storesRes.totalElements || stores.length,
        totalProducts: productsRes.totalElements || products.length,
        totalSales: salesRes.totalElements || sales.length,
        criticalCount: critical,
        reorderCount: reorder,
        watchCount: watch,
        healthyCount: healthy
      });

      setCriticalInventory(alertList.slice(0, 5));

      setStatusChartData([
        { name: 'Critical', count: critical, color: '#ef4444' },
        { name: 'Reorder', count: reorder, color: '#f59e0b' },
        { name: 'Watch', count: watch, color: '#06b6d4' },
        { name: 'Healthy', count: healthy, color: '#10b981' }
      ]);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load dashboard metrics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  if (loading) return <LoadingSpinner text="Loading executive dashboard..." />;

  return (
    <div className="dashboard-page flex-column gap-4">
      <div className="flex-between flex-align-center">
        <div>
          <h1 className="text-2xl font-bold">Executive Dashboard</h1>
          <p className="text-muted text-sm">Real-time store performance, inventory health & AI demand insights</p>
        </div>
        <button className="btn btn-outline btn-sm" onClick={fetchDashboardData}>
          🔄 Refresh Metrics
        </button>
      </div>

      <ErrorMessage message={error} onRetry={fetchDashboardData} />

      {/* Metric Cards */}
      <div className="grid-4">
        <div className="card">
          <div className="text-sm font-semibold text-muted">Active Stores</div>
          <div className="text-2xl font-bold text-dark mt-1">{metrics.totalStores}</div>
          <div className="text-xs text-muted mt-2">Configured store locations</div>
        </div>

        <div className="card">
          <div className="text-sm font-semibold text-muted">Product Catalog</div>
          <div className="text-2xl font-bold text-dark mt-1">{metrics.totalProducts}</div>
          <div className="text-xs text-muted mt-2">Active SKUs in organization</div>
        </div>

        <div className="card">
          <div className="text-sm font-semibold text-muted">Recorded Sales</div>
          <div className="text-2xl font-bold text-dark mt-1">{metrics.totalSales}</div>
          <div className="text-xs text-muted mt-2">Historical sales transactions</div>
        </div>

        <div className="card">
          <div className="text-sm font-semibold text-muted">Low Stock Alerts</div>
          <div className="text-2xl font-bold text-danger mt-1">
            {metrics.criticalCount + metrics.reorderCount}
          </div>
          <div className="text-xs text-muted mt-2">
            {metrics.criticalCount} Critical | {metrics.reorderCount} Reorder
          </div>
        </div>
      </div>

      {/* Visual Charts & Breakdown */}
      <div className="grid-2">
        {/* Inventory Status Distribution Chart */}
        <div className="card">
          <div className="card-header">
            <h3 className="font-semibold text-dark">Inventory Health Breakdown</h3>
          </div>
          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer>
              <BarChart data={statusChartData} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="count" fill="#2563eb" radius={[4, 4, 0, 0]}>
                  {statusChartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Quick Intelligence Actions */}
        <div className="card flex-column justify-between">
          <div>
            <div className="card-header">
              <h3 className="font-semibold text-dark">Inventory Intelligence Actions</h3>
            </div>
            <p className="text-sm text-muted mb-4">
              Access real-time forecasting engines and automated order recommendation APIs.
            </p>
            <div className="flex-column gap-3">
              <Link to="/assessment" className="btn btn-primary py-2 text-decoration-none">
                ⚡ Inventory Assessment & Reorder Engine
              </Link>
              <Link to="/forecasts" className="btn btn-outline py-2 text-decoration-none">
                🔮 Generate Demand Forecast
              </Link>
              <Link to="/sales" className="btn btn-outline py-2 text-decoration-none">
                📈 Record New Sales Data
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Low Stock Attention Table */}
      <div className="card">
        <div className="card-header flex-between flex-align-center">
          <h3 className="font-semibold text-dark">Attention Required: Low Stock Items</h3>
          <Link to="/inventory" className="text-sm text-primary font-semibold text-decoration-none">
            View All Inventory &raquo;
          </Link>
        </div>

        {criticalInventory.length === 0 ? (
          <div className="p-3 text-center text-muted">
            All inventory levels are healthy. No immediate reorders required.
          </div>
        ) : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Store Code</th>
                  <th>Product SKU</th>
                  <th>On Hand</th>
                  <th>Reorder Point</th>
                  <th>Safety Stock</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {criticalInventory.map((item) => (
                  <tr key={item.id}>
                    <td><strong>{item.storeCode || `Store #${item.storeId}`}</strong></td>
                    <td>{item.productSku || `Product #${item.productId}`}</td>
                    <td>{item.quantityOnHand}</td>
                    <td>{item.reorderPoint}</td>
                    <td>{item.safetyStock}</td>
                    <td>
                      <Badge value={item.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;
