import { useState } from 'react';
import { X, Activity, Clock, CheckCircle2, XCircle, TrendingUp } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { ApiEndpoint } from '../types';

interface EndpointDetailProps {
  endpoint: ApiEndpoint;
  onClose: () => void;
}

// Generate dummy data for the last 24 hours
const generateDummyData = () => {
  const data = [];
  const now = new Date();
  
  for (let i = 23; i >= 0; i--) {
    const timestamp = new Date(now.getTime() - i * 60 * 60 * 1000);
    const hour = timestamp.getHours();
    
    // Simulate realistic patterns
    const baseResponseTime = 50 + Math.random() * 100;
    const successRate = 0.85 + Math.random() * 0.1;
    const requests = Math.floor(100 + Math.random() * 200);
    
    data.push({
      time: timestamp.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
      hour: hour,
      responseTime: Math.round(baseResponseTime),
      successRate: Math.round(successRate * 100),
      requests: requests,
      errors: Math.round(requests * (1 - successRate)),
      success: Math.round(requests * successRate),
    });
  }
  
  return data;
};

const generateStatusDistribution = () => {
  return [
    { status: '200', count: 850, color: '#10b981' },
    { status: '201', count: 120, color: '#10b981' },
    { status: '400', count: 45, color: '#f59e0b' },
    { status: '401', count: 30, color: '#f59e0b' },
    { status: '500', count: 15, color: '#ef4444' },
  ];
};

