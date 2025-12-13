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

// Generate dummy dashboard data
const generateRecentActivity = () => {
  return [
    { id: 1, endpoint: 'User Authentication API', status: 'success', time: '2 min ago', responseTime: '145ms' },
    { id: 2, endpoint: 'Get User Profile', status: 'success', time: '5 min ago', responseTime: '89ms' },
    { id: 3, endpoint: 'Create New Post', status: 'error', time: '8 min ago', responseTime: '452ms' },
    { id: 4, endpoint: 'Update Product', status: 'success', time: '12 min ago', responseTime: '123ms' },
    { id: 5, endpoint: 'Delete Comment', status: 'success', time: '15 min ago', responseTime: '67ms' },
  ];
};

const generateResponseTimeData = (): Array<{ time: string; avg: number; p95: number; p99: number }> => {
  const data: Array<{ time: string; avg: number; p95: number; p99: number }> = [];
  for (let i = 11; i >= 0; i--) {
    const date = new Date();
    date.setHours(date.getHours() - i);
    data.push({
      time: date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
      avg: Math.round(80 + Math.random() * 60),
      p95: Math.round(120 + Math.random() * 80),
      p99: Math.round(180 + Math.random() * 100),
    });
  }
  return data;
};

const generateRequestVolumeData = (): Array<{ day: string; requests: number; errors: number }> => {
  const data: Array<{ day: string; requests: number; errors: number }> = [];
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  days.forEach((day) => {
    data.push({
      day,
      requests: Math.floor(5000 + Math.random() * 3000),
      errors: Math.floor(50 + Math.random() * 100),
    });
  });
  return data;
};

const generateStatusDistribution = () => {
  return [
    { name: '2xx', value: 85, color: '#10b981' },
    { name: '3xx', value: 5, color: '#3b82f6' },
    { name: '4xx', value: 7, color: '#f59e0b' },
    { name: '5xx', value: 3, color: '#ef4444' },
  ];
};

const generateEndpointPerformance = () => {
  return [
    { name: 'User Auth API', requests: 12500, avgTime: 145, success: 98.5 },
    { name: 'Get User Profile', requests: 8900, avgTime: 89, success: 99.2 },
    { name: 'Create Post', requests: 5600, avgTime: 234, success: 95.8 },
    { name: 'Update Product', requests: 3200, avgTime: 123, success: 97.3 },
    { name: 'Delete Comment', requests: 1800, avgTime: 67, success: 99.8 },
  ];
};

export default function DashboardView() {
  const recentActivity = generateRecentActivity();
  const responseTimeData = generateResponseTimeData();
  const requestVolumeData = generateRequestVolumeData();
  const statusDistribution = generateStatusDistribution();
  const endpointPerformance = generateEndpointPerformance();

  const totalRequests = 32000;
  const totalErrors = 320;
  const avgResponseTime = 156;
  const successRate = 99.0;
  const activeEndpoints = 5;
  const totalTests = 1250;

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
            <div className="text-2xl font-bold">{successRate}%</div>
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
          </CardContent>
        </Card>

        {/* Endpoint Performance */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Top Performing Endpoints</CardTitle>
            <CardDescription>Request volume, average response time, and success rate</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {endpointPerformance.map((endpoint, index) => (
                <div key={index} className="flex items-center justify-between p-4 bg-slate-50 rounded-lg">
                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-semibold text-slate-900">{endpoint.name}</h4>
                      <Badge variant={endpoint.success >= 98 ? 'success' : 'secondary'}>
                        {endpoint.success}% success
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
                  <Badge variant={activity.status === 'success' ? 'success' : 'destructive'}>
                    {activity.status}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
