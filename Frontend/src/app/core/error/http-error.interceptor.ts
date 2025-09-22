// src/app/core/error/http-error.interceptor.ts
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';

@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        const msg = err.error?.message || err.statusText || 'Request failed';
        
        // Log to console for debugging
        console.error('[HTTP ERROR]', {
          url: req.url,
          status: err.status,
          message: msg,
          error: err.error
        });
        
        // Re-throw so components can handle if needed
        return throwError(() => err);
      })
    );
  }
}