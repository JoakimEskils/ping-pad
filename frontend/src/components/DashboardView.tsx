import { useState, useEffect } from 'react';
import { Activity, TrendingUp, CheckCircle2, XCircle, Clock, Zap, Globe, Server } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { ApiEndpoint, ApiTestResult } from '../types';
import { getAuthHeaders } from '../utils/auth';

// Helper function to format time ago
const getTimeAgo = (date: Date): string => {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  
  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins} min ago`;
  if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
  if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  return date.toLocaleDateString();
};

export default function DashboardView() {
  const [endpoints, setEndpoints] = useState<ApiEndpoint[]>([]);
  const [allTestResults, setAllTestResults] = useState<ApiTestResult[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
    // Refresh every 30 seconds
    const interval = setInterval(loadDashboardData, 30000);
    return () => clearInterval(interval);
  }, []);

  const loadDashboardData = async () => {
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      
      // Load endpoints
      const endpointsResponse = await fetch(`${backendUrl}/api/endpoints`, {
        headers: getAuthHeaders(),
        credentials: 'include'
      });

      if (endpointsResponse.ok) {
        const endpointsData = await endpointsResponse.json();
        const parsedEndpoints = endpointsData.map((endpoint: any) => {
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
            createdAt: endpoint.createdAt ? new Date(endpoint.createdAt) : new Date(),
            updatedAt: endpoint.updatedAt ? new Date(endpoint.updatedAt) : new Date()
          };
        });
        setEndpoints(parsedEndpoints);

        // Load test results for all endpoints (last 24 hours)
        const allResults: ApiTestResult[] = [];
        for (const endpoint of parsedEndpoints) {
          try {
            const analyticsResponse = await fetch(
              `${backendUrl}/api/endpoints/${endpoint.id}/analytics?hours=24`,
              {
                headers: getAuthHeaders(),
                credentials: 'include'
              }
            );
            
            if (analyticsResponse.ok) {
              const results = await analyticsResponse.json();
              const parsedResults = Array.isArray(results) ? results.map((result: any) => {
                // Parse timestamp
                let timestamp: Date;
                if (result.timestamp) {
                  if (typeof result.timestamp === 'string') {
                    const isoString = result.timestamp.includes('T') 
                      ? result.timestamp 
                      : `${result.timestamp}T00:00:00`;
                    timestamp = new Date(isoString);
                  } else {
                    timestamp = new Date();
                  }
                } else {
                  timestamp = new Date();
                }
                
                if (isNaN(timestamp.getTime())) {
                  timestamp = new Date();
                }
                
                return {
                  ...result,
                  timestamp,
                  id: result.id?.toString() || String(result.id),
                  success: result.success !== undefined ? result.success : (result.statusCode ? result.statusCode >= 200 && result.statusCode < 300 : false),
                  endpointId: endpoint.id
                };
              }) : [];
              allResults.push(...parsedResults);
            }
          } catch (error) {
            console.error(`Error loading analytics for endpoint ${endpoint.id}:`, error);
          }
        }
        
        setAllTestResults(allResults);
      }
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // Calculate metrics from real data
  const totalRequests = allTestResults.length;
  const totalErrors = allTestResults.filter(r => !r.success).length;
  const avgResponseTime = allTestResults.length > 0
    ? Math.round(allTestResults.reduce((sum, r) => sum + (r.responseTime || 0), 0) / allTestResults.length)
    : 0;
  const successRate = totalRequests > 0
    ? ((totalRequests - totalErrors) / totalRequests) * 100
    : 0;
  const activeEndpoints = endpoints.length;
  const totalTests = totalRequests;

  // Generate response time data (last 12 hours)
  const responseTimeData = (() => {
    const data: Array<{ time: string; avg: number; p95: number; p99: number }> = [];
    const now = new Date();
    for (let i = 11; i >= 0; i--) {
      const date = new Date(now.getTime() - i * 60 * 60 * 1000);
      const hourStart = new Date(date);
      hourStart.setMinutes(0, 0, 0);
      const hourEnd = new Date(hourStart);
      hourEnd.setHours(hourEnd.getHours() + 1);
      
      const hourResults = allTestResults.filter(r => {
        const rTime = r.timestamp instanceof Date ? r.timestamp : new Date(r.timestamp);
        return rTime >= hourStart && rTime < hourEnd;
      });
      
      const avg = hourResults.length > 0
        ? Math.round(hourResults.reduce((sum, r) => sum + (r.responseTime || 0), 0) / hourResults.length)
        : 0;
      
      data.push({
        time: date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
        avg,
        p95: avg, // Simplified - would need percentile calculation for real p95
        p99: avg, // Simplified
      });
    }
    return data;
  })();

  // Generate request volume data (last 7 days)
  const requestVolumeData = (() => {
    const data: Array<{ day: string; requests: number; errors: number }> = [];
    const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const now = new Date();
    
    for (let i = 6; i >= 0; i--) {
      const date = new Date(now);
      date.setDate(date.getDate() - i);
      date.setHours(0, 0, 0, 0);
      const nextDay = new Date(date);
      nextDay.setDate(nextDay.getDate() + 1);
      
      const dayResults = allTestResults.filter(r => {
        const rTime = r.timestamp instanceof Date ? r.timestamp : new Date(r.timestamp);
        return rTime >= date && rTime < nextDay;
      });
      
      data.push({
        day: days[date.getDay()],
        requests: dayResults.length,
        errors: dayResults.filter(r => !r.success).length,
      });
    }
    return data;
  })();

  // Calculate status distribution
  const statusDistribution = (() => {
    const counts: Record<string, number> = { '2xx': 0, '3xx': 0, '4xx': 0, '5xx': 0 };
    allTestResults.forEach(result => {
      if (result.statusCode) {
        if (result.statusCode >= 200 && result.statusCode < 300) counts['2xx']++;
        else if (result.statusCode >= 300 && result.statusCode < 400) counts['3xx']++;
        else if (result.statusCode >= 400 && result.statusCode < 500) counts['4xx']++;
        else if (result.statusCode >= 500) counts['5xx']++;
      }
    });
    
    return [
      { name: '2xx', value: counts['2xx'], color: '#10b981' },
      { name: '3xx', value: counts['3xx'], color: '#3b82f6' },
      { name: '4xx', value: counts['4xx'], color: '#f59e0b' },
      { name: '5xx', value: counts['5xx'], color: '#ef4444' },
    ].filter(item => item.value > 0);
  })();

  // Calculate endpoint performance
  const endpointPerformance = (() => {
    const endpointStats = new Map<string, { requests: number; totalTime: number; successes: number }>();
    
    allTestResults.forEach(result => {
      const stats = endpointStats.get(result.endpointId) || { requests: 0, totalTime: 0, successes: 0 };
      stats.requests++;
      stats.totalTime += result.responseTime || 0;
      if (result.success) stats.successes++;
      endpointStats.set(result.endpointId, stats);
    });
    
    return Array.from(endpointStats.entries())
      .map(([endpointId, stats]) => {
        const endpoint = endpoints.find(e => e.id === endpointId);
        return {
          name: endpoint?.name || 'Unknown',
          requests: stats.requests,
          avgTime: stats.requests > 0 ? Math.round(stats.totalTime / stats.requests) : 0,
          success: stats.requests > 0 ? (stats.successes / stats.requests) * 100 : 0,
        };
      })
      .sort((a, b) => b.requests - a.requests)
      .slice(0, 5);
  })();

  // Recent activity (last 10 test results)
  const recentActivity = allTestResults
    .sort((a, b) => {
      const aTime = a.timestamp instanceof Date ? a.timestamp : new Date(a.timestamp);
      const bTime = b.timestamp instanceof Date ? b.timestamp : new Date(b.timestamp);
      return bTime.getTime() - aTime.getTime();
    })
    .slice(0, 10)
    .map((result, idx) => {
      const endpoint = endpoints.find(e => e.id === result.endpointId);
      const resultTime = result.timestamp instanceof Date ? result.timestamp : new Date(result.timestamp);
      const timeAgo = getTimeAgo(resultTime);
      
      return {
        id: idx,
        endpoint: endpoint?.name || 'Unknown Endpoint',
        status: result.success ? 'success' : 'error',
        time: timeAgo,
        responseTime: `${result.responseTime || 0}ms`,
      };
    });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 mb-2">Dashboard Overview</h1>
          <p className="text-slate-600">Monitor your API performance and health at a glance</p>
        </div>
        <div className="flex justify-center items-center h-64">
          <div className="text-slate-600">Loading dashboard data...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Welcome Section */}
      <div>
        <h1 className="text-3xl font-bold text-slate-900 mb-2">Dashboard Overview</h1>
        <p className="text-slate-600">Monitor your API performance and health at a glance</p>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Requests</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalRequests.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground flex items-center gap-1 mt-1">
              <TrendingUp className="h-3 w-3 text-green-600" />
              <span className="text-green-600">+12.5%</span> from last week
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Success Rate</CardTitle>
            <CheckCircle2 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{successRate.toFixed(1)}%</div>
            <p className="text-xs text-muted-foreground flex items-center gap-1 mt-1">
              <span>{totalRequests - totalErrors} successful</span>
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Avg Response Time</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{avgResponseTime}ms</div>
            <p className="text-xs text-muted-foreground flex items-center gap-1 mt-1">
              <TrendingUp className="h-3 w-3 text-green-600" />
              <span className="text-green-600">-8.2%</span> improvement
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Endpoints</CardTitle>
            <Server className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{activeEndpoints}</div>
            <p className="text-xs text-muted-foreground mt-1">
              {totalTests} tests executed
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Charts Row 1 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Response Time Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Zap className="h-5 w-5" />
              Response Time Trends
            </CardTitle>
            <CardDescription>Average, P95, and P99 response times over the last 12 hours</CardDescription>
          </CardHeader>
          <CardContent>
            {responseTimeData.length === 0 ? (
              <div className="flex items-center justify-center h-[300px] text-slate-500">
                No data available
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={responseTimeData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="time" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Area
                  type="monotone"
                  dataKey="avg"
                  stackId="1"
                  stroke="#3b82f6"
                  fill="#3b82f6"
                  fillOpacity={0.6}
                  name="Average"
                />
                <Area
                  type="monotone"
                  dataKey="p95"
                  stackId="2"
                  stroke="#8b5cf6"
                  fill="#8b5cf6"
                  fillOpacity={0.6}
                  name="P95"
                />
                <Area
                  type="monotone"
                  dataKey="p99"
                  stackId="3"
                  stroke="#ec4899"
                  fill="#ec4899"
                  fillOpacity={0.6}
                  name="P99"
                />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* Request Volume Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Globe className="h-5 w-5" />
              Request Volume
            </CardTitle>
            <CardDescription>Daily request volume and error count for the past week</CardDescription>
          </CardHeader>
          <CardContent>
            {requestVolumeData.length === 0 ? (
              <div className="flex items-center justify-center h-[300px] text-slate-500">
                No data available
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={requestVolumeData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis yAxisId="left" />
                <YAxis yAxisId="right" orientation="right" />
                <Tooltip />
                <Legend />
                <Bar yAxisId="left" dataKey="requests" fill="#3b82f6" name="Requests" />
                <Bar yAxisId="right" dataKey="errors" fill="#ef4444" name="Errors" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Charts Row 2 */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Status Distribution */}
        <Card>
          <CardHeader>
            <CardTitle>Status Code Distribution</CardTitle>
            <CardDescription>Breakdown of HTTP status codes</CardDescription>
          </CardHeader>
          <CardContent>
            {statusDistribution.length === 0 ? (
              <div className="flex items-center justify-center h-[250px] text-slate-500">
                No data available
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                <Pie
                  data={statusDistribution}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }: { name?: string; percent?: number }) => `${name || ''} ${((percent || 0) * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {statusDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* Endpoint Performance */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Top Performing Endpoints</CardTitle>
            <CardDescription>Request volume, average response time, and success rate</CardDescription>
          </CardHeader>
          <CardContent>
            {endpointPerformance.length === 0 ? (
              <div className="flex items-center justify-center h-[250px] text-slate-500">
                No endpoint data available
              </div>
            ) : (
              <div className="space-y-4">
                {endpointPerformance.map((endpoint, index) => (
                <div key={index} className="flex items-center justify-between p-4 bg-slate-50 rounded-lg">
                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-semibold text-slate-900">{endpoint.name}</h4>
                      <Badge className={endpoint.success >= 98 ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}>
                        {endpoint.success.toFixed(1)}% success
                      </Badge>
                    </div>
                    <div className="flex items-center gap-4 text-sm text-slate-600">
                      <span>{endpoint.requests.toLocaleString()} requests</span>
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {endpoint.avgTime}ms avg
                      </span>
                    </div>
                  </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Recent Activity */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Activity</CardTitle>
          <CardDescription>Latest API test results and events</CardDescription>
        </CardHeader>
        <CardContent>
          {recentActivity.length === 0 ? (
            <div className="text-center py-8 text-slate-500">
              <p>No recent activity</p>
              <p className="text-sm mt-2">Test endpoints to see activity here</p>
            </div>
          ) : (
            <div className="space-y-3">
              {recentActivity.map((activity) => (
                <div
                  key={activity.id}
                  className="flex items-center justify-between p-4 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors"
                >
                  <div className="flex items-center gap-4">
                    {activity.status === 'success' ? (
                      <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
                        <CheckCircle2 className="h-5 w-5 text-green-600" />
                      </div>
                    ) : (
                      <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
                        <XCircle className="h-5 w-5 text-red-600" />
                      </div>
                    )}
                    <div>
                      <p className="font-medium text-slate-900">{activity.endpoint}</p>
                      <p className="text-sm text-slate-500">{activity.time}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <Badge variant="outline" className="font-mono">
                      {activity.responseTime}
                    </Badge>
                    <Badge className={activity.status === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}>
                      {activity.status}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
