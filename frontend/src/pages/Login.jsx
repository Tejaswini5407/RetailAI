import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ErrorMessage from '../components/common/ErrorMessage';

const Login = () => {
  const [formData, setFormData] = useState({
    organizationCode: '',
    email: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const { login, error, errorDetails, setError } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.organizationCode || !formData.email || !formData.password) {
      setError('Please fill in all fields.');
      return;
    }

    setLoading(true);
    try {
      await login(formData);
      navigate('/');
    } catch (err) {
      // Error handled by AuthContext
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex-center screen-height bg-gray-50 p-3">
      <div className="auth-container card p-4">
        <div className="text-center mb-4">
          <h1 className="text-2xl font-bold text-dark">
            Retail<span className="text-primary">AI</span>
          </h1>
          <p className="text-muted text-sm mt-1">Multi-Tenant Retail Intelligence Platform</p>
        </div>

        <h2 className="text-xl font-semibold mb-3">Sign In</h2>

        {errorDetails?.isOrgNotFound ? (
          <div className="alert alert-warning mb-3 p-3 text-start border-start border-4 border-warning">
            <div className="fw-bold text-dark mb-1">
              {error || 'This organization code is not registered. Please register your organization before logging in.'}
            </div>
            <div className="mt-2">
              <Link to="/register" className="btn btn-primary btn-sm font-semibold text-white text-decoration-none">
                Register Organization →
              </Link>
            </div>
          </div>
        ) : (
          <ErrorMessage message={error} />
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group mb-3">
            <label className="form-label font-semibold">Organization Code</label>
            <input
              type="text"
              name="organizationCode"
              className="form-control"
              placeholder="e.g. ORG-1001"
              value={formData.organizationCode}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group mb-3">
            <label className="form-label font-semibold">Email Address</label>
            <input
              type="email"
              name="email"
              className="form-control"
              placeholder="user@organization.com"
              value={formData.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group mb-3">
            <label className="form-label font-semibold">Password</label>
            <input
              type="password"
              name="password"
              className="form-control"
              placeholder="••••••••"
              value={formData.password}
              onChange={handleChange}
              required
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary w-100 mt-3 py-2 font-semibold"
            style={{ width: '100%' }}
            disabled={loading}
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <div className="text-center mt-4 text-sm text-muted">
          Need a new organization account?{' '}
          <Link to="/register" className="text-primary font-semibold text-decoration-none">
            Register Organization
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Login;
