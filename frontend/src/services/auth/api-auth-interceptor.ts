import { DOCUMENT } from '@angular/common';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, isDevMode } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { ApiAccessError } from './api-access-error';
import { isOwnApiUrl } from './api-config';
import { AuthService } from './auth-service';

export const apiAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const baseUrl = inject(DOCUMENT).baseURI;

  if (!isOwnApiUrl(request.url, baseUrl)) {
    return next(request);
  }

  const auth = inject(AuthService);

  return from(auth.getAccessToken()).pipe(
    switchMap((token) => {
      const authorizedRequest = request.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
      });

      return next(authorizedRequest);
    }),
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      if (error.status === 401) {
        auth.invalidateSession();
        return throwError(() => new ApiAccessError('session'));
      }

      if (error.status === 403) {
        return throwError(() => new ApiAccessError('forbidden'));
      }

      if (isApiUnavailable(error)) {
        return throwError(() => new ApiAccessError('network'));
      }

      return throwError(() => error);
    }),
  );
};

function isApiUnavailable(error: HttpErrorResponse): boolean {
  if ([0, 502, 503, 504].includes(error.status)) {
    return true;
  }

  return (
    isDevMode() &&
    error.status === 500 &&
    !error.error &&
    error.headers.get('Content-Type') === 'text/plain'
  );
}
