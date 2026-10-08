import React, { useState, useEffect } from 'react';
import { forecastApi } from '../api/forecastApi';
import { storeApi } from '../api/storeApi';
import { productApi } from '../api/productApi';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid
} from 'recharts';

const Forecasts = () => {
  const [stores, setStores] = useState([]);
  const [products, setProducts] = useState([]);
  const [storeId, setStoreId] = useState('');
  const [productId, setProductId] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [forecastResult, setForecastResult] = useState(null);

  useEffect(() => {
    const loadDropdowns = async () => {
      try {
        const [storesRes, productsRes] = await Promise.all([
          storeApi.getStores({ page: 0, size: 100 }),
          productApi.getProducts({ page: 0, size: 100 })
        ]);
        const sList = storesRes.content || [];
        const pList = productsRes.content || [];
        setStores(sList);
        setProducts(pList);
        if (sList.length > 0) setStoreId(sList[0].id);
        if (pList.length > 0) setProductId(pList[0].id);
      } catch (err) {
        console.error('Failed to load stores/products:', err);
      }
    };
    loadDropdowns();
  }, []);

  const handleGenerateForecast = async (e) => {
    e.preventDefault();
    if (!storeId || !productId || !date) {
      setError('Please select a store, product, and target date.');
      return;
    }

    setLoading(true);
    setError(null);
    setForecastResult(null);

    try {
      const payload = {
        storeId: parseInt(storeId, 10),
        productId: parseInt(productId, 10),
        date: date
      };
      const res = await forecastApi.generateForecast(payload);
      setForecastResult(res);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to generate demand forecast.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="forecasts-page flex-column gap-4">
      <div>
        <h1 className="text-2xl font-bold">Demand Forecasting Engine</h1>
        <p className="text-muted text-sm">
          Generate machine-learning-driven sales demand forecasts via the Python ML microservice.
        </p>
      </div>

      <ErrorMessage message={error} />

      <div className="grid-2">
        {/* Forecast Configuration Form */}
        <div className="card">
          <div className="card-header">
            <h3 className="font-semibold text-dark">Forecast Parameters</h3>
          </div>

          <form onSubmit={handleGenerateForecast}>
            <div className="form-group">
              <label className="form-label">Store Location</label>
              <select
                className="form-control"
                value={storeId}
                onChange={(e) => setStoreId(e.target.value)}
                required
              >
                {stores.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.code} - {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Product SKU</label>
              <select
                className="form-control"
                value={productId}
                onChange={(e) => setProductId(e.target.value)}
                required
              >
                {products.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.sku} - {p.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Target Date</label>
              <input
                type="date"
                className="form-control"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary w-100 mt-4 py-2 font-semibold"
              style={{ width: '100%' }}
              disabled={loading}
            >
              {loading ? 'Running ML Model Inference...' : '🔮 Generate ML Forecast'}
            </button>
          </form>
        </div>

        {/* Forecast Results & Chart */}
        <div className="card flex-column justify-between">
          <div className="card-header">
            <h3 className="font-semibold text-dark">Predicted Demand Output</h3>
          </div>

          {loading ? (
            <LoadingSpinner text="Invoking LightGBM ML model..." />
          ) : !forecastResult ? (
            <div className="p-4 text-center text-muted">
              Select target store, product, and date to execute machine learning forecast.
            </div>
          ) : (
            <div className="flex-column gap-3">
              <div className="card alert-card p-3 bg-light border-primary text-center">
                <div className="text-sm text-muted">Predicted Sales Demand</div>
                <div className="text-2xl font-bold text-primary mt-1">
                  {forecastResult.predictedDemand?.toFixed(2)} units
                </div>
                <div className="text-xs text-muted mt-1">
                  Rounded Demand: <strong>{forecastResult.roundedDemand} units</strong>
                </div>
              </div>

              <div className="p-3 bg-gray-50 rounded border text-sm grid-2">
                <div>Store: <strong>{forecastResult.storeCode || forecastResult.storeId}</strong></div>
                <div>Product SKU: <strong>{forecastResult.sku || forecastResult.productId}</strong></div>
                <div>Target Date: <strong>{forecastResult.date}</strong></div>
                <div>Model Name: <strong>{forecastResult.modelName || 'LGBMRegressor'}</strong></div>
                <div>
                  Model Version: <strong>{forecastResult.modelVersion ? forecastResult.modelVersion : 'None'}</strong>
                </div>
              </div>

              {/* Visual Demand Chart */}
              <div className="mt-2" style={{ width: '100%', height: 180 }}>
                <ResponsiveContainer>
                  <BarChart
                    data={[
                      { name: 'Predicted Demand', value: forecastResult.predictedDemand || 0 }
                    ]}
                    margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="value" fill="#2563eb" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Forecasts;
