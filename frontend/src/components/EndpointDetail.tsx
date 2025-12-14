import { useState, useEffect } from 'react';
import { X, Activity, Clock, CheckCircle2, XCircle, TrendingUp, ChevronDown, ChevronUp, List } from 'lucide-react';
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
import type { ApiEndpoint, ApiTestResult } from '../types';
import { getAuthHeaders } from '../utils/auth';

interface EndpointDetailProps {
  endpoint: ApiEndpoint;
  onClose: () => void;
}

interface ChartDataPoint {
  time: string;
  hour: number;
  responseTime: number;
  successRate: number;
  requests: number;
  errors: number;
  success: number;
  _timestamp?: number; // Internal field for sorting
}

interface StatusDistribution {
  status: string;
  count: number;
  color: string;
}

export default function EndpointDetail({ endpoint, onClose }: EndpointDetailProps) {
  const [timeRange, setTimeRange] = useState<'24h' | '7d' | '30d'>('24h');
  const [testResults, setTestResults] = useState<ApiTestResult[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedCalls, setExpandedCalls] = useState<Set<string>>(new Set());

  // Fetch analytics data
  const fetchAnalytics = async () => {
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const hours = timeRange === '24h' ? 24 : timeRange === '7d' ? 168 : 720;
      
      // Ensure endpoint.id is a string
      const endpointId = String(endpoint.id);
      
      const response = await fetch(
        `${backendUrl}/api/endpoints/${endpointId}/analytics?hours=${hours}`,
        {
          headers: getAuthHeaders(),
          credentials: 'include'
        }
      );

      if (response.ok) {
        const data = await response.json();
        // Convert timestamps to Date objects and parse response headers
        const results = Array.isArray(data) ? data.map((result: any) => {
          // Parse responseHeaders from string format if needed
          let responseHeaders: Record<string, string> | undefined = undefined;
          if (result.responseHeaders) {
            if (typeof result.responseHeaders === 'string') {
              const headers: Record<string, string> = {};
              result.responseHeaders.split('\n').forEach((line: string) => {
                const [key, ...valueParts] = line.split(':');
                if (key && valueParts.length > 0) {
                  headers[key.trim()] = valueParts.join(':').trim();
                }
              });
              responseHeaders = headers;
            } else {
              responseHeaders = result.responseHeaders;
            }
          }
          
          // Parse timestamp - handle ISO string format from LocalDateTime
          // Format from backend: "2025-12-14T19:36:56" (no timezone, local time)
          let timestamp: Date;
          if (result.timestamp) {
            if (typeof result.timestamp === 'string') {
              // Handle ISO format: "2025-12-14T19:36:56" or "2025-12-14T19:36:56.123"
              // LocalDateTime doesn't include timezone, so parse as local time
              let isoString = result.timestamp.trim();
              
              // Ensure it has 'T' separator
              if (!isoString.includes('T')) {
                isoString = `${isoString}T00:00:00`;
              }
              
              // Parse as local time (no timezone conversion)
              // Split date and time, then create Date object
              const [datePart, timePart] = isoString.split('T');
              const [year, month, day] = datePart.split('-').map(Number);
              const timeStr = timePart.split('.')[0]; // Remove milliseconds if present
              const [hour, minute, second] = timeStr.split(':').map(Number);
              
              timestamp = new Date(year, month - 1, day, hour || 0, minute || 0, second || 0);
            } else if (Array.isArray(result.timestamp)) {
              // Handle LocalDateTime array format [year, month, day, hour, minute, second, nanosecond]
              const [year, month, day, hour, minute, second] = result.timestamp;
              timestamp = new Date(year, month - 1, day, hour || 0, minute || 0, second || 0);
            } else {
              timestamp = new Date();
            }
          } else {
            timestamp = new Date();
          }
          
          // Validate the date
          if (isNaN(timestamp.getTime())) {
            console.warn('Invalid timestamp:', result.timestamp, 'Using current time');
            timestamp = new Date();
          }
          
          return {
            ...result,
            timestamp,
            id: result.id?.toString() || String(result.id),
            success: result.success !== undefined ? result.success : (result.statusCode ? result.statusCode >= 200 && result.statusCode < 300 : false),
            responseHeaders
          };
        }) : [];
        setTestResults(results);
        setError(null);
      } else {
        // Try to get error message from response
        let errorMsg = 'Failed to load analytics data';
        try {
          const errorData = await response.json();
          if (errorData.error) {
            errorMsg = errorData.error;
          }
        } catch {
          errorMsg = `Failed to load analytics data (${response.status} ${response.statusText})`;
        }
        setError(errorMsg);
        console.error('Analytics fetch failed:', response.status, response.statusText);
      }
    } catch (err) {
      console.error('Error fetching analytics:', err);
      setError(`Error loading analytics data: ${err instanceof Error ? err.message : String(err)}`);
    } finally {
      setIsLoading(false);
    }
  };

  // Initial fetch and when time range changes
  useEffect(() => {
    setIsLoading(true);
    fetchAnalytics();
  }, [endpoint.id, timeRange]);

  // Poll for updates every 5 seconds when modal is open
  useEffect(() => {
    const interval = setInterval(() => {
      fetchAnalytics();
    }, 5000); // Poll every 5 seconds

    return () => clearInterval(interval);
  }, [endpoint.id, timeRange]);

  // Transform test results into hourly chart data
  const transformToChartData = (): ChartDataPoint[] => {
    if (testResults.length === 0) {
      return [];
    }

    const now = new Date();
    const hours = timeRange === '24h' ? 24 : timeRange === '7d' ? 168 : 720;
    
    // Create time buckets
    const buckets: Map<string, ApiTestResult[]> = new Map();
    const startTime = new Date(now.getTime() - hours * 60 * 60 * 1000);

    // Group results by time bucket
    testResults.forEach(result => {
      // Ensure timestamp is a valid Date object
      const resultTime = result.timestamp instanceof Date 
        ? result.timestamp 
        : new Date(result.timestamp);
      
      // Skip if invalid date or before start time
      if (isNaN(resultTime.getTime()) || resultTime < startTime) return;

      let bucketKey: string;
      if (timeRange === '24h') {
        // Group by hour
        const hour = resultTime.getHours();
        bucketKey = `${resultTime.toLocaleDateString()}-${hour}`;
      } else if (timeRange === '7d') {
        // Group by hour
        const hour = resultTime.getHours();
        bucketKey = `${resultTime.toLocaleDateString()}-${hour}`;
      } else {
        // Group by day
        bucketKey = resultTime.toLocaleDateString();
      }

      if (!buckets.has(bucketKey)) {
        buckets.set(bucketKey, []);
      }
      buckets.get(bucketKey)!.push(result);
    });

    // Convert buckets to chart data points
    const chartData: ChartDataPoint[] = [];
    const sortedBuckets = Array.from(buckets.entries()).sort((a, b) => {
      const aTime = new Date(a[0].split('-')[0] + ' ' + (a[0].split('-')[1] || '00:00'));
      const bTime = new Date(b[0].split('-')[0] + ' ' + (b[0].split('-')[1] || '00:00'));
      return aTime.getTime() - bTime.getTime();
    });

    sortedBuckets.forEach(([bucketKey, results]) => {
      const avgResponseTime = results.reduce((sum, r) => sum + (r.responseTime || 0), 0) / results.length;
      const successCount = results.filter(r => r.success === true).length;
      const errorCount = results.length - successCount;
      const successRate = results.length > 0 ? (successCount / results.length) * 100 : 0;

      // Parse the bucket key to get date and hour
      const [bucketDatePart, bucketHourPart] = bucketKey.split('-');
      const displayTime = timeRange === '30d' 
        ? new Date(bucketDatePart).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
        : `${new Date(bucketDatePart).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} ${bucketHourPart || '00'}:00`;

      // Parse the bucket key to get actual timestamp for sorting
      let bucketTimestamp: Date;
      try {
        if (bucketHourPart) {
          bucketTimestamp = new Date(`${bucketDatePart} ${bucketHourPart}:00:00`);
        } else {
          bucketTimestamp = new Date(bucketDatePart);
        }
        if (isNaN(bucketTimestamp.getTime())) {
          bucketTimestamp = new Date();
        }
      } catch {
        bucketTimestamp = new Date();
      }

      chartData.push({
        time: displayTime,
        hour: bucketHourPart ? parseInt(bucketHourPart) : 0,
        responseTime: Math.round(avgResponseTime),
        successRate: Math.round(successRate),
        requests: results.length,
        errors: errorCount,
        success: successCount,
        _timestamp: bucketTimestamp.getTime(), // Internal field for sorting
      });
    });

    // Sort by timestamp
    return chartData.sort((a, b) => (a._timestamp || 0) - (b._timestamp || 0));
  };

  // Calculate status code distribution
  const calculateStatusDistribution = (): StatusDistribution[] => {
    const statusCounts: Map<number, number> = new Map();
    
    testResults.forEach(result => {
      if (result.statusCode) {
        const status = result.statusCode;
        statusCounts.set(status, (statusCounts.get(status) || 0) + 1);
      }
    });

    const distribution: StatusDistribution[] = [];
    statusCounts.forEach((count, status) => {
      let color = '#ef4444'; // red for errors
      if (status >= 200 && status < 300) {
        color = '#10b981'; // green for success
      } else if (status >= 300 && status < 400) {
        color = '#3b82f6'; // blue for redirects
      } else if (status >= 400 && status < 500) {
        color = '#f59e0b'; // yellow for client errors
      }
      
      distribution.push({
        status: status.toString(),
        count,
        color
      });
    });

    return distribution.sort((a, b) => b.count - a.count);
  };

  const chartData = transformToChartData();
  const statusData = calculateStatusDistribution();

  // Calculate stats
  const avgResponseTime = chartData.length > 0
    ? Math.round(chartData.reduce((sum, d) => sum + d.responseTime, 0) / chartData.length)
    : 0;
  const totalRequests = testResults.length;
  const totalErrors = testResults.filter(r => r.success === false).length;
  const successRate = totalRequests > 0
    ? Math.round(((totalRequests - totalErrors) / totalRequests) * 100)
    : 0;

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
                {endpoint.recurringEnabled && (
                  <Badge variant="default" className="text-xs bg-green-100 text-green-700">
                    Auto-enabled
                  </Badge>
                )}
              </div>
              <p className="text-sm text-slate-600 font-mono bg-slate-50 px-3 py-1.5 rounded-md inline-block">{endpoint.url}</p>
            </div>
            <Button variant="ghost" size="icon" onClick={onClose}>
              <X className="h-5 w-5" />
            </Button>
          </div>

          {/* Content */}
          <div className="p-6 space-y-6">
            {error && (
              <Card className="border-red-200 bg-red-50">
                <CardContent className="p-4">
                  <p className="text-sm text-red-700">{error}</p>
                </CardContent>
              </Card>
            )}

            {isLoading && testResults.length === 0 ? (
              <div className="flex justify-center items-center h-64">
                <div className="text-slate-600">Loading analytics...</div>
              </div>
            ) : testResults.length === 0 ? (
              <Card>
                <CardContent className="p-8 text-center">
                  <p className="text-slate-600">No test results available yet.</p>
                  <p className="text-sm text-slate-500 mt-2">
                    {endpoint.recurringEnabled 
                      ? 'Results will appear here once automatic tests start running.'
                      : 'Test the endpoint to see results here.'}
                  </p>
                </CardContent>
              </Card>
            ) : (
              <>
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
                        Based on {totalRequests} test{totalRequests !== 1 ? 's' : ''}
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
                        Last {timeRange}
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
                        {totalRequests > 0 ? Math.round((totalErrors / totalRequests) * 100) : 0}% error rate
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
                {chartData.length > 0 ? (
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
                        <CardDescription>Number of requests per {timeRange === '30d' ? 'day' : 'hour'}</CardDescription>
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
                    {statusData.length > 0 && (
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
                    )}
                  </div>
                ) : (
                  <Card>
                    <CardContent className="p-8 text-center">
                      <p className="text-slate-600">No data available for the selected time range.</p>
                    </CardContent>
                  </Card>
                )}
              </>
            )}

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

            {/* Recent Calls */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <List className="h-5 w-5" />
                  Recent Calls
                </CardTitle>
                <CardDescription>Last 10 test executions for this endpoint</CardDescription>
              </CardHeader>
              <CardContent>
                {testResults.length === 0 ? (
                  <div className="text-center py-8 text-slate-500">
                    <p>No test results available yet.</p>
                    <p className="text-sm mt-2">Test the endpoint to see results here.</p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {testResults.slice(0, 10).map((result) => {
                      const isExpanded = expandedCalls.has(result.id);
                      const toggleExpand = () => {
                        setExpandedCalls(prev => {
                          const newSet = new Set(prev);
                          if (newSet.has(result.id)) {
                            newSet.delete(result.id);
                          } else {
                            newSet.add(result.id);
                          }
                          return newSet;
                        });
                      };

                      return (
                        <div
                          key={result.id}
                          className="border border-slate-200 rounded-lg overflow-hidden hover:border-slate-300 transition-colors"
                        >
                          <div
                            className="p-4 cursor-pointer bg-slate-50 hover:bg-slate-100 transition-colors"
                            onClick={toggleExpand}
                          >
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-4 flex-1">
                                <div className="flex items-center gap-2">
                                  {isExpanded ? (
                                    <ChevronUp className="h-4 w-4 text-slate-400" />
                                  ) : (
                                    <ChevronDown className="h-4 w-4 text-slate-400" />
                                  )}
                                  <span className="text-xs text-slate-500 font-mono">
                                    {result.timestamp.toLocaleString()}
                                  </span>
                                </div>
                                <div className="flex items-center gap-3">
                                  {result.statusCode && (
                                    <Badge
                                      className={
                                        result.statusCode >= 200 && result.statusCode < 300
                                          ? 'bg-green-100 text-green-700'
                                          : result.statusCode >= 400
                                          ? 'bg-red-100 text-red-700'
                                          : 'bg-yellow-100 text-yellow-700'
                                      }
                                    >
                                      {result.statusCode}
                                    </Badge>
                                  )}
                                  {result.responseTime && (
                                    <span className="text-sm text-slate-600">
                                      <Clock className="h-3 w-3 inline mr-1" />
                                      {result.responseTime}ms
                                    </span>
                                  )}
                                  <Badge
                                    className={
                                      result.success
                                        ? 'bg-green-100 text-green-700'
                                        : 'bg-red-100 text-red-700'
                                    }
                                  >
                                    {result.success ? 'Success' : 'Failed'}
                                  </Badge>
                                </div>
                              </div>
                            </div>
                          </div>
                          {isExpanded && (
                            <div className="p-4 bg-white border-t border-slate-200 space-y-3">
                              {result.error && (
                                <div>
                                  <p className="text-sm font-medium text-slate-600 mb-1">Error</p>
                                  <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{result.error}</p>
                                </div>
                              )}
                              {result.responseBody && (
                                <div>
                                  <p className="text-sm font-medium text-slate-600 mb-1">Response Body</p>
                                  <pre className="bg-slate-50 rounded-md p-3 text-xs overflow-x-auto max-h-60 overflow-y-auto">
                                    {result.responseBody.length > 1000
                                      ? result.responseBody.substring(0, 1000) + '...'
                                      : result.responseBody}
                                  </pre>
                                </div>
                              )}
                              {result.responseHeaders && (
                                <div>
                                  <p className="text-sm font-medium text-slate-600 mb-1">Response Headers</p>
                                  <div className="bg-slate-50 rounded-md p-3 font-mono text-xs">
                                    {result.responseHeaders && Object.entries(result.responseHeaders).map(([key, value]) => (
                                      <div key={key}>
                                        <span className="text-slate-600">{key}:</span>{' '}
                                        <span className="text-slate-900">{String(value)}</span>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              )}
                              <div className="text-xs text-slate-500">
                                Test ID: {result.id}
                              </div>
                            </div>
                          )}
                        </div>
                      );
                    })}
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
