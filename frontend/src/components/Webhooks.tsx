import { useState } from 'react';
import type { User, Webhook, WebhookEvent } from '../types';
import Modal from './Modal';

interface WebhooksProps {
  user: User;
}

export default function Webhooks({ user }: WebhooksProps) {
  const [webhooks, setWebhooks] = useState<Webhook[]>([]);
  const [webhookEvents, setWebhookEvents] = useState<WebhookEvent[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newWebhook, setNewWebhook] = useState({
    name: '',
    url: '',
    events: '',
    secret: '',
    isActive: true
  });

  const handleCreateWebhook = () => {
    setIsModalOpen(true);
  };

  const handleSubmitWebhook = () => {
    if (!newWebhook.name || !newWebhook.url) {
      alert('Please fill in all required fields');
      return;
    }

    const events = newWebhook.events
      .split(',')
      .map(event => event.trim())
      .filter(event => event.length > 0);

    const webhook: Webhook = {
      id: Date.now().toString(),
      name: newWebhook.name,
      url: newWebhook.url,
      events: events.length > 0 ? events : ['all'],
      secret: newWebhook.secret || undefined,
      isActive: newWebhook.isActive,
      createdAt: new Date(),
      updatedAt: new Date()
    };

    setWebhooks(prev => [webhook, ...prev]);
    setNewWebhook({ name: '', url: '', events: '', secret: '', isActive: true });
    setIsModalOpen(false);
  };

  const handleTestWebhook = async (webhook: Webhook) => {
    const startTime = Date.now();
    
    try {
      const testPayload = {
        event: 'test',
        timestamp: new Date().toISOString(),
        data: {
          message: 'This is a test webhook event',
          userId: user.id
        }
      };

      const response = await fetch(webhook.url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(webhook.secret && { 'X-Webhook-Secret': webhook.secret })
        },
        body: JSON.stringify(testPayload),
      });
      
      const endTime = Date.now();
      const responseTime = endTime - startTime;
      
      const event: WebhookEvent = {
        id: Date.now().toString(),
        webhookId: webhook.id,
        eventType: 'test',
        payload: testPayload,
        status: response.ok ? 'sent' : 'failed',
        responseCode: response.status,
        responseTime,
        timestamp: new Date(),
      };
      
      setWebhookEvents(prev => [event, ...prev]);
    } catch (error) {
      const event: WebhookEvent = {
        id: Date.now().toString(),
        webhookId: webhook.id,
        eventType: 'test',
        payload: { error: 'Test failed' },
        status: 'failed',
        responseTime: Date.now() - startTime,
        error: error instanceof Error ? error.message : 'Unknown error',
        timestamp: new Date(),
      };
      
      setWebhookEvents(prev => [event, ...prev]);
    }
  };

  return (
    <div style={{ padding: '20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '8px' }}>Webhooks</h1>
          <p style={{ color: '#666' }}>Create and test webhook endpoints</p>
        </div>
        <button 
          onClick={handleCreateWebhook}
          style={{
            backgroundColor: '#3182ce',
            color: 'white',
            padding: '8px 16px',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Create Webhook
        </button>
      </div>

      {webhooks.length === 0 ? (
        <div style={{
          backgroundColor: 'white',
          padding: '48px',
          textAlign: 'center',
          borderRadius: '8px',
          border: '1px solid #e2e8f0'
        }}>
          <p style={{ fontSize: '18px', color: '#666', marginBottom: '16px' }}>
            No webhooks created yet
          </p>
          <p style={{ color: '#999', marginBottom: '24px' }}>
            Create your first webhook to start receiving events
          </p>
          <button 
            onClick={handleCreateWebhook}
            style={{
              backgroundColor: '#3182ce',
              color: 'white',
              padding: '8px 16px',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            Create your first webhook
          </button>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(400px, 1fr))', gap: '24px' }}>
          {webhooks.map(webhook => (
            <div key={webhook.id} style={{
              backgroundColor: 'white',
              borderRadius: '8px',
              border: '1px solid #e2e8f0',
              overflow: 'hidden'
            }}>
              <div style={{ padding: '16px', borderBottom: '1px solid #e2e8f0' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <h3 style={{ fontSize: '18px', fontWeight: 'bold' }}>{webhook.name}</h3>
                  <span style={{
                    backgroundColor: webhook.isActive ? '#38a169' : '#e53e3e',
                    color: 'white',
                    padding: '4px 8px',
                    borderRadius: '4px',
                    fontSize: '12px'
                  }}>
                    {webhook.isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>
              </div>
              <div style={{ padding: '16px' }}>
                <p style={{ color: '#666', marginBottom: '8px', fontSize: '14px' }}>
                  {webhook.url}
                </p>
                <p style={{ color: '#999', marginBottom: '16px', fontSize: '12px' }}>
                  Events: {webhook.events.join(', ')}
                </p>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    onClick={() => handleTestWebhook(webhook)}
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

      {webhookEvents.length > 0 && (
        <div style={{ marginTop: '32px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '16px' }}>Recent Webhook Events</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
            {webhookEvents.slice(0, 6).map(event => (
              <div key={event.id} style={{
                backgroundColor: 'white',
                padding: '16px',
                borderRadius: '8px',
                border: '1px solid #e2e8f0'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <span style={{
                    backgroundColor: event.status === 'sent' ? '#38a169' : '#e53e3e',
                    color: 'white',
                    padding: '4px 8px',
                    borderRadius: '4px',
                    fontSize: '12px'
                  }}>
                    {event.status}
                  </span>
                  {event.responseTime && (
                    <span style={{ fontSize: '14px', color: '#666' }}>
                      {event.responseTime}ms
                    </span>
                  )}
                </div>
                <p style={{ fontSize: '14px', color: '#666', marginBottom: '4px' }}>
                  Event: {event.eventType}
                </p>
                {event.responseCode && (
                  <p style={{ fontSize: '14px', color: '#666', marginBottom: '4px' }}>
                    Status: {event.responseCode}
                  </p>
                )}
                {event.error && (
                  <p style={{ fontSize: '14px', color: '#e53e3e', marginTop: '8px' }}>
                    {event.error}
                  </p>
                )}
                <p style={{ fontSize: '12px', color: '#999', marginTop: '8px' }}>
                  {event.timestamp.toLocaleString()}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Webhook"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
              Name *
            </label>
            <input
              type="text"
              value={newWebhook.name}
              onChange={(e) => setNewWebhook(prev => ({ ...prev, name: e.target.value }))}
              placeholder="My Webhook"
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
              value={newWebhook.url}
              onChange={(e) => setNewWebhook(prev => ({ ...prev, url: e.target.value }))}
              placeholder="https://your-server.com/webhook"
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
              Events
            </label>
            <input
              type="text"
              value={newWebhook.events}
              onChange={(e) => setNewWebhook(prev => ({ ...prev, events: e.target.value }))}
              placeholder="user.created, order.completed, payment.failed"
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
              Secret (optional)
            </label>
            <input
              type="password"
              value={newWebhook.secret}
              onChange={(e) => setNewWebhook(prev => ({ ...prev, secret: e.target.value }))}
              placeholder="webhook-secret-key"
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

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <input
              type="checkbox"
              id="isActive"
              checked={newWebhook.isActive}
              onChange={(e) => setNewWebhook(prev => ({ ...prev, isActive: e.target.checked }))}
              style={{ margin: 0 }}
            />
            <label htmlFor="isActive" style={{ fontSize: '14px', cursor: 'pointer' }}>
              Active
            </label>
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
              onClick={handleSubmitWebhook}
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
              Create Webhook
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
} 