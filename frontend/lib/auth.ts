export type User = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  role: "CUSTOMER" | "ADMIN";
};

export type AuthResponse = {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: User;
  cartId: number | null;
};

export type LoginInput = {
  email: string;
  password: string;
  cartToken?: string;
};

export type RegisterInput = LoginInput & {
  firstName: string;
  lastName: string;
  phone?: string;
  verificationCode: string;
};

export type VerificationCodeResponse = {
  message: string;
  expiresInSeconds: number;
  resendAfterSeconds: number;
};

type ApiErrorBody = {
  message?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
};

export const AUTH_TOKEN_KEY = "snow-alpine-access-token";
export const AUTH_CART_ID_KEY = "snow-alpine-auth-cart-id";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, init);
  if (!response.ok) {
    let body: ApiErrorBody = {};
    try { body = await response.json() as ApiErrorBody; } catch { /* Use the status fallback. */ }
    const fieldMessage = body.fieldErrors?.[0]?.message;
    throw new Error(fieldMessage ? `${body.fieldErrors?.[0].field}: ${fieldMessage}` : body.message ?? `Account request failed (${response.status}).`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export function login(input: LoginInput) {
  return apiRequest<AuthResponse>("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function register(input: RegisterInput) {
  return apiRequest<AuthResponse>("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export function sendVerificationCode(email: string) {
  return apiRequest<VerificationCodeResponse>("/api/auth/verification-codes", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email }),
  });
}

export function getCurrentUser(accessToken: string) {
  return apiRequest<User>("/api/auth/me", {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

export function requestLogout(accessToken: string) {
  return apiRequest<void>("/api/auth/logout", {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}
