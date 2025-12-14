export interface User {
  id: string;
  email: string;
  name: string;
  apiKeys: string[];
}

export interface ApiEndpoint {
  id: string;
  name: string;
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  headers: Record<string, string>;
  body?: string;
  recurringEnabled?: boolean;
  recurringInterval?: string; // '30s', '5m', '1h', '24h'
  createdAt: Date;
  updatedAt: Date;
}

export interface ApiTestResult {
  id: string;
  endpointId: string;
  statusCode?: number;
  responseTime?: number;
  responseBody?: string;
  responseHeaders?: Record<string, string>;
  error?: string;
  success: boolean;
  timestamp: Date;
}

export interface Webhook {
  id: string;
  name: string;
  url: string;
  events: string[];
  secret?: string;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface WebhookEvent {
  id: string;
  webhookId: string;
  eventType: string;
  payload: any;
  status: 'pending' | 'sent' | 'failed';
  responseCode?: number;
  responseTime?: number;
  error?: string;
  timestamp: Date;
}

export interface Alarm {
  testResultId: number;
  endpointId: string;
  endpointName: string;
  endpointUrl: string;
  error?: string;
  statusCode?: number;
  timestamp: Date;
} 