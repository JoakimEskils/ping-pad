const TOKEN_KEY = 'pingpad_auth_token';
const CORRELATION_ID_KEY = 'pingpad_correlation_id';

/**
 * Generates a new correlation ID (UUID v4)
 */
const generateCorrelationId = (): string => {
  return crypto.randomUUID();
};

/**
 * Gets or creates a correlation ID for the current session
 */
const getOrCreateCorrelationId = (): string => {
  let correlationId = sessionStorage.getItem(CORRELATION_ID_KEY);
  if (!correlationId) {
    correlationId = generateCorrelationId();
    sessionStorage.setItem(CORRELATION_ID_KEY, correlationId);
  }
  return correlationId;
};

export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const removeToken = (): void => {
  localStorage.removeItem(TOKEN_KEY);
};

/**
 * Gets headers with correlation ID (without authentication)
 */
export const getCorrelationHeaders = (): HeadersInit => {
  const correlationId = getOrCreateCorrelationId();
  return {
    'Content-Type': 'application/json',
    'X-Correlation-ID': correlationId,
  };
};

export const getAuthHeaders = (): HeadersInit => {
  const token = getToken();
  const correlationId = getOrCreateCorrelationId();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    'X-Correlation-ID': correlationId,
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  return headers;
};
