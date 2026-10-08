import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ErrorMessage from '../components/common/ErrorMessage';

const Register = () => {
  const [formData, setFormData] = useState({
    organizationName: '',
    organizationCode: '',
    email: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const { register, error, setError } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.organizationName || !formData.organizationCode || !formData.email || !formData.password) {
      setError('Please fill in all required fields.');
      return;
    }

    setLoading(true);
    try {
      await register(formData);
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

        <h2 className="text-xl font-semibold mb-3">Register New Organization</h2>

        <ErrorMessage message={error} />

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Organization Name</label>
            <input
              type="text"
              name="organizationName"
              className="form-control"
              placeholder="Acme Supermarkets"
              value={formData.organizationName}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Organization Code</label>
            <input
              type="text"
              name="organizationCode"
              className="form-control"
              placeholder="e.g. ACME-01"
              value={formData.organizationCode}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Admin Email Address</label>
            <input
              type="email"
              name="email"
              className="form-control"
              placeholder="admin@acme.com"
              value={formData.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              name="password"
              className="form-control"
              placeholder="Min 8 chars, uppercase, number, symbol"
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
            {loading ? 'Creating Organization...' : 'Register & Create Account'}
          </button>
        </form>

        <div className="text-center mt-4 text-sm text-muted">
          Already have an organization account?{' '}
          <Link to="/login" className="text-primary font-semibold text-decoration-none">
            Sign In
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Register;
