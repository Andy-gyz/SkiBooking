import type { BookingItem, BookingStatus, PaymentStatus } from "@/lib/booking";
import type { ResortSummary } from "@/lib/catalog";
import { publicApiBaseUrl } from "@/lib/api-config";

export type AdminCategory = "RESORT_ACCESS" | "LIFT_TICKET" | "LESSON" | "RENTAL";

export type AdminDashboard = {
  resortAccessReservations: number;
  liftTicketReservations: number;
  lessonReservations: number;
  rentalReservations: number;
};

export type AdminReservation = {
  bookingId: number;
  bookingNumber: string;
  bookingStatus: BookingStatus;
  createdAt: string;
  customerFirstName: string;
  customerLastName: string;
  customerEmail: string;
  customerPhone: string | null;
  paymentStatus: PaymentStatus | null;
  item: BookingItem;
};

export type AdminPayment = {
  id: number;
  stripePaymentId: string | null;
  amount: number;
  currency: string;
  status: PaymentStatus;
  paymentMethod: string | null;
  paidAt: string | null;
  createdAt: string;
};

export type AdminBooking = {
  id: number;
  bookingNumber: string;
  status: BookingStatus;
  currency: string;
  totalAmount: number;
  customerFirstName: string;
  customerLastName: string;
  customerEmail: string;
  customerPhone: string | null;
  createdAt: string;
  updatedAt: string;
  items: BookingItem[];
  payments: AdminPayment[];
};

export type AdminProduct = {
  id: number;
  resort: ResortSummary;
  name: string;
  category: AdminCategory;
  description: string | null;
  price: number;
  currency: string;
  imageUrl: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AdminProductInput = {
  resortId: number;
  name: string;
  category: AdminCategory;
  description?: string;
  price: number;
  imageUrl?: string;
  active: boolean;
};

export type Resort = ResortSummary & { description: string | null; imageUrl: string | null };

export type AdminLessonSession = {
  id: number;
  productId: number;
  productName: string;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  bookedCount: number;
  availableCount: number;
  status: "ACTIVE" | "CANCELLED";
};

export type AdminLessonSessionInput = {
  productId: number;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  status: "ACTIVE" | "CANCELLED";
};

export type AdminLessonSessionBulkInput = {
  productId: number;
  startDate: string;
  endDate: string;
  daysOfWeek: Array<"MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY">;
  slots: Array<{ startTime: string; endTime: string; capacity: number }>;
};

export type AdminLessonSessionBulkResult = {
  createdCount: number;
  skippedCount: number;
  sessions: AdminLessonSession[];
};

type ApiErrorBody = { message?: string };

async function adminRequest<T>(path: string, accessToken: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${publicApiBaseUrl}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${accessToken}`, ...init?.headers },
  });
  if (!response.ok) {
    let body: ApiErrorBody = {};
    try { body = await response.json() as ApiErrorBody; } catch { /* Use the fallback. */ }
    throw new Error(body.message ?? `Admin request failed (${response.status}).`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export function getAdminDashboard(accessToken: string) {
  return adminRequest<AdminDashboard>("/api/admin/dashboard", accessToken);
}

export function getAdminReservations(category: AdminCategory, accessToken: string) {
  return adminRequest<AdminReservation[]>(`/api/admin/bookings?category=${category}`, accessToken);
}

export function getAdminBooking(id: number, accessToken: string) {
  return adminRequest<AdminBooking>(`/api/admin/bookings/${id}`, accessToken);
}

export async function getResorts() {
  const response = await fetch(`${publicApiBaseUrl}/api/resorts`);
  if (!response.ok) throw new Error("We could not load resorts.");
  return response.json() as Promise<Resort[]>;
}

export function getAdminProducts(accessToken: string) {
  return adminRequest<AdminProduct[]>("/api/admin/products", accessToken);
}

export function createAdminProduct(input: AdminProductInput, accessToken: string) {
  return adminRequest<AdminProduct>("/api/admin/products", accessToken, { method: "POST", body: JSON.stringify(input) });
}

export function updateAdminProduct(id: number, input: AdminProductInput, accessToken: string) {
  return adminRequest<AdminProduct>(`/api/admin/products/${id}`, accessToken, { method: "PUT", body: JSON.stringify(input) });
}

export function deactivateAdminProduct(id: number, accessToken: string) {
  return adminRequest<void>(`/api/admin/products/${id}`, accessToken, { method: "DELETE" });
}

export function getAdminLessonSessions(accessToken: string) {
  return adminRequest<AdminLessonSession[]>("/api/admin/lesson-sessions", accessToken);
}

export function createAdminLessonSession(input: AdminLessonSessionInput, accessToken: string) {
  return adminRequest<AdminLessonSession>("/api/admin/lesson-sessions", accessToken, { method: "POST", body: JSON.stringify(input) });
}

export function updateAdminLessonSession(id: number, input: AdminLessonSessionInput, accessToken: string) {
  return adminRequest<AdminLessonSession>(`/api/admin/lesson-sessions/${id}`, accessToken, { method: "PUT", body: JSON.stringify(input) });
}

export function generateAdminLessonSessions(input: AdminLessonSessionBulkInput, accessToken: string) {
  return adminRequest<AdminLessonSessionBulkResult>("/api/admin/lesson-sessions/generate", accessToken, { method: "POST", body: JSON.stringify(input) });
}

export const adminCategories = [
  { category: "RESORT_ACCESS" as const, slug: "resort-access", title: "Resort Entry", shortTitle: "Entry", description: "Vehicle access and mountain parking", countKey: "resortAccessReservations" as const, accent: "ice" },
  { category: "LIFT_TICKET" as const, slug: "lift-tickets", title: "Lift Tickets", shortTitle: "Lifts", description: "Daily lift access reservations", countKey: "liftTicketReservations" as const, accent: "sun" },
  { category: "LESSON" as const, slug: "lessons", title: "Lessons", shortTitle: "Lessons", description: "Scheduled ski lesson places", countKey: "lessonReservations" as const, accent: "coral" },
  { category: "RENTAL" as const, slug: "rentals", title: "Rentals", shortTitle: "Rentals", description: "Equipment and sizing reservations", countKey: "rentalReservations" as const, accent: "pine" },
];

export function findAdminCategory(slug: string) {
  return adminCategories.find((item) => item.slug === slug);
}
