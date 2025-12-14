import { useState, useEffect } from 'react';
import { AlertTriangle, CheckCircle2, Clock, ExternalLink } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Checkbox } from './ui/checkbox';
import type { Alarm } from '../types';
import { getAuthHeaders } from '../utils/auth';

export default function AlarmsView() {
  const [alarms, setAlarms] = useState<Alarm[]>([]);
  const [selectedAlarms, setSelectedAlarms] = useState<Set<number>>(new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [isAcknowledging, setIsAcknowledging] = useState(false);

  useEffect(() => {
    loadAlarms();
    // Refresh every 10 seconds
    const interval = setInterval(loadAlarms, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadAlarms = async () => {
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/alarms`, {
        headers: getAuthHeaders(),
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        const parsedAlarms = Array.isArray(data) ? data.map((alarm: any) => ({
          ...alarm,
          testResultId: alarm.testResultId,
          endpointId: alarm.endpointId?.toString() || String(alarm.endpointId),
          timestamp: new Date(alarm.timestamp)
        })) : [];
        setAlarms(parsedAlarms);
      } else {
        console.error('Failed to load alarms:', response.status, response.statusText);
      }
    } catch (error) {
      console.error('Error loading alarms:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggleAlarm = (testResultId: number) => {
    setSelectedAlarms(prev => {
      const newSet = new Set(prev);
      if (newSet.has(testResultId)) {
        newSet.delete(testResultId);
      } else {
        newSet.add(testResultId);
      }
      return newSet;
    });
  };

  const handleAcknowledgeSelected = async () => {
    if (selectedAlarms.size === 0) return;

    setIsAcknowledging(true);
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const acknowledgePromises = Array.from(selectedAlarms).map(testResultId =>
        fetch(`${backendUrl}/api/alarms/${testResultId}/acknowledge`, {
          method: 'POST',
          headers: getAuthHeaders(),
          credentials: 'include'
        })
      );

      await Promise.all(acknowledgePromises);
      
      // Reload alarms and clear selection
      setSelectedAlarms(new Set());
      await loadAlarms();
    } catch (error) {
      console.error('Error acknowledging alarms:', error);
    } finally {
      setIsAcknowledging(false);
    }
  };

  const handleAcknowledgeSingle = async (testResultId: number) => {
    setIsAcknowledging(true);
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/alarms/${testResultId}/acknowledge`, {
        method: 'POST',
        headers: getAuthHeaders(),
        credentials: 'include'
      });

      if (response.ok) {
        await loadAlarms();
      } else {
        console.error('Failed to acknowledge alarm:', response.status);
      }
    } catch (error) {
      console.error('Error acknowledging alarm:', error);
    } finally {
      setIsAcknowledging(false);
    }
  };

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

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 mb-2">Alarms</h1>
          <p className="text-slate-600">Monitoring endpoint errors and failures</p>
        </div>
        <div className="flex justify-center items-center h-64">
          <div className="text-slate-600">Loading alarms...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 mb-2">Alarms</h1>
          <p className="text-slate-600">Monitoring endpoint errors and failures</p>
        </div>
        {selectedAlarms.size > 0 && (
          <Button
            onClick={handleAcknowledgeSelected}
            disabled={isAcknowledging}
            className="bg-blue-600 hover:bg-blue-700"
          >
            <CheckCircle2 className="h-4 w-4 mr-2" />
            Acknowledge Selected ({selectedAlarms.size})
          </Button>
        )}
      </div>

      {alarms.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <CheckCircle2 className="h-12 w-12 text-green-500 mb-4" />
            <h3 className="text-lg font-semibold text-slate-900 mb-2">No Active Alarms</h3>
            <p className="text-sm text-slate-600 text-center">
              All endpoints are healthy. No errors detected.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {alarms.map((alarm) => (
            <Card key={alarm.testResultId} className="border-l-4 border-l-red-500">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-4 flex-1">
                    <Checkbox
                      checked={selectedAlarms.has(alarm.testResultId)}
                      onCheckedChange={() => handleToggleAlarm(alarm.testResultId)}
                      className="mt-1"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <AlertTriangle className="h-5 w-5 text-red-500" />
                        <CardTitle className="text-lg">{alarm.endpointName}</CardTitle>
                        {alarm.statusCode && (
                          <Badge variant="destructive" className="ml-2">
                            {alarm.statusCode}
                          </Badge>
                        )}
                      </div>
                      <CardDescription className="flex items-center gap-4 mt-2">
                        <span className="flex items-center gap-1">
                          <ExternalLink className="h-3 w-3" />
                          <a
                            href={alarm.endpointUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-blue-600 hover:underline"
                          >
                            {alarm.endpointUrl}
                          </a>
                        </span>
                        <span className="flex items-center gap-1">
                          <Clock className="h-3 w-3" />
                          {getTimeAgo(alarm.timestamp)}
                        </span>
                      </CardDescription>
                    </div>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleAcknowledgeSingle(alarm.testResultId)}
                    disabled={isAcknowledging}
                    className="ml-4"
                  >
                    <CheckCircle2 className="h-4 w-4 mr-2" />
                    Acknowledge
                  </Button>
                </div>
              </CardHeader>
              {alarm.error && (
                <CardContent>
                  <div className="bg-red-50 border border-red-200 rounded-md p-3">
                    <p className="text-sm font-medium text-red-900 mb-1">Error Message:</p>
                    <p className="text-sm text-red-700 font-mono">{alarm.error}</p>
                  </div>
                </CardContent>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
