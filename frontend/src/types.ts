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
  createdAt: Date;
  updatedAt: Date;
}

export interface ApiTestResult {
  id: string;
  endpointId: string;
  statusCode: number;
  responseTime: number;
  responseBody: string;
  responseHeaders: Record<string, string>;
  error?: string;
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