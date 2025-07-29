import { useState } from 'react';
import type { User } from '../types';
import ApiEndpoints from './ApiEndpoints';
import Webhooks from './Webhooks';

interface DashboardProps {
  user: User;
  onLogout: () => void;
}

export default function Dashboard({ user, onLogout }: DashboardProps) {
  const [activeTab, setActiveTab] = useState<'endpoints' | 'webhooks'>('endpoints');

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f7fafc' }}>
      <div style={{ 
        backgroundColor: 'white', 
        borderBottom: '1px solid #e2e8f0', 
        padding: '16px 24px' 
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h1 style={{ fontSize: '24px', fontWeight: 'bold', color: '#3182ce' }}>PingPad</h1>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <span>Welcome, {user.name}</span>
            <button 
              onClick={onLogout}
              style={{
                backgroundColor: 'transparent',
                color: '#666',
                padding: '6px 12px',
                border: '1px solid #e2e8f0',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              Logout
            </button>
          </div>
        </div>
      </div>

      <div style={{ 
        backgroundColor: 'white', 
        borderBottom: '1px solid #e2e8f0', 
        padding: '0 24px' 
      }}>
        <div style={{ display: 'flex', gap: '32px' }}>
          <button
            onClick={() => setActiveTab('endpoints')}
            style={{
              backgroundColor: activeTab === 'endpoints' ? '#3182ce' : 'transparent',
              color: activeTab === 'endpoints' ? 'white' : '#666',
              padding: '12px 16px',
              border: 'none',
              cursor: 'pointer',
              fontSize: '16px',
              fontWeight: activeTab === 'endpoints' ? 'bold' : 'normal'
            }}
          >
            API Endpoints
          </button>
          <button
            onClick={() => setActiveTab('webhooks')}
            style={{
              backgroundColor: activeTab === 'webhooks' ? '#3182ce' : 'transparent',
              color: activeTab === 'webhooks' ? 'white' : '#666',
              padding: '12px 16px',
              border: 'none',
              cursor: 'pointer',
              fontSize: '16px',
              fontWeight: activeTab === 'webhooks' ? 'bold' : 'normal'
            }}
          >
            Webhooks
          </button>
        </div>
      </div>

      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '32px 20px' }}>
        {activeTab === 'endpoints' ? (
          <ApiEndpoints user={user} />
        ) : (
          <Webhooks user={user} />
        )}
      </div>
    </div>
  );
} 