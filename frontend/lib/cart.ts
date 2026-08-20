import type { Product } from "@/lib/catalog";

export type CartItemRequest = {
  productId: number;
  quantity: number;
  bookingDate?: string;
  vehicleRegistration?: string;
  vehicleType?: string;
  entryDate?: string;
  exitDate?: string;
  lessonSessionId?: number;
  rentalStartDate?: string;
  rentalEndDate?: string;
  rentalSize?: string;
  rentalBootSize?: string;
};

export type CartItem = {
  id: number;
  product: Product;
  lessonSessionId: number | null;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  bookingDate: string | null;
  vehicleRegistration: string | null;
  vehicleType: string | null;
  entryDate: string | null;
  exitDate: string | null;
  rentalStartDate: string | null;
  rentalEndDate: string | null;
  rentalSize: string | null;
  rentalBootSize: string | null;
};

export type Cart = {
  id: number;
  status: "ACTIVE" | "CHECKED_OUT" | "ABANDONED";
  itemCount: number;
  subtotal: number;
  total: number;
  currency: string;
  items: CartItem[];
};

export type LessonSession = {
  id: number;
  productId: number;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  availableCount: number;
  status: "ACTIVE" | "CANCELLED";
};

export type CartIdentity = { id: number; token?: string };

type CreateCartResponse = { cartToken: string | null; cart: Cart };
type ApiErrorBody = { message?: string; code?: string };

export const CART_ID_KEY = "snow-alpine-cart-id";
export const CART_TOKEN_KEY = "snow-alpine-cart-token";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, init);
  if (!response.ok) {
    let body: ApiErrorBody = {};
    try { body = await response.json() as ApiErrorBody; } catch { /* Use the status fallback. */ }
    throw new Error(body.message ?? `Booking service request failed (${response.status}).`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

function cartHeaders(identity?: CartIdentity, accessToken?: string | null) {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  else if (identity?.token) headers["X-Cart-Token"] = identity.token;
  return headers;
}

export async function createCart(accessToken?: string | null): Promise<{ identity: CartIdentity; cart: Cart }> {
  const response = await apiRequest<CreateCartResponse>("/api/carts", { method: "POST", headers: cartHeaders(undefined, accessToken) });
  return { identity: { id: response.cart.id, token: response.cartToken ?? undefined }, cart: response.cart };
}

export function getCart(identity: CartIdentity, accessToken?: string | null) {
  return apiRequest<Cart>(`/api/carts/${identity.id}`, { headers: cartHeaders(identity, accessToken) });
}

export function addCartItem(identity: CartIdentity, request: CartItemRequest, accessToken?: string | null) {
  return apiRequest<Cart>(`/api/carts/${identity.id}/items`, { method: "POST", headers: cartHeaders(identity, accessToken), body: JSON.stringify(request) });
}

export function updateCartItem(identity: CartIdentity, itemId: number, request: CartItemRequest, accessToken?: string | null) {
  const { productId: _productId, ...updateRequest } = request;
  void _productId;
  return apiRequest<Cart>(`/api/carts/${identity.id}/items/${itemId}`, { method: "PUT", headers: cartHeaders(identity, accessToken), body: JSON.stringify(updateRequest) });
}

export function deleteCartItem(identity: CartIdentity, itemId: number, accessToken?: string | null) {
  return apiRequest<void>(`/api/carts/${identity.id}/items/${itemId}`, { method: "DELETE", headers: cartHeaders(identity, accessToken) });
}

export function getLessonSessions(productId: number, date: string) {
  return apiRequest<LessonSession[]>(`/api/lesson-sessions?productId=${productId}&date=${encodeURIComponent(date)}`);
}

export function requestFromCartItem(item: CartItem, quantity = item.quantity): CartItemRequest {
  const request: CartItemRequest = { productId: item.product.id, quantity };
  if (item.bookingDate) request.bookingDate = item.bookingDate;
  if (item.vehicleRegistration) request.vehicleRegistration = item.vehicleRegistration;
  if (item.vehicleType) request.vehicleType = item.vehicleType;
  if (item.entryDate) request.entryDate = item.entryDate;
  if (item.exitDate) request.exitDate = item.exitDate;
  if (item.lessonSessionId) request.lessonSessionId = item.lessonSessionId;
  if (item.rentalStartDate) request.rentalStartDate = item.rentalStartDate;
  if (item.rentalEndDate) request.rentalEndDate = item.rentalEndDate;
  if (item.rentalSize) request.rentalSize = item.rentalSize;
  if (item.rentalBootSize) request.rentalBootSize = item.rentalBootSize;
  return request;
}
