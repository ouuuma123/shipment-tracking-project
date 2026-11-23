import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth-service';

const unsafeMethods = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export const credentialsCsrfInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const token = readCsrfCookie();
  const needsCsrfToken = unsafeMethods.has(request.method.toUpperCase()) && token !== null;
  const accessToken = authService.accessToken();
  const isApiRequest = request.url.startsWith(`${environment.apiUrl}/api/`);

  const headers: Record<string, string> = {};
  if (needsCsrfToken && token !== null) {
    headers['X-XSRF-TOKEN'] = token;
  }
  if (isApiRequest && accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }

  const authenticatedRequest = request.clone({
    withCredentials: true,
    ...(Object.keys(headers).length > 0 ? { setHeaders: headers } : {}),
  });

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (
        !(error instanceof HttpErrorResponse) ||
        error.status !== 401 ||
        !isApiRequest ||
        ['/api/auth/login', '/api/auth/register', '/api/auth/refresh',
          '/api/auth/logout', '/api/auth/csrf', '/api/auth/password/forgot',
          '/api/auth/password/reset'].some((path) => request.url.includes(path))
      ) {
        return throwError(() => error);
      }

      return from(authService.refreshAccessToken()).pipe(
        switchMap((newToken) =>
          newToken
            ? next(
                request.clone({
                  withCredentials: true,
                  setHeaders: {
                    Authorization: `Bearer ${newToken}`,
                    ...(needsCsrfToken && token !== null ? { 'X-XSRF-TOKEN': token } : {}),
                  },
                }),
              )
            : throwError(() => error),
        ),
      );
    }),
  );
};

function readCsrfCookie(): string | null {
  if (typeof document === 'undefined') {
    return null;
  }

  const cookie = document.cookie
    .split('; ')
    .find((entry) => entry.startsWith('XSRF-TOKEN='));
  return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : null;
}
