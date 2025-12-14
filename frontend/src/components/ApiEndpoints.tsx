import { useState, useEffect } from 'react';
import { Plus, Trash2, Play, Eye, ChevronDown, ChevronRight, Calendar, Clock } from 'lucide-react';
import type { ApiEndpoint } from '../types';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from './ui/dialog';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Select } from './ui/select';
import { Textarea } from './ui/textarea';
import EndpointDetail from './EndpointDetail';
import { getAuthHeaders } from '../utils/auth';

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
  const [expandedEndpoints, setExpandedEndpoints] = useState<Set<string>>(new Set());
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
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/endpoints`, {
        headers: getAuthHeaders(),
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
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/endpoints`, {
        method: 'POST',
        headers: getAuthHeaders(),
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
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/endpoints/${endpoint.id}/test`, {
        method: 'POST',
        headers: getAuthHeaders(),
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
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/endpoints/${endpointId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
        credentials: 'include'
      });

      if (response.ok || response.status === 404) {
        setEndpoints(prev => prev.filter(e => e.id !== endpointId));
        setExpandedEndpoints(prev => {
          const newSet = new Set(prev);
          newSet.delete(endpointId);
          return newSet;
        });
      } else {
        alert('Failed to delete endpoint');
      }
    } catch (error) {
      console.error('Error deleting endpoint:', error);
      // Remove from local state even if API fails
      setEndpoints(prev => prev.filter(e => e.id !== endpointId));
      setExpandedEndpoints(prev => {
        const newSet = new Set(prev);
        newSet.delete(endpointId);
        return newSet;
      });
    }
  };

  const toggleExpand = (endpointId: string) => {
    setExpandedEndpoints(prev => {
      const newSet = new Set(prev);
      if (newSet.has(endpointId)) {
        newSet.delete(endpointId);
      } else {
        newSet.add(endpointId);
      }
      return newSet;
    });
  };

  const getMethodColor = (method: string) => {
    const colors: Record<string, { bg: string; text: string }> = {
      GET: { bg: 'bg-blue-100', text: 'text-blue-700' },
      POST: { bg: 'bg-green-100', text: 'text-green-700' },
      PUT: { bg: 'bg-yellow-100', text: 'text-yellow-700' },
      DELETE: { bg: 'bg-red-100', text: 'text-red-700' },
      PATCH: { bg: 'bg-purple-100', text: 'text-purple-700' },
    };
    return colors[method] || { bg: 'bg-gray-100', text: 'text-gray-700' };
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
          <h1 className="text-3xl font-bold text-slate-900 mb-2">My Endpoints</h1>
          <p className="text-slate-600">Create, manage, and monitor your API endpoints</p>
        </div>
        <Button onClick={handleCreateEndpoint} className="gap-2">
          <Plus className="h-4 w-4" />
          Create Endpoint
        </Button>
      </div>

      {/* Endpoints Table */}
      {endpoints.length === 0 ? (
        <Card className="border-2 border-dashed border-slate-300">
          <CardContent className="flex flex-col items-center justify-center py-20">
            <div className="rounded-full bg-gradient-to-br from-blue-100 to-purple-100 p-6 mb-6">
              <Plus className="h-10 w-10 text-blue-600" />
            </div>
            <h3 className="text-xl font-semibold text-slate-900 mb-2">
              No endpoints created yet
            </h3>
            <p className="text-sm text-slate-600 mb-8 text-center max-w-md">
              Create your first API endpoint to start testing and monitoring your APIs with real-time analytics
            </p>
            <Button onClick={handleCreateEndpoint} className="gap-2" size="lg">
              <Plus className="h-5 w-5" />
              Create Your First Endpoint
            </Button>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="p-0">
            <div className="divide-y divide-slate-200">
              {endpoints.map((endpoint) => {
                const isExpanded = expandedEndpoints.has(endpoint.id);
                const methodStyle = getMethodColor(endpoint.method);
                
                return (
                  <div key={endpoint.id} className="transition-colors hover:bg-slate-50/50">
                    {/* Main Row */}
                    <div
                      className="flex items-center gap-4 p-4 cursor-pointer"
                      onClick={() => toggleExpand(endpoint.id)}
                    >
                      {/* Expand Icon */}
                      <div className="flex-shrink-0">
                        {isExpanded ? (
                          <ChevronDown className="h-5 w-5 text-slate-400" />
                        ) : (
                          <ChevronRight className="h-5 w-5 text-slate-400" />
                        )}
                      </div>

                      {/* Method Badge */}
                      <div className="flex-shrink-0">
                        <Badge className={`${methodStyle.bg} ${methodStyle.text} border-0 font-semibold px-2.5 py-0.5`}>
                          {endpoint.method}
                        </Badge>
                      </div>

                      {/* Endpoint Name */}
                      <div className="flex-1 min-w-0">
                        <h3 className="font-semibold text-slate-900 truncate">{endpoint.name}</h3>
                        <p className="text-sm text-slate-500 font-mono truncate mt-0.5">{endpoint.url}</p>
                      </div>

                      {/* Metadata */}
                      <div className="hidden md:flex items-center gap-4 text-sm text-slate-500">
                        <div className="flex items-center gap-1.5">
                          <Calendar className="h-4 w-4" />
                          <span>{endpoint.createdAt.toLocaleDateString()}</span>
                        </div>
                      </div>

                      {/* Actions */}
                      <div className="flex items-center gap-2 flex-shrink-0">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedEndpoint(endpoint);
                          }}
                          className="h-8 w-8 p-0"
                          title="View Details"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleTestEndpoint(endpoint);
                          }}
                          disabled={isTesting === endpoint.id}
                          className="h-8 w-8 p-0"
                          title="Test Endpoint"
                        >
                          <Play className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDeleteEndpoint(endpoint.id);
                          }}
                          className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
                          title="Delete Endpoint"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>

                    {/* Expanded Content */}
                    {isExpanded && (
                      <div className="px-4 pb-4 pt-0 bg-slate-50/50 border-t border-slate-100">
                        <div className="pl-9 space-y-4 pt-4">
                          {/* URL */}
                          <div>
                            <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
                              URL
                            </label>
                            <div className="bg-white border border-slate-200 rounded-md p-3 font-mono text-sm text-slate-900">
                              {endpoint.url}
                            </div>
                          </div>

                          {/* Headers */}
                          {Object.keys(endpoint.headers).length > 0 && (
                            <div>
                              <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
                                Headers
                              </label>
                              <div className="bg-white border border-slate-200 rounded-md p-3 space-y-1">
                                {Object.entries(endpoint.headers).map(([key, value]) => (
                                  <div key={key} className="text-sm font-mono">
                                    <span className="text-slate-600">{key}:</span>{' '}
                                    <span className="text-slate-900">{value}</span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Request Body */}
                          {endpoint.body && (
                            <div>
                              <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
                                Request Body
                              </label>
                              <pre className="bg-white border border-slate-200 rounded-md p-3 text-xs overflow-x-auto">
                                {endpoint.body}
                              </pre>
                            </div>
                          )}

                          {/* Metadata */}
                          <div className="grid grid-cols-2 gap-4 pt-2">
                            <div>
                              <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
                                Created
                              </label>
                              <div className="flex items-center gap-2 text-sm text-slate-700">
                                <Calendar className="h-4 w-4 text-slate-400" />
                                <span>{endpoint.createdAt.toLocaleString()}</span>
                              </div>
                            </div>
                            <div>
                              <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5 block">
                                Last Updated
                              </label>
                              <div className="flex items-center gap-2 text-sm text-slate-700">
                                <Clock className="h-4 w-4 text-slate-400" />
                                <span>{endpoint.updatedAt.toLocaleString()}</span>
                              </div>
                            </div>
                          </div>

                          {/* Action Buttons */}
                          <div className="flex gap-2 pt-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={(e) => {
                                e.stopPropagation();
                                setSelectedEndpoint(endpoint);
                              }}
                              className="gap-2"
                            >
                              <Eye className="h-4 w-4" />
                              View Analytics
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleTestEndpoint(endpoint);
                              }}
                              disabled={isTesting === endpoint.id}
                              className="gap-2"
                            >
                              <Play className="h-4 w-4" />
                              {isTesting === endpoint.id ? 'Testing...' : 'Test Now'}
                            </Button>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>
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
