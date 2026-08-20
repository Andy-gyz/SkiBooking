"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { AdminRouteState, AdminShell } from "@/components/admin-shell";
import { useAuth } from "@/components/auth-provider";
import { getAdminBooking, type AdminBooking } from "@/lib/admin";
import type { BookingItem } from "@/lib/booking";

const money = new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 0 });
const date = new Intl.DateTimeFormat("en-AU", { day: "numeric", month: "long", year: "numeric" });
const dateTime = new Intl.DateTimeFormat("en-AU", { day: "numeric", month: "short", year: "numeric", hour: "numeric", minute: "2-digit" });

function itemFacts(item: BookingItem) {
  return [
    item.bookingDate && date.format(new Date(`${item.bookingDate}T00:00:00`)),
    item.entryDate && `Entry ${date.format(new Date(`${item.entryDate}T00:00:00`))}`,
    item.exitDate && `Exit ${date.format(new Date(`${item.exitDate}T00:00:00`))}`,
    item.vehicleRegistration && `${item.vehicleType ?? "Vehicle"} · ${item.vehicleRegistration}`,
    item.rentalStartDate && `From ${date.format(new Date(`${item.rentalStartDate}T00:00:00`))}`,
    item.rentalEndDate && `To ${date.format(new Date(`${item.rentalEndDate}T00:00:00`))}`,
    item.rentalSize,
    item.rentalBootSize && `Boot ${item.rentalBootSize}`,
    item.lessonSessionId && `Session #${item.lessonSessionId}`,
  ].filter(Boolean) as string[];
}

export function AdminBookingPage({ bookingId }: { bookingId: number }) {
  const { user, accessToken, loading: authLoading } = useAuth();
  const [booking, setBooking] = useState<AdminBooking | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isInteger(bookingId) || bookingId <= 0 || !accessToken || user?.role !== "ADMIN") return;
    let active = true;
    getAdminBooking(bookingId, accessToken)
      .then((result) => { if (active) setBooking(result); })
      .catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : "We could not load this booking."); });
    return () => { active = false; };
  }, [accessToken, bookingId, user]);

  if (!Number.isInteger(bookingId) || bookingId <= 0) return <AdminRouteState kind="error" message="The booking ID is invalid." />;
  if (authLoading) return <AdminRouteState kind="loading" />;
  if (!user || !accessToken) return <AdminRouteState kind="signed-out" />;
  if (user.role !== "ADMIN") return <AdminRouteState kind="forbidden" />;
  if (error) return <AdminRouteState kind="error" message={error} />;
  if (!booking) return <AdminRouteState kind="loading" />;

  return (
    <AdminShell eyebrow="Booking record" title={booking.bookingNumber} description={`Created ${dateTime.format(new Date(booking.createdAt))}. Internal booking ID ${booking.id}.`}>
      <div className="admin-booking-top"><span className={`booking-status booking-status--${booking.status.toLowerCase()}`}>{booking.status}</span><strong>{money.format(booking.totalAmount)} <small>{booking.currency}</small></strong><Link href="/admin">← Dashboard</Link></div>
      <div className="admin-booking-layout">
        <div className="admin-booking-main">
          <section className="admin-panel"><div className="admin-panel__heading"><span>Guest</span><h2>Customer snapshot</h2></div><div className="admin-customer-grid"><div><small>Name</small><strong>{booking.customerFirstName} {booking.customerLastName}</strong></div><div><small>Email</small><strong>{booking.customerEmail}</strong></div><div><small>Phone</small><strong>{booking.customerPhone || "Not provided"}</strong></div></div></section>
          <section className="admin-panel"><div className="admin-panel__heading"><span>Itinerary</span><h2>Reserved products</h2></div><div className="admin-booking-items">{booking.items.map((item) => <article key={item.id}><div><small>{item.category.replaceAll("_", " ")}</small><h3>{item.productName}</h3>{itemFacts(item).map((fact) => <p key={fact}>{fact}</p>)}</div><span><em>Qty {item.quantity} × {money.format(item.unitPrice)}</em><strong>{money.format(item.subtotal)}</strong></span></article>)}</div></section>
        </div>
        <aside className="admin-payment-panel"><span>Payment history</span><h2>{booking.payments.length} {booking.payments.length === 1 ? "attempt" : "attempts"}</h2>{booking.payments.length === 0 ? <p>No Stripe payment has been created.</p> : booking.payments.map((payment) => <div className="admin-payment-record" key={payment.id}><span className={`admin-payment admin-payment--${payment.status.toLowerCase()}`}>{payment.status}</span><strong>{money.format(payment.amount)} {payment.currency}</strong><small>{payment.paymentMethod || "Payment method pending"}</small><p>{payment.paidAt ? `Paid ${dateTime.format(new Date(payment.paidAt))}` : `Created ${dateTime.format(new Date(payment.createdAt))}`}</p><code>{payment.stripePaymentId || "No Stripe ID"}</code></div>)}</aside>
      </div>
    </AdminShell>
  );
}
