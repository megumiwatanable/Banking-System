import { HttpInterceptorFn } from "@angular/common/http";

export const AUTH_TOKEN_KEY = "banking_token";

/** Adds the bearer token only when the current browser session is authenticated. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  const authenticatedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authenticatedRequest);
};
