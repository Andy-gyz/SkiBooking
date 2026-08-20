"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { AdminRouteState, AdminShell } from "@/components/admin-shell";
import { ArrowIcon } from "@/components/icons";
import { useAuth } from "@/components/auth-provider";
import { findAdminCategory, getAdminReservations, type AdminReservation } from "@/lib/admin";
import type { BookingItem } from "@/lib/booking";

const money = new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 0 });
const date = new Intl.DateTimeFormat("en-AU", { day: "numeric", month: "short", year: "numeric" });

function reservationDetail(item: BookingItem) {
  if (item.category === "RESORT_ACCESS") return [item.entryDate && date.format(new Date(`${item.entryDate}T00:00:00`)), item.vehicleRegistration].filter(Boolean).join(" · ");
  if (item.category === "RENTAL") return [item.rentalStartDate && date.format(new Date(`${item.rentalStartDate}T00:00:00`)), item.rentalSize, item.rentalBootSize && `Boot ${item.rentalBootSize}`].filter(Boolean).join(" · ");
  return item.bookingDate ? date.format(new Date(`${item.bookingDate}T00:00:00`)) : "Date not set";
}

export function AdminReservationsPage({ categorySlug }: { categorySlug: string }) {
  const category = findAdminCategory(categorySlug);
  const { user, accessToken, loading: authLoading } = useAuth();
  const [reservations, setReservations] = useState<AdminReservation[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!category || !accessToken || user?.role !== "ADMIN") return;
    let active = true;
    getAdminReservations(category.category, accessToken)
      .then((result) => { if (active) setReservations(result); })
      .catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : "We could not load reservations."); });
    return () => { active = false; };
  }, [accessToken, category, user]);

  if (!category) return <AdminRouteState kind="error" message="This reservation category does not exist." />;
  if (authLoading) return <AdminRouteState kind="loading" />;
  if (!user || !accessToken) return <AdminRouteState kind="signed-out" />;
  if (user.role !== "ADMIN") return <AdminRouteState kind="forbidden" />;
  if (error) return <AdminRouteState kind="error" message={error} />;
  if (!reservations) return <AdminRouteState kind="loading" />;

  const quantity = reservations.reduce((sum, reservation) => sum + reservation.item.quantity, 0);

  return (
    <AdminShell eyebrow="Reservations" title={category.title} description={`${category.description}. Confirmed operational records only.`}>
      <div className="admin-list-summary"><div><span>Reserved quantity</span><strong>{quantity}</strong></div><div><span>Reservation rows</span><strong>{reservations.length}</strong></div><Link href="/admin">← Overview</Link></div>
      {reservations.length === 0 ? <div className="admin-empty"><span>0</span><h2>No confirmed reservations yet.</h2><p>This category will update automatically after a Stripe payment is confirmed.</p></div> : <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Status</th><th>Guest</th><th>Product &amp; selection</th><th>Qty</th><th>Subtotal</th><th>Payment</th><th /></tr></thead><tbody>{reservations.map((reservation) => <tr key={`${reservation.bookingId}-${reservation.item.id}`}>
        <td><span className={`booking-status booking-status--${reservation.bookingStatus.toLowerCase()}`}>{reservation.bookingStatus}</span></td>
        <td><strong>{reservation.customerFirstName} {reservation.customerLastName}</strong><small>{reservation.customerEmail}</small><em>{reservation.bookingNumber}</em></td>
        <td><strong>{reservation.item.productName}</strong><small>{reservationDetail(reservation.item)}</small></td>
        <td><b>{reservation.item.quantity}</b></td><td><b>{money.format(reservation.item.subtotal)}</b></td>
        <td><span className={`admin-payment admin-payment--${reservation.paymentStatus?.toLowerCase() ?? "none"}`}>{reservation.paymentStatus ?? "NONE"}</span></td>
        <td><Link aria-label={`Open booking ${reservation.bookingNumber}`} href={`/admin/bookings/${reservation.bookingId}`}><ArrowIcon /></Link></td>
      </tr>)}</tbody></table></div>}
    </AdminShell>
  );
}
