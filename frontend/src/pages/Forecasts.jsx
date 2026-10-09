import React, { useState, useEffect } from 'react';
import { forecastApi } from '../api/forecastApi';
import { storeApi } from '../api/storeApi';
import { productApi } from '../api/productApi';
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
  CartesianGrid
} from 'recharts';

const Forecasts = () => {
  const [activeTab, setActiveTab] = useState('GENERATE');
  const [stores, setStores] = useState([]);
  const [products, setProducts] = useState([]);
  const [storeId, setStoreId] = useState('');
  const [productId, setProductId] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [horizonDays, setHorizonDays] = useState(7);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [forecastResult, setForecastResult] = useState(null);
  const [explanation, setExplanation] = useState(null);

  const [accuracy, setAccuracy] = useState(null);
  const [accuracyLoading, setAccuracyLoading] = useState(false);

  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

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

  useEffect(() => {
    if (activeTab === 'ACCURACY') fetchAccuracy();
    if (activeTab === 'HISTORY') fetchHistory();
  }, [activeTab]);

  const fetchAccuracy = async () => {
    setAccuracyLoading(true);
    try {
      const data = await forecastApi.getForecastAccuracy();
      setAccuracy(data);
    } catch (err) {
      setError('Failed to load forecast accuracy metrics.');
    } finally {
      setAccuracyLoading(false);
    }
  };

  const fetchHistory = async () => {
    setHistoryLoading(true);
    try {
      const data = await forecastApi.getForecastHistory();
      setHistory(data);
    } catch (err) {
      setError('Failed to load forecast history.');
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleGenerateForecast = async (e) => {
    e.preventDefault();
    if (!storeId || !productId || !date) {
      setError('Please select a store, product, and target date.');
      return;
    }

    setLoading(true);
    setError(null);
    setForecastResult(null);
    setExplanation(null);

    try {
      const payload = {
        storeId: parseInt(storeId, 10),
        productId: parseInt(productId, 10),
        date: date,
        horizonDays: parseInt(horizonDays, 10)
      };
      const res = await forecastApi.generateForecast(payload);
      setForecastResult(res);

      // Fetch explanation factors
      const exp = await forecastApi.getForecastExplanation(storeId, productId);
      setExplanation(exp);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to generate demand forecast.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="forecasts-page flex-column gap-4">
      <div>
        <h1 className="text-2xl font-bold">Demand Forecasting & Accuracy Center</h1>
        <p className="text-muted text-sm">
          Generate machine-learning sales demand predictions, inspect model feature factors, and track forecast accuracy metrics.
        </p>
      </div>

      <ErrorMessage message={error} />

      {/* Tab Controls */}
      <div className="flex gap-2 border-b pb-2">
        <button
          className={`btn btn-sm ${activeTab === 'GENERATE' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('GENERATE')}
        >
          🔮 Generate Forecast
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'FACTORS' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('FACTORS')}
        >
          🧠 Forecast Factors ("Why?")
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'ACCURACY' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('ACCURACY')}
        >
          📈 Accuracy Tracking
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'HISTORY' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('HISTORY')}
        >
          📜 Forecast History
        </button>
      </div>

      {activeTab === 'GENERATE' && (
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
                <label className="form-label">Target Start Date</label>
                <input
                  type="date"
                  className="form-control"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Forecast Horizon Window</label>
                <select
                  className="form-control"
                  value={horizonDays}
                  onChange={(e) => setHorizonDays(e.target.value)}
                >
                  <option value={7}>7 Days (Short-term)</option>
                  <option value={14}>14 Days (Bi-weekly)</option>
                  <option value={30}>30 Days (Monthly window)</option>
                </select>
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
              <LoadingSpinner text="Invoking Random Forest ML model..." />
            ) : !forecastResult ? (
              <div className="p-4 text-center text-muted">
                Select target store, product, and target date to execute machine learning forecast.
              </div>
            ) : (
              <div className="flex-column gap-3">
                <div className="card alert-card p-3 bg-light border-primary text-center">
                  <div className="text-sm text-muted">Predicted Sales Demand ({horizonDays}-Day Window)</div>
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
                  <div>Model Name: <strong>{forecastResult.modelName || 'Random Forest'}</strong></div>
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
      )}

      {/* Forecast Factors Tab */}
      {activeTab === 'FACTORS' && (
        <div className="card flex-column gap-3">
          <div className="flex justify-between items-center border-b pb-2">
            <div>
              <h3 className="font-semibold text-dark">Active Model Pipeline Input Signals</h3>
              <p className="text-sm text-muted">
                General feature pipeline signals used by the Random Forest demand prediction model:
              </p>
            </div>
            {explanation?.inventoryFactors?.modelVersion && (
              <span className="badge badge-info text-xs">
                Model: {explanation.inventoryFactors.modelName || 'Random Forest'} ({explanation.inventoryFactors.modelVersion})
              </span>
            )}
          </div>

          <div className="table-responsive">
            <table className="table text-sm">
              <thead>
                <tr>
                  <th>Signal Factor</th>
                  <th>Feature Group Weight</th>
                  <th>Pipeline Description</th>
                </tr>
              </thead>
              <tbody>
                {(explanation?.factors && explanation.factors.length > 0 ? explanation.factors : [
                  { factor: "Recent Demand Trend", impact: "High", description: "Weighted moving average of recent 7-day sales volume." },
                  { factor: "Day-of-Week Seasonality", impact: "Medium", description: "Historical demand patterns for target day of week." },
                  { factor: "Lagged Sales Features", impact: "High", description: "7-day, 14-day, and 28-day historical lag values." },
                  { factor: "Rolling Demand Windows", impact: "Medium", description: "Rolling 7-day and 28-day mean sales per store/product." },
                  { factor: "Price Information", impact: "Low", description: "Unit price elasticity and historical price ratio where available." }
                ]).map((f, i) => (
                  <tr key={i}>
                    <td className="font-semibold">{f.factor}</td>
                    <td><Badge type="info" value={f.impact}>{f.impact}</Badge></td>
                    <td>{f.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Accuracy Tracking Tab */}
      {activeTab === 'ACCURACY' && (
        <div className="card flex-column gap-3">
          <h3 className="font-semibold text-dark">Forecast Accuracy Metrics</h3>
          {accuracyLoading ? (
            <LoadingSpinner />
          ) : !accuracy || !accuracy.sufficientData ? (
            <div className="p-4 bg-light rounded text-center text-muted border">
              ⚠️ {accuracy?.message || 'Accuracy will be available after actual sales data is recorded for the forecast period.'}
            </div>
          ) : (
            <div className="grid-3 gap-3">
              <div className="p-3 bg-light rounded border text-center">
                <div className="text-xs text-muted">WAPE (Weighted Abs % Error)</div>
                <div className="text-2xl font-bold text-primary">{accuracy.wape?.toFixed(1)}%</div>
              </div>
              <div className="p-3 bg-light rounded border text-center">
                <div className="text-xs text-muted">MAE (Mean Absolute Error)</div>
                <div className="text-2xl font-bold text-dark">{accuracy.mae?.toFixed(2)} units</div>
              </div>
              <div className="p-3 bg-light rounded border text-center">
                <div className="text-xs text-muted">RMSE (Root Mean Square Error)</div>
                <div className="text-2xl font-bold text-dark">{accuracy.rmse?.toFixed(2)} units</div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* History Tab */}
      {activeTab === 'HISTORY' && (
        <div className="card">
          <h3 className="font-semibold text-dark mb-3">Persisted Forecast History Log</h3>
          {historyLoading ? (
            <LoadingSpinner />
          ) : history.length === 0 ? (
            <div className="text-muted text-center py-4">No forecast history records created yet.</div>
          ) : (
            <div className="table-responsive">
              <table className="table text-sm">
                <thead>
                  <tr>
                    <th>Run ID</th>
                    <th>Store Code</th>
                    <th>Product SKU</th>
                    <th>Forecast Date</th>
                    <th>Target Date</th>
                    <th>Horizon</th>
                    <th>Predicted Demand</th>
                    <th>Model</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((h) => (
                    <tr key={h.id}>
                      <td><code>{h.forecastRunId}</code></td>
                      <td className="font-semibold">{h.storeCode}</td>
                      <td>{h.sku}</td>
                      <td>{h.forecastDate}</td>
                      <td>{h.targetDate}</td>
                      <td>{h.horizonDays} days</td>
                      <td className="text-primary font-bold">{h.predictedDemand?.toFixed(2)}</td>
                      <td><Badge type="info">{h.modelName}</Badge></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Forecasts;
