/**
 * Production environment configuration
 * Used when running `ng build --configuration=production`
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api/v1', // Relative URL - assumes frontend served from same domain
  apiTimeout: 30000, // 30 seconds
  pollingInterval: 10000, // 10 seconds - slower refresh in production
  enableDebugLogs: false
};

