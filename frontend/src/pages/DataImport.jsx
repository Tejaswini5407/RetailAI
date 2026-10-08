import React, { useState, useEffect } from 'react';
import { importApi } from '../api/importApi';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorMessage from '../components/common/ErrorMessage';
import Badge from '../components/common/Badge';

const DataImport = () => {
  const [activeTab, setActiveTab] = useState('PRODUCTS');
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [preview, setPreview] = useState(null);
  const [importSummary, setImportSummary] = useState(null);
  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  useEffect(() => {
    if (activeTab === 'HISTORY') {
      fetchHistory();
    }
  }, [activeTab]);

  const fetchHistory = async () => {
    setHistoryLoading(true);
    try {
      const data = await importApi.getImportHistory();
      setHistory(data);
    } catch (err) {
      setError('Failed to fetch import history.');
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setPreview(null);
      setImportSummary(null);
      setError(null);
    }
  };

  const handlePreview = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a CSV file to upload.');
      return;
    }

    setLoading(true);
    setError(null);
    setPreview(null);
    setImportSummary(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const data = await importApi.previewImport(formData, activeTab);
      setPreview(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to parse and validate import file.');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmImport = async () => {
    if (!preview || !preview.previewData || preview.previewData.length === 0) {
      setError('No valid rows available to import.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const payload = {
        importType: activeTab,
        fileName: file ? file.name : 'import.csv',
        rows: preview.previewData,
      };
      const summary = await importApi.confirmImport(payload);
      setImportSummary(summary);
      setPreview(null);
      setFile(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Import execution failed.');
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadgeType = (status) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'COMPLETED_WITH_ERRORS': return 'warning';
      case 'FAILED': return 'danger';
      default: return 'neutral';
    }
  };

  return (
    <div className="data-import-page flex-column gap-4">
      <div>
        <h1 className="text-2xl font-bold">Data Import Center</h1>
        <p className="text-muted text-sm">
          Bulk ingest catalog items, retail stores, historical sales, and inventory levels via validated CSV templates.
        </p>
      </div>

      <ErrorMessage message={error} />

      {/* Tab Controls */}
      <div className="flex gap-2 border-b pb-2">
        <button
          className={`btn btn-sm ${activeTab === 'PRODUCTS' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => { setActiveTab('PRODUCTS'); setPreview(null); setFile(null); setImportSummary(null); }}
        >
          📦 Import Products
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'STORES' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => { setActiveTab('STORES'); setPreview(null); setFile(null); setImportSummary(null); }}
        >
          🏪 Import Stores
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'SALES' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => { setActiveTab('SALES'); setPreview(null); setFile(null); setImportSummary(null); }}
        >
          📊 Import Historical Sales
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'INVENTORY' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => { setActiveTab('INVENTORY'); setPreview(null); setFile(null); setImportSummary(null); }}
        >
          📋 Import Inventory
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'HISTORY' ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => setActiveTab('HISTORY')}
        >
          📜 Import History
        </button>
      </div>

      {activeTab !== 'HISTORY' ? (
        <div className="grid-2">
          {/* File Upload Card */}
          <div className="card">
            <div className="card-header flex justify-between align-center">
              <h3 className="font-semibold text-dark">
                Upload {activeTab.replace('_', ' ')} CSV
              </h3>
              <a
                href={importApi.downloadTemplateUrl(activeTab)}
                download
                className="text-xs text-primary font-medium underline"
              >
                📥 Download CSV Template
              </a>
            </div>

            <form onSubmit={handlePreview} className="flex-column gap-3 mt-3">
              <div className="form-group">
                <label className="form-label">Select CSV File</label>
                <input
                  type="file"
                  accept=".csv"
                  className="form-control"
                  onChange={handleFileChange}
                  required
                />
                <span className="text-xs text-muted mt-1">
                  Supported format: .csv (Max 10MB). High-performance streaming validation applied.
                </span>
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading || !file}
              >
                {loading ? 'Validating & Parsing File...' : '🔍 Parse & Validate File'}
              </button>
            </form>
          </div>

          {/* Guidelines Card */}
          <div className="card bg-light">
            <h4 className="font-semibold text-dark mb-2">Import Guidelines</h4>
            <ul className="text-sm text-muted flex-column gap-2 pl-4">
              <li>Organization scope is automatically enforced from your authenticated session.</li>
              <li>Data is validated against database constraints before confirmation.</li>
              <li>Row errors and duplicates will be flagged for review prior to insertion.</li>
              <li>Store codes and Product SKUs must exist in catalog for Sales and Inventory imports.</li>
            </ul>
          </div>
        </div>
      ) : null}

      {/* Validation Preview Section */}
      {preview && (
        <div className="card flex-column gap-3">
          <div className="card-header flex justify-between align-center">
            <h3 className="font-semibold text-dark">Validation Summary & Preview</h3>
            <button
              className="btn btn-success"
              onClick={handleConfirmImport}
              disabled={loading || preview.importableRows === 0}
            >
              ✅ Confirm & Import {preview.importableRows} Valid Rows
            </button>
          </div>

          {/* Stat Badges */}
          <div className="grid-4 gap-3 my-2">
            <div className="p-3 bg-light rounded text-center">
              <div className="text-xs text-muted">Total Rows</div>
              <div className="text-xl font-bold">{preview.totalRows}</div>
            </div>
            <div className="p-3 bg-light rounded text-center">
              <div className="text-xs text-muted">Valid Rows</div>
              <div className="text-xl font-bold text-success">{preview.validRows}</div>
            </div>
            <div className="p-3 bg-light rounded text-center">
              <div className="text-xs text-muted">Invalid Rows</div>
              <div className="text-xl font-bold text-danger">{preview.invalidRows}</div>
            </div>
            <div className="p-3 bg-light rounded text-center">
              <div className="text-xs text-muted">Duplicate Rows</div>
              <div className="text-xl font-bold text-warning">{preview.duplicateRows}</div>
            </div>
          </div>

          {/* Errors Table */}
          {preview.validationErrors && preview.validationErrors.length > 0 && (
            <div>
              <h4 className="font-semibold text-danger mb-2">Validation Errors ({preview.validationErrors.length})</h4>
              <div className="table-responsive max-h-48 overflow-y-auto border rounded">
                <table className="table text-sm">
                  <thead>
                    <tr>
                      <th>Row #</th>
                      <th>Field</th>
                      <th>Error Description</th>
                    </tr>
                  </thead>
                  <tbody>
                    {preview.validationErrors.map((err, idx) => (
                      <tr key={idx}>
                        <td><Badge type="neutral">Row {err.row}</Badge></td>
                        <td><code>{err.field}</code></td>
                        <td className="text-danger">{err.message}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Valid Data Sample Preview */}
          {preview.previewData && preview.previewData.length > 0 && (
            <div>
              <h4 className="font-semibold text-dark mb-2">Valid Data Sample (First {Math.min(10, preview.previewData.length)} rows)</h4>
              <div className="table-responsive border rounded">
                <table className="table text-sm">
                  <thead>
                    <tr>
                      {Object.keys(preview.previewData[0]).map((key) => (
                        <th key={key}>{key}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {preview.previewData.slice(0, 10).map((row, idx) => (
                      <tr key={idx}>
                        {Object.values(row).map((val, cIdx) => (
                          <td key={cIdx}>{val}</td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Import Summary Result Card */}
      {importSummary && (
        <div className="card border-success">
          <div className="card-header flex justify-between align-center">
            <h3 className="font-semibold text-success">Import Complete</h3>
            <Badge type={getStatusBadgeType(importSummary.status)}>{importSummary.status}</Badge>
          </div>
          <div className="grid-3 gap-3 my-2">
            <div>Total Processed: <strong>{importSummary.totalRows}</strong></div>
            <div className="text-success">Successful: <strong>{importSummary.successfulRows}</strong></div>
            <div className="text-danger">Failed: <strong>{importSummary.failedRows}</strong></div>
          </div>
        </div>
      )}

      {/* Import History Tab */}
      {activeTab === 'HISTORY' && (
        <div className="card">
          <h3 className="font-semibold text-dark mb-3">Tenant Import History Log</h3>
          {historyLoading ? (
            <LoadingSpinner />
          ) : history.length === 0 ? (
            <div className="text-muted text-center py-4">No import history logs recorded for this tenant.</div>
          ) : (
            <div className="table-responsive">
              <table className="table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>File Name</th>
                    <th>Uploaded Date</th>
                    <th>Total Rows</th>
                    <th>Successful</th>
                    <th>Failed</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((h) => (
                    <tr key={h.id}>
                      <td><Badge type="info">{h.importType}</Badge></td>
                      <td className="font-medium">{h.fileName}</td>
                      <td>{h.uploadedAt ? new Date(h.uploadedAt).toLocaleString() : 'N/A'}</td>
                      <td>{h.totalRows}</td>
                      <td className="text-success font-semibold">{h.successfulRows}</td>
                      <td className={h.failedRows > 0 ? 'text-danger font-semibold' : ''}>{h.failedRows}</td>
                      <td><Badge type={getStatusBadgeType(h.status)}>{h.status}</Badge></td>
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

export default DataImport;
