import { HttpInterceptorFn } from '@angular/common/http';

export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  return next(request.url.startsWith('http://localhost:8080/api/')
    ? request.clone({ withCredentials: true })
    : request);
};
