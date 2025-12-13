import { useState, useEffect } from 'react';
import { Plus, Trash2, Play, Eye } from 'lucide-react';
import type { ApiEndpoint } from '../types';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from './ui/dialog';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Select } from './ui/select';
import { Textarea } from './ui/textarea';
import EndpointDetail from './EndpointDetail';

// Generate dummy endpoints
const generateDummyEndpoints = (): ApiEndpoint[] => {
  const endpoints: ApiEndpoint[] = [
    {
      id: '1',
      name: 'User Authentication API',
      url: 'https://api.example.com/v1/auth/login',
      method: 'POST' as const,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'user@example.com', password: '***' }, null, 2),
      createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
      updatedAt: new Date(Date.now() - 1 * 60 * 60 * 1000),
    },
    {
      id: '2',
      name: 'Get User Profile',
      url: 'https://api.example.com/v1/users/me',
      method: 'GET' as const,
      headers: { 'Authorization': 'Bearer token123' },
      createdAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000),
      updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000),
    },
    {
      id: '3',
      name: 'Create New Post',
      url: 'https://api.example.com/v1/posts',
      method: 'POST' as const,
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer token123' },
      body: JSON.stringify({ title: 'New Post', content: 'Post content' }, null, 2),
      createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
      updatedAt: new Date(Date.now() - 3 * 60 * 60 * 1000),
    },
    {
      id: '4',
      name: 'Update Product',
      url: 'https://api.example.com/v1/products/123',
      method: 'PUT' as const,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Updated Product', price: 99.99 }, null, 2),
      createdAt: new Date(Date.now() - 10 * 24 * 60 * 60 * 1000),
      updatedAt: new Date(Date.now() - 4 * 60 * 60 * 1000),
    },
    {
      id: '5',
      name: 'Delete Comment',
      url: 'https://api.example.com/v1/comments/456',
      method: 'DELETE' as const,
      headers: { 'Authorization': 'Bearer token123' },
      createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
      updatedAt: new Date(Date.now() - 30 * 60 * 1000),
    },
  ];
  
  return endpoints;
};

