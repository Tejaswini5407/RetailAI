import React, { useState, useEffect } from 'react';
import { inventoryApi } from '../api/inventoryApi';
import { storeApi } from '../api/storeApi';
import { productApi } from '../api/productApi';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import Badge from '../components/common/Badge';

const Assessment = () => {
  const [stores, setStores] = useState([]);
  const [products, setProducts] = useState([]);
  const [mode, setMode] = useState('forecast'); // 'direct' | 'forecast'

  // Common Form Inputs
  const [storeId, setStoreId] = useState('');
  const [productId, setProductId] = useState('');
  const [forecastHorizonDays, setForecastHorizonDays] = useState('7');
  const [leadTimeDays, setLeadTimeDays] = useState('7');
  const [safetyStock, setSafetyStock] = useState('10');
  const [existingOnOrder, setExistingOnOrder] = useState('0');
  const [moq, setMoq] = useState('1');

  // Mode Specific Inputs
  const [currentInventory, setCurrentInventory] = useState('15'); // For direct mode
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]); // For forecast mode

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);

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

  const handleAssess = async (e) => {
    e.preventDefault();
    if (!storeId || !productId) {
      setError('Please select a store and product.');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      if (mode === 'direct') {
        const payload = {
          storeId: parseInt(storeId, 10),
          productId: parseInt(productId, 10),
          currentInventory: parseInt(currentInventory, 10),
          forecastHorizonDays: parseInt(forecastHorizonDays, 10),
          leadTimeDays: parseInt(leadTimeDays, 10),
          safetyStock: parseInt(safetyStock, 10),
          existingOnOrder: parseInt(existingOnOrder, 10),
          moq: parseInt(moq, 10)
        };
        const res = await inventoryApi.assessInventory(payload);
        setResult({ type: 'direct', data: res });
      } else {
        const payload = {
          storeId: parseInt(storeId, 10),
          productId: parseInt(productId, 10),
          date: date,
          forecastHorizonDays: parseInt(forecastHorizonDays, 10),
          leadTimeDays: parseInt(leadTimeDays, 10),
          safetyStock: parseInt(safetyStock, 10),
          existingOnOrder: parseInt(existingOnOrder, 10),
          moq: parseInt(moq, 10)
        };
        const res = await inventoryApi.assessForecastInventory(payload);
        setResult({ type: 'forecast', data: res });
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to compute inventory assessment.');
    } finally {
      setLoading(false);
    }
  };

  const getAssessmentObj = () => {
    if (!result) return null;
    return result.type === 'forecast' ? result.data.assessment : result.data;
  };

  const getForecastObj = () => {
    if (!result || result.type !== 'forecast') return null;
    return result.data.forecast;
  };

  const assessment = getAssessmentObj();
  const forecast = getForecastObj();

  return (
    <div className="assessment-page flex-column gap-4">
      <div>
        <h1 className="text-2xl font-bold">Inventory Intelligence & Order Recommendation Engine</h1>
        <p className="text-muted text-sm">
          Run automated stockout risk assessments, safety stock calculations, and ML demand-driven order recommendations.
        </p>
      </div>

      <ErrorMessage message={error} />

      <div className="grid-2">
        {/* Input Form */}
        <div className="card">
          <div className="card-header flex-between flex-align-center">
            <h3 className="font-semibold text-dark">Assessment Mode</h3>
            <div className="flex-gap-2">
              <button
                className={`btn btn-sm ${mode === 'forecast' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setMode('forecast')}
              >
                🔮 ML Forecast Driven
              </button>
              <button
                className={`btn btn-sm ${mode === 'direct' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setMode('direct')}
              >
                ⚙️ Direct Manual Input
              </button>
            </div>
          </div>

          <form onSubmit={handleAssess}>
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

            {mode === 'forecast' ? (
              <div className="form-group">
                <label className="form-label">Forecast Start Date</label>
                <input
                  type="date"
                  className="form-control"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  required
                />
              </div>
            ) : (
              <div className="form-group">
                <label className="form-label">Current Inventory On Hand</label>
                <input
                  type="number"
                  className="form-control"
                  value={currentInventory}
                  onChange={(e) => setCurrentInventory(e.target.value)}
                  required
                />
              </div>
            )}

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Horizon (Days)</label>
                <input
                  type="number"
                  min="1"
                  className="form-control"
                  value={forecastHorizonDays}
                  onChange={(e) => setForecastHorizonDays(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Supplier Lead Time (Days)</label>
                <input
                  type="number"
                  min="1"
                  className="form-control"
                  value={leadTimeDays}
                  onChange={(e) => setLeadTimeDays(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="grid-3 mt-2">
              <div className="form-group">
                <label className="form-label text-xs">Safety Stock</label>
                <input
                  type="number"
                  min="0"
                  className="form-control"
                  value={safetyStock}
                  onChange={(e) => setSafetyStock(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label text-xs">Existing On Order</label>
                <input
                  type="number"
                  min="0"
                  className="form-control"
                  value={existingOnOrder}
                  onChange={(e) => setExistingOnOrder(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label text-xs">MOQ (Min Order)</label>
                <input
                  type="number"
                  min="1"
                  className="form-control"
                  value={moq}
                  onChange={(e) => setMoq(e.target.value)}
                />
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary w-100 mt-4 py-2 font-semibold"
              style={{ width: '100%' }}
              disabled={loading}
            >
              {loading ? 'Evaluating Inventory Engine...' : 'Run Inventory Intelligence Engine'}
            </button>
          </form>
        </div>

        {/* Results Panel */}
        <div className="card flex-column justify-between">
          <div className="card-header">
            <h3 className="font-semibold text-dark">Recommendation & Risk Results</h3>
          </div>

          {loading ? (
            <LoadingSpinner text="Executing Phase 5 deterministic calculation & Phase 6 ML inference..." />
          ) : !assessment ? (
            <div className="p-4 text-center text-muted">
              Select a store and product, then click "Run Inventory Intelligence Engine" to generate recommendations.
            </div>
          ) : (
            <div className="flex-column gap-3">
              {/* Status Header */}
              <div className="flex-between flex-align-center p-3 rounded bg-gray-50 border">
                <div>
                  <div className="text-xs text-muted">OPERATIONAL STATUS</div>
                  <div className="mt-1">
                    <Badge value={assessment.status} />
                  </div>
                </div>

                <div>
                  <div className="text-xs text-muted">STOCKOUT RISK</div>
                  <div className="mt-1">
                    <Badge value={assessment.stockoutRisk} />
                  </div>
                </div>

                <div>
                  <div className="text-xs text-muted">REORDER REQUIRED</div>
                  <div className="mt-1 font-bold text-dark">
                    {assessment.reorderRequired ? 'YES ⚠️' : 'NO ✅'}
                  </div>
                </div>
              </div>

              {/* Order Quantity Banner */}
              <div className="card alert-card p-3 bg-light border-primary text-center">
                <div className="text-sm text-muted">Recommended Purchase Order Quantity</div>
                <div className="text-2xl font-bold text-primary mt-1">
                  {assessment.recommendedOrderQty} units
                </div>
              </div>

              {/* ML Model Insights (If Forecast Driven) */}
              {forecast && (
                <div className="p-3 bg-gray-50 rounded border">
                  <div className="text-xs font-bold text-muted text-uppercase mb-2">ML Demand Inference Engine</div>
                  <div className="grid-2 text-sm">
                    <div>Predicted Demand: <strong>{forecast.predictedDemand?.toFixed(2)} units</strong></div>
                    <div>Rounded Demand: <strong>{forecast.roundedDemand} units</strong></div>
                    <div>ML Model Name: <strong>{forecast.modelName || 'LGBMRegressor'}</strong></div>
                    <div>
                      Model Version: <strong>{forecast.modelVersion ? forecast.modelVersion : 'None'}</strong>
                    </div>
                  </div>
                </div>
              )}

              {/* Calculated Metrics Breakdown */}
              <div className="p-3 bg-gray-50 rounded border text-sm">
                <div className="text-xs font-bold text-muted text-uppercase mb-2">Inventory Metrics</div>
                <div className="grid-2 gap-2">
                  <div>Avg Daily Demand: <strong>{assessment.averageDailyDemand?.toFixed(2)} units/day</strong></div>
                  <div>Lead Time Demand: <strong>{assessment.leadTimeDemand?.toFixed(2)} units</strong></div>
                  <div>Reorder Point: <strong>{assessment.reorderPoint} units</strong></div>
                  <div>Net Inventory Position: <strong>{assessment.netInventoryPosition} units</strong></div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Assessment;
