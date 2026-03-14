/**
 * Development environment configuration
 * Used when running `ng serve` or `ng build` without --configuration=production
 *
 * IMPORTANT: Use relative URL for apiBaseUrl to enable proxy configuration
 * The proxy (proxy.conf.json) will forward /api requests to http://localhost:8080
 */
export const environment = {
  production: false,
  apiBaseUrl: '/api/v1', // Relative URL - proxy will forward to http://localhost:8080
  apiTimeout: 30000, // 30 seconds
  pollingInterval: 5000, // 5 seconds - for auto-refresh
  enableDebugLogs: true
};