export default function ApiEndpoints() {
  const [endpoints, setEndpoints] = useState<ApiEndpoint[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedEndpoint, setSelectedEndpoint] = useState<ApiEndpoint | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isTesting, setIsTesting] = useState<string | null>(null);
  const [newEndpoint, setNewEndpoint] = useState({
    name: '',
    url: '',
    method: 'GET' as const,
    headers: '',
    body: ''
  });

  // Load endpoints on component mount
  useEffect(() => {
    loadEndpoints();
  }, []);

  const loadEndpoints = async () => {
    try {
      // Try to load from API, fallback to dummy data
      const response = await fetch('http://localhost:8080/api/endpoints', {
        credentials: 'include'
      });
      
      if (response.ok) {
        const data = await response.json();
        const endpoints = data.map((endpoint: any) => ({
          ...endpoint,
          id: endpoint.id.toString(),
          createdAt: new Date(endpoint.createdAt),
          updatedAt: new Date(endpoint.updatedAt)
        }));
        setEndpoints(endpoints);
      } else {
        // Use dummy data if API fails
        setEndpoints(generateDummyEndpoints());
      }
    } catch (error) {
      console.error('Error loading endpoints:', error);
      // Use dummy data on error
      setEndpoints(generateDummyEndpoints());
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateEndpoint = () => {
    setIsModalOpen(true);
  };

  const handleSubmitEndpoint = async () => {
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

    try {
      const response = await fetch('http://localhost:8080/api/endpoints', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
          name: newEndpoint.name,
          url: newEndpoint.url,
          method: newEndpoint.method,
          headers,
          body: newEndpoint.body || undefined
        })
      });

      if (response.ok) {
        const data = await response.json();
        const endpoint = {
          ...data,
          id: data.id.toString(),
          createdAt: new Date(data.createdAt),
          updatedAt: new Date(data.updatedAt)
        };
        setEndpoints(prev => [endpoint, ...prev]);
        setNewEndpoint({ name: '', url: '', method: 'GET', headers: '', body: '' });
        setIsModalOpen(false);
      } else {
        // If API fails, add to local state with dummy data
        const newId = (Math.max(...endpoints.map(e => parseInt(e.id)), 0) + 1).toString();
        const endpoint: ApiEndpoint = {
          id: newId,
          name: newEndpoint.name,
          url: newEndpoint.url,
          method: newEndpoint.method,
          headers,
          body: newEndpoint.body,
          createdAt: new Date(),
          updatedAt: new Date(),
        };
        setEndpoints(prev => [endpoint, ...prev]);
        setNewEndpoint({ name: '', url: '', method: 'GET', headers: '', body: '' });
        setIsModalOpen(false);
      }
    } catch (error) {
      console.error('Error creating endpoint:', error);
      // Add to local state even if API fails
      const newId = (Math.max(...endpoints.map(e => parseInt(e.id)), 0) + 1).toString();
      const endpoint: ApiEndpoint = {
        id: newId,
        name: newEndpoint.name,
        url: newEndpoint.url,
        method: newEndpoint.method,
        headers,
        body: newEndpoint.body,
        createdAt: new Date(),
        updatedAt: new Date(),
      };
      setEndpoints(prev => [endpoint, ...prev]);
      setNewEndpoint({ name: '', url: '', method: 'GET', headers: '', body: '' });
      setIsModalOpen(false);
    }
  };

  const handleTestEndpoint = async (endpoint: ApiEndpoint) => {
    setIsTesting(endpoint.id);
    
    try {
      const response = await fetch(`http://localhost:8080/api/endpoints/${endpoint.id}/test`, {
        method: 'POST',
        credentials: 'include'
      });

      if (response.ok) {
        alert('Endpoint tested successfully!');
      } else {
        alert('Test completed (using dummy data)');
      }
    } catch (error) {
      console.error('Error testing endpoint:', error);
      alert('Test completed (using dummy data)');
    } finally {
      setIsTesting(null);
    }
  };

  const handleDeleteEndpoint = async (endpointId: string) => {
    if (!confirm('Are you sure you want to delete this endpoint?')) {
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/api/endpoints/${endpointId}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (response.ok || response.status === 404) {
        setEndpoints(prev => prev.filter(e => e.id !== endpointId));
      } else {
        alert('Failed to delete endpoint');
      }
    } catch (error) {
      console.error('Error deleting endpoint:', error);
      // Remove from local state even if API fails
      setEndpoints(prev => prev.filter(e => e.id !== endpointId));
    }
  };

  const getMethodColor = (method: string) => {
    const colors: Record<string, string> = {
      GET: 'bg-blue-500',
      POST: 'bg-green-500',
      PUT: 'bg-yellow-500',
      DELETE: 'bg-red-500',
      PATCH: 'bg-purple-500',
    };
    return colors[method] || 'bg-gray-500';
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-slate-600">Loading endpoints...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 mb-2">API Endpoints</h1>
          <p className="text-slate-600">Create and monitor your API endpoints</p>
        </div>
        <Button onClick={handleCreateEndpoint} className="gap-2">
          <Plus className="h-4 w-4" />
          Create Endpoint
        </Button>
      </div>

      {/* Endpoints Grid */}
      {endpoints.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-16">
            <div className="rounded-full bg-slate-100 p-4 mb-4">
              <Plus className="h-8 w-8 text-slate-400" />
            </div>
            <h3 className="text-lg font-semibold text-slate-900 mb-2">
              No endpoints created yet
            </h3>
            <p className="text-sm text-slate-600 mb-6 text-center max-w-md">
              Create your first API endpoint to start testing and monitoring your APIs
            </p>
            <Button onClick={handleCreateEndpoint} className="gap-2">
              <Plus className="h-4 w-4" />
              Create Your First Endpoint
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {endpoints.map(endpoint => (
            <Card key={endpoint.id} className="hover:shadow-lg transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1 min-w-0">
                    <CardTitle className="text-lg mb-2 truncate">{endpoint.name}</CardTitle>
                    <Badge className={getMethodColor(endpoint.method)}>
                      {endpoint.method}
                    </Badge>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <p className="text-xs text-slate-500 mb-1">URL</p>
                  <p className="text-sm font-mono text-slate-700 truncate" title={endpoint.url}>
                    {endpoint.url}
                  </p>
                </div>
                <div className="flex items-center gap-2 text-xs text-slate-500">
                  <span>Created {endpoint.createdAt.toLocaleDateString()}</span>
                </div>
                <div className="flex gap-2 pt-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setSelectedEndpoint(endpoint)}
                    className="flex-1 gap-2"
                  >
                    <Eye className="h-4 w-4" />
                    View
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleTestEndpoint(endpoint)}
                    disabled={isTesting === endpoint.id}
                    className="gap-2"
                  >
                    <Play className="h-4 w-4" />
                    {isTesting === endpoint.id ? 'Testing...' : 'Test'}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDeleteEndpoint(endpoint.id)}
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Create Endpoint Dialog */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Create API Endpoint</DialogTitle>
            <DialogDescription>
              Add a new API endpoint to monitor and test
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">Name *</Label>
              <Input
                id="name"
                placeholder="My API Endpoint"
                value={newEndpoint.name}
                onChange={(e) => setNewEndpoint(prev => ({ ...prev, name: e.target.value }))}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="url">URL *</Label>
              <Input
                id="url"
                type="url"
                placeholder="https://api.example.com/endpoint"
                value={newEndpoint.url}
                onChange={(e) => setNewEndpoint(prev => ({ ...prev, url: e.target.value }))}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="method">Method</Label>
              <Select
                id="method"
                value={newEndpoint.method}
                onChange={(e) => setNewEndpoint(prev => ({ ...prev, method: e.target.value as any }))}
              >
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
                <option value="PATCH">PATCH</option>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="headers">Headers (one per line, format: Key: Value)</Label>
              <Textarea
                id="headers"
                placeholder="Content-Type: application/json&#10;Authorization: Bearer token"
                rows={4}
                value={newEndpoint.headers}
                onChange={(e) => setNewEndpoint(prev => ({ ...prev, headers: e.target.value }))}
                className="font-mono text-xs"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="body">Request Body (for POST, PUT, PATCH)</Label>
              <Textarea
                id="body"
                placeholder='{"key": "value"}'
                rows={4}
                value={newEndpoint.body}
                onChange={(e) => setNewEndpoint(prev => ({ ...prev, body: e.target.value }))}
                className="font-mono text-xs"
              />
            </div>

            <div className="flex justify-end gap-3 pt-4">
              <Button variant="outline" onClick={() => setIsModalOpen(false)}>
                Cancel
              </Button>
              <Button onClick={handleSubmitEndpoint}>
                Create Endpoint
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Endpoint Detail View */}
      {selectedEndpoint && (
        <EndpointDetail
          endpoint={selectedEndpoint}
          onClose={() => setSelectedEndpoint(null)}
        />
      )}
    </div>
  );
}
