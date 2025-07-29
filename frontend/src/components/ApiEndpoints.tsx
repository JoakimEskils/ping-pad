import { useState } from 'react';
import type { User, ApiEndpoint, ApiTestResult } from '../types';
import Modal from './Modal';

interface ApiEndpointsProps {
  user: User;
}

export default function ApiEndpoints({ user }: ApiEndpointsProps) {
  const [endpoints, setEndpoints] = useState<ApiEndpoint[]>([]);
  const [testResults, setTestResults] = useState<ApiTestResult[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newEndpoint, setNewEndpoint] = useState({
    name: '',
    url: '',
    method: 'GET' as const,
    headers: '',
    body: ''
  });

  const handleCreateEndpoint = () => {
    setIsModalOpen(true);
  };

  const handleSubmitEndpoint = () => {
    if (!newEndpoint.name || !newEndpoint.url) {
      alert('Please fill in all required fields');
      return;
    }

    const headers: Record<string, string> = {};
    if (newEndpoint.headers) {
      try {
        newEndpoint.headers.split('\n').forEach(line => {
          const [key, value] = line.split(':').map(s => s.trim());
          if (key && value) {
            headers[key] = value;
          }
        });
      } catch (error) {
        console.error('Error parsing headers:', error);
      }
    }

    const endpoint: ApiEndpoint = {
      id: Date.now().toString(),
      name: newEndpoint.name,
      url: newEndpoint.url,
      method: newEndpoint.method,
      headers,
      body: newEndpoint.body || undefined,
      createdAt: new Date(),
      updatedAt: new Date()
    };

    setEndpoints(prev => [endpoint, ...prev]);
    setNewEndpoint({ name: '', url: '', method: 'GET', headers: '', body: '' });
    setIsModalOpen(false);
  };

  const handleTestEndpoint = async (endpoint: ApiEndpoint) => {
    const startTime = Date.now();
    
    try {
      const response = await fetch(endpoint.url, {
        method: endpoint.method,
        headers: endpoint.headers,
        body: endpoint.body,
      });
      
      const endTime = Date.now();
      const responseTime = endTime - startTime;
      
      const result: ApiTestResult = {
        id: Date.now().toString(),
        endpointId: endpoint.id,
        statusCode: response.status,
        responseTime,
        responseBody: await response.text(),
        responseHeaders: Object.fromEntries(response.headers.entries()),
        timestamp: new Date(),
      };
      
      setTestResults(prev => [result, ...prev]);
    } catch (error) {
      const result: ApiTestResult = {
        id: Date.now().toString(),
        endpointId: endpoint.id,
        statusCode: 0,
        responseTime: Date.now() - startTime,
        responseBody: '',
        responseHeaders: {},
        error: error instanceof Error ? error.message : 'Unknown error',
        timestamp: new Date(),
      };
      
      setTestResults(prev => [result, ...prev]);
    }
  };

  return (
    <div style={{ padding: '20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '8px' }}>API Endpoints</h1>
          <p style={{ color: '#666' }}>Create and test your API endpoints</p>
        </div>
        <button 
          onClick={handleCreateEndpoint}
          style={{
            backgroundColor: '#3182ce',
            color: 'white',
            padding: '8px 16px',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Create Endpoint
        </button>
      </div>

      {endpoints.length === 0 ? (
        <div style={{
          backgroundColor: 'white',
          padding: '48px',
          textAlign: 'center',
          borderRadius: '8px',
          border: '1px solid #e2e8f0'
        }}>
          <p style={{ fontSize: '18px', color: '#666', marginBottom: '16px' }}>
            No endpoints created yet
          </p>
          <p style={{ color: '#999', marginBottom: '24px' }}>
            Create your first API endpoint to start testing
          </p>
          <button 
            onClick={handleCreateEndpoint}
            style={{
              backgroundColor: '#3182ce',
              color: 'white',
              padding: '8px 16px',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            Create Your First Endpoint
          </button>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(400px, 1fr))', gap: '24px' }}>
          {endpoints.map(endpoint => (
            <div key={endpoint.id} style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              border: '1px solid #e2e8f0',
              overflow: 'hidden'
            }}>
              <div style={{ padding: '16px', borderBottom: '1px solid #e2e8f0' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <h3 style={{ fontSize: '18px', fontWeight: 'bold' }}>{endpoint.name}</h3>
                  <span style={{
                    backgroundColor: '#3182ce',
                    color: 'white',
                    padding: '4px 8px',
                    borderRadius: '4px',
                    fontSize: '12px'
                  }}>
                    {endpoint.method}
                  </span>
                </div>
              </div>
              <div style={{ padding: '16px' }}>
                <p style={{ color: '#666', marginBottom: '16px', fontSize: '14px' }}>
                  {endpoint.url}
                </p>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    onClick={() => handleTestEndpoint(endpoint)}
                    style={{
                      backgroundColor: '#38a169',
                      color: 'white',
                      padding: '6px 12px',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '14px'
                    }}
                  >
                    Test
                  </button>
                  <button style={{
                    backgroundColor: 'transparent',
                    color: '#666',
                    padding: '6px 12px',
                    border: '1px solid #e2e8f0',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '14px'
                  }}>
                    Edit
                  </button>
                  <button style={{
                    backgroundColor: 'transparent',
                    color: '#e53e3e',
                    padding: '6px 12px',
                    border: '1px solid #e2e8f0',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '14px'
                  }}>
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {testResults.length > 0 && (
        <div style={{ marginTop: '32px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '16px' }}>Recent Test Results</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
            {testResults.slice(0, 6).map(result => (
              <div key={result.id} style={{
                backgroundColor: 'white',
                padding: '16px',
                borderRadius: '8px',
                border: '1px solid #e2e8f0'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <span style={{
                    backgroundColor: result.statusCode >= 200 && result.statusCode < 300 ? '#38a169' : '#e53e3e',
                    color: 'white',
                    padding: '4px 8px',
                    borderRadius: '4px',
                    fontSize: '12px'
                  }}>
                    {result.statusCode}
                  </span>
                  <span style={{ fontSize: '14px', color: '#666' }}>
                    {result.responseTime}ms
                  </span>
                </div>
                {result.error && (
                  <p style={{ fontSize: '14px', color: '#e53e3e', marginTop: '8px' }}>
                    {result.error}
                  </p>
                )}
                <p style={{ fontSize: '12px', color: '#999', marginTop: '8px' }}>
                  {result.timestamp.toLocaleString()}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create API Endpoint"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
              Name *
            </label>
            <input
              type="text"
              value={newEndpoint.name}
              onChange={(e) => setNewEndpoint(prev => ({ ...prev, name: e.target.value }))}
              placeholder="My API Endpoint"
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #e2e8f0',
                borderRadius: '4px',
                fontSize: '14px',
                boxSizing: 'border-box'
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
              URL *
            </label>
            <input
              type="url"
              value={newEndpoint.url}
              onChange={(e) => setNewEndpoint(prev => ({ ...prev, url: e.target.value }))}
              placeholder="https://api.example.com/endpoint"
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #e2e8f0',
                borderRadius: '4px',
                fontSize: '14px',
                boxSizing: 'border-box'
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
              Method
            </label>
            <select
              value={newEndpoint.method}
              onChange={(e) => setNewEndpoint(prev => ({ ...prev, method: e.target.value as any }))}
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #e2e8f0',
                borderRadius: '4px',
                fontSize: '14px',
                boxSizing: 'border-box'
              }}
            >
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
              <option value="PATCH">PATCH</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
              Headers (one per line, format: Key: Value)
            </label>
            <textarea
              value={newEndpoint.headers}
              onChange={(e) => setNewEndpoint(prev => ({ ...prev, headers: e.target.value }))}
              placeholder="Content-Type: application/json&#10;Authorization: Bearer token"
              rows={4}
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #e2e8f0',
                borderRadius: '4px',
                fontSize: '14px',
                boxSizing: 'border-box',
                fontFamily: 'monospace'
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
              Request Body (for POST, PUT, PATCH)
            </label>
            <textarea
              value={newEndpoint.body}
              onChange={(e) => setNewEndpoint(prev => ({ ...prev, body: e.target.value }))}
              placeholder='{"key": "value"}'
              rows={4}
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #e2e8f0',
                borderRadius: '4px',
                fontSize: '14px',
                boxSizing: 'border-box',
                fontFamily: 'monospace'
              }}
            />
          </div>

          <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '16px' }}>
            <button
              onClick={() => setIsModalOpen(false)}
              style={{
                backgroundColor: 'transparent',
                color: '#666',
                padding: '8px 16px',
                border: '1px solid #e2e8f0',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              Cancel
            </button>
            <button
              onClick={handleSubmitEndpoint}
              style={{
                backgroundColor: '#3182ce',
                color: 'white',
                padding: '8px 16px',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              Create Endpoint
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
} 