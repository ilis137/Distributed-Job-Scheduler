import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

/**
 * API Interceptor - adds base URL and common headers to all HTTP requests
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  // Skip if URL is already absolute
  if (req.url.startsWith('http://') || req.url.startsWith('https://')) {
    return next(req);
  }

  // Add base URL and headers
  const apiReq = req.clone({
    url: `${environment.apiBaseUrl}${req.url}`,
    setHeaders: {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }
  });

  if (environment.enableDebugLogs) {
    console.log('[API] Request:', apiReq.method, apiReq.url);
  }

  return next(apiReq);
};

