import { useState, useEffect } from 'react';
import { Plus, Trash2, Play, Eye, ChevronDown, ChevronRight, Calendar, Clock, X, AlertCircle, CheckCircle2, Edit } from 'lucide-react';
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


export default function ApiEndpoints() {
  const [endpoints, setEndpoints] = useState<ApiEndpoint[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingEndpointId, setEditingEndpointId] = useState<string | null>(null);
  const [selectedEndpoint, setSelectedEndpoint] = useState<ApiEndpoint | null>(null);
  const [expandedEndpoints, setExpandedEndpoints] = useState<Set<string>>(new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [isTesting, setIsTesting] = useState<string | null>(null);
  const [apiKeys, setApiKeys] = useState<Array<{ id: number; name: string; keyValue: string }>>([]);
  const [selectedApiKeyId, setSelectedApiKeyId] = useState<string>('');
  const [apiKeyHeaderName, setApiKeyHeaderName] = useState('Authorization');
  const [newEndpoint, setNewEndpoint] = useState({
    name: '',
    url: '',
    method: 'GET' as const,
    headers: '',
    body: '',
    recurringEnabled: false,
    recurringInterval: '' as '' | '30s' | '5m' | '1h' | '24h'
  });
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [dialogError, setDialogError] = useState<string | null>(null);

  // Load endpoints and API keys on component mount
  useEffect(() => {
    loadEndpoints();
    loadApiKeys();
  }, []);

  const loadApiKeys = async () => {
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/api-keys`, {
        headers: getAuthHeaders(),
        credentials: 'include'
      });
      
      if (response.ok) {
        const data = await response.json();
        setApiKeys(data);
      }
    } catch (error) {
      console.error('Error loading API keys:', error);
    }
  };

  const handleApiKeyChange = (apiKeyId: string) => {
    setSelectedApiKeyId(apiKeyId);
    
    if (apiKeyId && apiKeyId !== '') {
      const selectedKey = apiKeys.find(k => k.id.toString() === apiKeyId);
      if (selectedKey) {
        // Determine the header format based on header name
        let headerValue = selectedKey.keyValue;
        if (apiKeyHeaderName === 'Authorization') {
          // Check if it already has "Bearer" or "Basic" prefix
          if (!headerValue.startsWith('Bearer ') && !headerValue.startsWith('Basic ')) {
            headerValue = `Bearer ${selectedKey.keyValue}`;
          }
        }
        
        // Add or update the header
        const currentHeaders = newEndpoint.headers.split('\n').filter(h => h.trim());
        const headerLine = `${apiKeyHeaderName}: ${headerValue}`;
        
        // Remove existing header with same name if exists
        const filteredHeaders = currentHeaders.filter(h => {
          const [key] = h.split(':');
          return key && key.trim() !== apiKeyHeaderName;
        });
        
        // Add new header
        filteredHeaders.push(headerLine);
        setNewEndpoint(prev => ({ ...prev, headers: filteredHeaders.join('\n') }));
      }
    } else {
      // Remove the header if API key is deselected
      const currentHeaders = newEndpoint.headers.split('\n').filter(h => {
        const [key] = h.split(':');
        return key && key.trim() !== apiKeyHeaderName;
      });
      setNewEndpoint(prev => ({ ...prev, headers: currentHeaders.join('\n') }));
    }
  };

  const loadEndpoints = async () => {
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/endpoints`, {
        headers: getAuthHeaders(),
        credentials: 'include'
      });
      
      if (response.ok) {
        const data = await response.json();
        const endpoints = data.map((endpoint: any) => {
          // Parse headers from string format
          const headers: Record<string, string> = {};
          if (endpoint.headers) {
            endpoint.headers.split('\n').forEach((line: string) => {
              const [key, ...valueParts] = line.split(':');
              if (key && valueParts.length > 0) {
                headers[key.trim()] = valueParts.join(':').trim();
              }
            });
          }
          
          return {
            ...endpoint,
            id: endpoint.id?.toString() || endpoint.uuid_id?.toString() || String(endpoint.id),
            headers,
            recurringEnabled: endpoint.recurringEnabled || false,
            recurringInterval: (endpoint.recurringInterval && ['30s', '5m', '1h', '24h'].includes(endpoint.recurringInterval)) 
              ? endpoint.recurringInterval as '30s' | '5m' | '1h' | '24h'
              : '',
            createdAt: endpoint.createdAt ? new Date(endpoint.createdAt) : new Date(),
            updatedAt: endpoint.updatedAt ? new Date(endpoint.updatedAt) : new Date()
          };
        });
        setEndpoints(endpoints);
      } else {
        console.error('Failed to load endpoints:', response.status, response.statusText);
        setEndpoints([]);
      }
    } catch (error) {
      console.error('Error loading endpoints:', error);
      setEndpoints([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateEndpoint = () => {
    setEditingEndpointId(null);
    setDialogError(null);
    setErrorMessage(null);
    setSuccessMessage(null);
    setNewEndpoint({ name: '', url: '', method: 'GET', headers: '', body: '', recurringEnabled: false, recurringInterval: '' });
    setSelectedApiKeyId('');
    setApiKeyHeaderName('Authorization');
    setIsModalOpen(true);
  };

  const handleEditEndpoint = (endpoint: ApiEndpoint) => {
    setEditingEndpointId(endpoint.id);
    setDialogError(null);
    setErrorMessage(null);
    setSuccessMessage(null);
    
    // Convert headers object to string format
    const headersStr = Object.entries(endpoint.headers || {})
      .map(([key, value]) => `${key}: ${value}`)
      .join('\n');
    
    setNewEndpoint({
      name: endpoint.name,
      url: endpoint.url,
      method: endpoint.method as any,
      headers: headersStr,
      body: endpoint.body || '',
      recurringEnabled: endpoint.recurringEnabled || false,
      recurringInterval: (endpoint.recurringInterval && ['30s', '5m', '1h', '24h'].includes(endpoint.recurringInterval)) 
        ? endpoint.recurringInterval as '30s' | '5m' | '1h' | '24h'
        : ''
    });
    setSelectedApiKeyId('');
    setApiKeyHeaderName('Authorization');
    setIsModalOpen(true);
  };

  const handleSubmitEndpoint = async () => {
    if (!newEndpoint.name || !newEndpoint.url) {
      setDialogError('Please fill in all required fields (Name and URL)');
      setErrorMessage(null);
      setSuccessMessage(null);
      return;
    }
    
    setDialogError(null);
    setErrorMessage(null);
    setSuccessMessage(null);

    const isEditing = editingEndpointId !== null;

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
      const url = isEditing 
        ? `${backendUrl}/api/endpoints/${editingEndpointId}`
        : `${backendUrl}/api/endpoints`;
      
      const response = await fetch(url, {
        method: isEditing ? 'PUT' : 'POST',
        headers: getAuthHeaders(),
        credentials: 'include',
        body: JSON.stringify({
          name: newEndpoint.name,
          url: newEndpoint.url,
          method: newEndpoint.method,
          headers,
          body: newEndpoint.body || undefined,
          recurringEnabled: newEndpoint.recurringEnabled,
          recurringInterval: newEndpoint.recurringInterval || null
        })
      });

      if (response.ok) {
        const data = await response.json();
        // Parse headers from string format
        const parsedHeaders: Record<string, string> = {};
        if (data.headers) {
          data.headers.split('\n').forEach((line: string) => {
            const [key, ...valueParts] = line.split(':');
            if (key && valueParts.length > 0) {
              parsedHeaders[key.trim()] = valueParts.join(':').trim();
            }
          });
        }
        
        // Don't add to list here - let loadEndpoints() handle it to avoid duplication
        // Just reload to ensure consistency
        setNewEndpoint({ name: '', url: '', method: 'GET', headers: '', body: '', recurringEnabled: false, recurringInterval: '' });
        setSelectedApiKeyId('');
        setApiKeyHeaderName('Authorization');
        setIsModalOpen(false);
        setEditingEndpointId(null);
        setSuccessMessage(isEditing ? 'Endpoint updated successfully!' : 'Endpoint created successfully!');
        setDialogError(null);
        setErrorMessage(null);
        // Reload endpoints to show updated data
        loadEndpoints();
        // Clear success message after 5 seconds
        setTimeout(() => setSuccessMessage(null), 5000);
      } else {
        let errorMsg = `Failed to ${isEditing ? 'update' : 'create'} endpoint (${response.status} ${response.statusText})`;
        try {
          const errorData = await response.json();
          if (errorData.error) {
            errorMsg = errorData.error;
            if (errorData.details) {
              errorMsg += ` - ${errorData.details}`;
            }
          } else {
            errorMsg += ` - ${JSON.stringify(errorData)}`;
          }
        } catch {
          const errorText = await response.text();
          if (errorText) {
            errorMsg += ` - ${errorText}`;
          }
        }
        setDialogError(errorMsg);
        setErrorMessage(null);
        setSuccessMessage(null);
      }
    } catch (error) {
      console.error(`Error ${isEditing ? 'updating' : 'creating'} endpoint:`, error);
      setDialogError(`Failed to ${isEditing ? 'update' : 'create'} endpoint. Please try again.`);
      setErrorMessage(null);
      setSuccessMessage(null);
    }
  };

  const handleTestEndpoint = async (endpoint: ApiEndpoint) => {
    setIsTesting(endpoint.id);
    setErrorMessage(null);
    setSuccessMessage(null);
    
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/endpoints/${endpoint.id}/test`, {
        method: 'POST',
        headers: getAuthHeaders(),
        credentials: 'include'
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setSuccessMessage('Test completed successfully! View results in Analytics.');
          setTimeout(() => setSuccessMessage(null), 5000);
        } else {
          setSuccessMessage('Test completed. View results in Analytics.');
          setTimeout(() => setSuccessMessage(null), 5000);
        }
        // Reload endpoints to show updated data
        loadEndpoints();
      } else {
        let errorMsg = `Test failed (${response.status} ${response.statusText})`;
        try {
          const errorData = await response.json();
          if (errorData.error) {
            errorMsg = errorData.error;
            if (errorData.details) {
              errorMsg += ` - ${errorData.details}`;
            }
          } else {
            errorMsg += ` - ${JSON.stringify(errorData)}`;
          }
        } catch {
          const errorText = await response.text();
          if (errorText) {
            errorMsg += ` - ${errorText}`;
          }
        }
        setErrorMessage(errorMsg);
      }
    } catch (error) {
      console.error('Error testing endpoint:', error);
      setErrorMessage('Failed to test endpoint. Please try again.');
    } finally {
      setIsTesting(null);
    }
  };

  const handleDeleteEndpoint = async (endpointId: string) => {
    if (!confirm('Are you sure you want to delete this endpoint?')) {
      return;
    }

    setErrorMessage(null);
    setSuccessMessage(null);

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
        setSuccessMessage('Endpoint deleted successfully');
        setTimeout(() => setSuccessMessage(null), 5000);
      } else {
        const errorText = await response.text();
        setErrorMessage(`Failed to delete endpoint: ${response.status} ${response.statusText}${errorText ? ` - ${errorText}` : ''}`);
      }
    } catch (error) {
      console.error('Error deleting endpoint:', error);
      setErrorMessage('Failed to delete endpoint. Please try again.');
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

      {/* Error Message */}
      {errorMessage && (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="p-4">
            <div className="flex items-start gap-3">
              <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="text-sm font-medium text-red-900">Error</p>
                <p className="text-sm text-red-700 mt-1">{errorMessage}</p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setErrorMessage(null)}
                className="h-6 w-6 p-0 text-red-600 hover:text-red-700 hover:bg-red-100"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Success Message */}
      {successMessage && (
        <Card className="border-green-200 bg-green-50">
          <CardContent className="p-4">
            <div className="flex items-start gap-3">
              <CheckCircle2 className="h-5 w-5 text-green-600 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="text-sm font-medium text-green-900">Success</p>
                <p className="text-sm text-green-700 mt-1">{successMessage}</p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setSuccessMessage(null)}
                className="h-6 w-6 p-0 text-green-600 hover:text-green-700 hover:bg-green-100"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          </CardContent>
        </Card>
      )}


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
                            handleEditEndpoint(endpoint);
                          }}
                          className="h-8 w-8 p-0"
                          title="Edit Endpoint"
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
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
                                handleEditEndpoint(endpoint);
                              }}
                              className="gap-2"
                            >
                              <Edit className="h-4 w-4" />
                              Edit
                            </Button>
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
      <Dialog open={isModalOpen} onOpenChange={(open) => {
        setIsModalOpen(open);
        if (!open) {
          // Reset form when closing
          setEditingEndpointId(null);
          setSelectedApiKeyId('');
          setApiKeyHeaderName('Authorization');
          setNewEndpoint({ name: '', url: '', method: 'GET', headers: '', body: '', recurringEnabled: false, recurringInterval: '' });
          setDialogError(null);
          setErrorMessage(null);
          setSuccessMessage(null);
        }
      }}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingEndpointId ? 'Edit API Endpoint' : 'Create API Endpoint'}</DialogTitle>
            <DialogDescription>
              {editingEndpointId ? 'Update your API endpoint details' : 'Add a new API endpoint to monitor and test'}
            </DialogDescription>
          </DialogHeader>
          {dialogError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-md flex items-start gap-2">
              <AlertCircle className="h-4 w-4 text-red-600 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-red-700">{dialogError}</p>
            </div>
          )}
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

            {/* API Key Selection */}
            {apiKeys.length > 0 && (
              <div className="space-y-2 p-4 bg-blue-50 rounded-lg border border-blue-200">
                <Label className="text-sm font-semibold text-slate-900">Quick Add API Key (Optional)</Label>
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-2">
                    <Label htmlFor="api-key-select" className="text-xs">Select API Key</Label>
                    <Select
                      id="api-key-select"
                      value={selectedApiKeyId}
                      onChange={(e) => handleApiKeyChange(e.target.value)}
                    >
                      <option value="">None</option>
                      {apiKeys.map((key) => (
                        <option key={key.id} value={key.id.toString()}>
                          {key.name}
                        </option>
                      ))}
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="api-key-header" className="text-xs">Header Name</Label>
                    <Select
                      id="api-key-header"
                      value={apiKeyHeaderName}
                      onChange={(e) => {
                        const newHeaderName = e.target.value;
                        const oldHeaderName = apiKeyHeaderName;
                        setApiKeyHeaderName(newHeaderName);
                        // If an API key is selected, update the header with new name
                        if (selectedApiKeyId && selectedApiKeyId !== '') {
                          const selectedKey = apiKeys.find(k => k.id.toString() === selectedApiKeyId);
                          if (selectedKey) {
                            // Remove old header
                            const currentHeaders = newEndpoint.headers.split('\n').filter(h => {
                              const [key] = h.split(':');
                              return key && key.trim() !== oldHeaderName;
                            });
                            
                            // Add new header with updated name
                            let headerValue = selectedKey.keyValue;
                            if (newHeaderName === 'Authorization') {
                              if (!headerValue.startsWith('Bearer ') && !headerValue.startsWith('Basic ')) {
                                headerValue = `Bearer ${selectedKey.keyValue}`;
                              }
                            }
                            currentHeaders.push(`${newHeaderName}: ${headerValue}`);
                            setNewEndpoint(prev => ({ ...prev, headers: currentHeaders.join('\n') }));
                          }
                        }
                      }}
                    >
                      <option value="Authorization">Authorization</option>
                      <option value="X-API-Key">X-API-Key</option>
                      <option value="X-Auth-Token">X-Auth-Token</option>
                      <option value="Api-Key">Api-Key</option>
                      <option value="X-Api-Key">X-Api-Key</option>
                    </Select>
                  </div>
                </div>
                <p className="text-xs text-slate-600 mt-2">
                  Select an API key to automatically add it to the headers below
                </p>
              </div>
            )}

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

            <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
              <Label htmlFor="recurringInterval" className="text-sm font-medium text-slate-900 block mb-2">
                Automatic/Recurring Calls
              </Label>
              <select
                id="recurringInterval"
                value={newEndpoint.recurringInterval || ''}
                onChange={(e) => {
                  const interval = e.target.value as '' | '30s' | '5m' | '1h' | '24h';
                  setNewEndpoint(prev => ({ 
                    ...prev, 
                    recurringEnabled: interval !== '',
                    recurringInterval: interval
                  }));
                }}
                className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
              >
                <option value="">Disabled</option>
                <option value="30s">Every 30 seconds</option>
                <option value="5m">Every 5 minutes</option>
                <option value="1h">Every hour</option>
                <option value="24h">Once per day (24 hours)</option>
              </select>
              <p className="text-xs text-slate-600 mt-2">
                Select an interval to automatically test this endpoint
              </p>
            </div>

            <div className="flex justify-end gap-3 pt-4">
              <Button variant="outline" onClick={() => setIsModalOpen(false)}>
                Cancel
              </Button>
              <Button onClick={handleSubmitEndpoint}>
                {editingEndpointId ? 'Update Endpoint' : 'Create Endpoint'}
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
