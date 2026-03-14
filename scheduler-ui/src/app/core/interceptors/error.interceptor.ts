import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Error Interceptor - handles HTTP errors globally
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unknown error occurred';

      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = `Client Error: ${error.error.message}`;
      } else {
        // Server-side error
        if (error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.status === 0) {
          errorMessage = 'Unable to connect to server. Please check if the backend is running.';
        } else {
          errorMessage = `Server Error: ${error.status} - ${error.statusText}`;
        }
      }

      if (environment.enableDebugLogs) {
        console.error('[API] Error:', errorMessage, error);
      }

      return throwError(() => new Error(errorMessage));
    })
  );
};

