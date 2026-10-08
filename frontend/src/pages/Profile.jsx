import React from 'react';
import { useAuth } from '../context/AuthContext';
import Badge from '../components/common/Badge';

const Profile = () => {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <div className="profile-page flex-column gap-4" style={{ maxWidth: '600px' }}>
      <div>
        <h1 className="text-2xl font-bold">User Account Profile</h1>
        <p className="text-muted text-sm">Active user credentials & tenant organization details</p>
      </div>

      <div className="card flex-column gap-3">
        <div className="card-header flex-between flex-align-center">
          <h3 className="font-semibold text-dark">Identity Details</h3>
          <Badge value={user.role} />
        </div>

        <div className="grid-2 gap-3 text-sm">
          <div>
            <span className="text-muted block text-xs">USER ID</span>
            <strong>#{user.userId || user.id}</strong>
          </div>

          <div>
            <span className="text-muted block text-xs">EMAIL ADDRESS</span>
            <strong>{user.username || user.email}</strong>
          </div>

          <div>
            <span className="text-muted block text-xs">TENANT ORG CODE</span>
            <strong className="text-primary">{user.organizationCode}</strong>
          </div>

          <div>
            <span className="text-muted block text-xs">ORGANIZATION ID</span>
            <strong>#{user.organizationId}</strong>
          </div>
        </div>

        <div className="mt-3 p-3 bg-gray-50 rounded border text-xs text-muted">
          ℹ️ Your session is secured with JWT Bearer Token authorization. All backend requests enforce
          tenant context isolation for organization code <strong>{user.organizationCode}</strong>.
        </div>
      </div>
    </div>
  );
};

export default Profile;