export default function EndpointDetail({ endpoint, onClose }: EndpointDetailProps) {
  const [timeRange, setTimeRange] = useState<'24h' | '7d' | '30d'>('24h');
  const chartData = generateDummyData();
  const statusData = generateStatusDistribution();
  
  // Calculate stats
  const avgResponseTime = Math.round(
    chartData.reduce((sum, d) => sum + d.responseTime, 0) / chartData.length
  );
  const totalRequests = chartData.reduce((sum, d) => sum + d.requests, 0);
  const totalErrors = chartData.reduce((sum, d) => sum + d.errors, 0);
  const successRate = Math.round(((totalRequests - totalErrors) / totalRequests) * 100);

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="fixed inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div className="relative min-h-screen flex items-center justify-center p-4">
        <div className="relative bg-white rounded-xl shadow-2xl w-full max-w-7xl max-h-[90vh] overflow-y-auto border border-slate-200">
          {/* Header */}
          <div className="sticky top-0 bg-gradient-to-r from-slate-50 to-white border-b border-slate-200 px-6 py-5 flex items-center justify-between z-10 backdrop-blur-sm">
            <div>
              <div className="flex items-center gap-3 mb-2">
                <h2 className="text-2xl font-bold text-slate-900">{endpoint.name}</h2>
                <Badge variant="default" className="text-xs">{endpoint.method}</Badge>
              </div>
              <p className="text-sm text-slate-600 font-mono bg-slate-50 px-3 py-1.5 rounded-md inline-block">{endpoint.url}</p>
            </div>
            <Button variant="ghost" size="icon" onClick={onClose}>
              <X className="h-5 w-5" />
            </Button>
          </div>

          {/* Content */}
          <div className="p-6 space-y-6">
            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Avg Response Time</CardTitle>
                  <Clock className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{avgResponseTime}ms</div>
                  <p className="text-xs text-muted-foreground">
                    <span className="text-green-600">-12%</span> from last hour
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Total Requests</CardTitle>
                  <Activity className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{totalRequests.toLocaleString()}</div>
                  <p className="text-xs text-muted-foreground">
                    Last 24 hours
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Success Rate</CardTitle>
                  <CheckCircle2 className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{successRate}%</div>
                  <p className="text-xs text-muted-foreground">
                    {totalRequests - totalErrors} successful
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Errors</CardTitle>
                  <XCircle className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-red-600">{totalErrors}</div>
                  <p className="text-xs text-muted-foreground">
                    {Math.round((totalErrors / totalRequests) * 100)}% error rate
                  </p>
                </CardContent>
              </Card>
            </div>

            {/* Time Range Selector */}
            <div className="flex gap-2">
              {(['24h', '7d', '30d'] as const).map((range) => (
                <Button
                  key={range}
                  variant={timeRange === range ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => setTimeRange(range)}
                >
                  {range}
                </Button>
              ))}
            </div>

            {/* Charts Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Response Time Chart */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <TrendingUp className="h-5 w-5" />
                    Response Time (ms)
                  </CardTitle>
                  <CardDescription>Average response time over time</CardDescription>
                </CardHeader>
                <CardContent>
                  <ResponsiveContainer width="100%" height={300}>
                    <AreaChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="time" />
                      <YAxis />
                      <Tooltip />
                      <Area
                        type="monotone"
                        dataKey="responseTime"
                        stroke="#3b82f6"
                        fill="#3b82f6"
                        fillOpacity={0.2}
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>

              {/* Request Volume Chart */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Activity className="h-5 w-5" />
                    Request Volume
                  </CardTitle>
                  <CardDescription>Number of requests per hour</CardDescription>
                </CardHeader>
                <CardContent>
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="time" />
                      <YAxis />
                      <Tooltip />
                      <Bar dataKey="requests" fill="#10b981" />
                    </BarChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>

              {/* Success Rate Chart */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <CheckCircle2 className="h-5 w-5" />
                    Success Rate
                  </CardTitle>
                  <CardDescription>Percentage of successful requests</CardDescription>
                </CardHeader>
                <CardContent>
                  <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="time" />
                      <YAxis domain={[0, 100]} />
                      <Tooltip />
                      <Legend />
                      <Line
                        type="monotone"
                        dataKey="successRate"
                        stroke="#10b981"
                        strokeWidth={2}
                        name="Success Rate %"
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>

              {/* Status Code Distribution */}
              <Card>
                <CardHeader>
                  <CardTitle>Status Code Distribution</CardTitle>
                  <CardDescription>Breakdown of HTTP status codes</CardDescription>
                </CardHeader>
                <CardContent>
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={statusData} layout="vertical">
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis type="number" />
                      <YAxis dataKey="status" type="category" />
                      <Tooltip />
                      <Bar dataKey="count" fill="#3b82f6" />
                    </BarChart>
                  </ResponsiveContainer>
                  <div className="mt-4 space-y-2">
                    {statusData.map((item) => (
                      <div key={item.status} className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <div
                            className="w-3 h-3 rounded-full"
                            style={{ backgroundColor: item.color }}
                          />
                          <span className="text-sm font-medium">{item.status}</span>
                        </div>
                        <span className="text-sm text-muted-foreground">{item.count}</span>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Endpoint Details */}
            <Card>
              <CardHeader>
                <CardTitle>Endpoint Details</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm font-medium text-slate-600 mb-1">Method</p>
                    <Badge>{endpoint.method}</Badge>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-slate-600 mb-1">Created</p>
                    <p className="text-sm">{endpoint.createdAt.toLocaleDateString()}</p>
                  </div>
                </div>
                {Object.keys(endpoint.headers).length > 0 && (
                  <div>
                    <p className="text-sm font-medium text-slate-600 mb-2">Headers</p>
                    <div className="bg-slate-50 rounded-md p-3 font-mono text-xs">
                      {Object.entries(endpoint.headers).map(([key, value]) => (
                        <div key={key}>
                          <span className="text-slate-600">{key}:</span>{' '}
                          <span className="text-slate-900">{value}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                {endpoint.body && (
                  <div>
                    <p className="text-sm font-medium text-slate-600 mb-2">Request Body</p>
                    <pre className="bg-slate-50 rounded-md p-3 text-xs overflow-x-auto">
                      {endpoint.body}
                    </pre>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
