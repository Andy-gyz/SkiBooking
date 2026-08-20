"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { useAuth } from "@/components/auth-provider";
import { ArrowIcon, MountainIcon } from "@/components/icons";
import { getBooking, pendingBookingKey, type Booking, type BookingItem } from "@/lib/booking";

const money = new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 0 });
const date = new Intl.DateTimeFormat("en-AU", { day: "numeric", month: "long", year: "numeric" });

function detailLines(item: BookingItem) {
  const lines: string[] = [];
  if (item.bookingDate) lines.push(date.format(new Date(`${item.bookingDate}T00:00:00`)));
  if (item.entryDate) lines.push(`${date.format(new Date(`${item.entryDate}T00:00:00`))}${item.exitDate ? ` – ${date.format(new Date(`${item.exitDate}T00:00:00`))}` : ""}`);
  if (item.vehicleRegistration) lines.push(`${item.vehicleType ?? "Vehicle"} · ${item.vehicleRegistration}`);
  if (item.rentalStartDate) lines.push(`${date.format(new Date(`${item.rentalStartDate}T00:00:00`))}${item.rentalEndDate ? ` – ${date.format(new Date(`${item.rentalEndDate}T00:00:00`))}` : ""}`);
  if (item.rentalSize) lines.push(item.rentalSize);
  if (item.rentalBootSize) lines.push(`Boot ${item.rentalBootSize}`);
  return lines;
}

export function BookingDetailPage({ bookingNumber }: { bookingNumber: string }) {
  const router = useRouter();
  const { user, accessToken, loading: authLoading } = useAuth();
  const [booking, setBooking] = useState<Booking | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authLoading && !user) router.replace(`/login?next=${encodeURIComponent(`/bookings/${bookingNumber}`)}`);
  }, [authLoading, bookingNumber, router, user]);

  useEffect(() => {
    if (!user || !accessToken) return;
    let active = true;
    getBooking(bookingNumber, accessToken)
      .then((result) => { if (active) setBooking(result); })
      .catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : "We could not load this booking."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [accessToken, bookingNumber, user]);

  function continuePayment() {
    if (!user || !booking) return;
    window.localStorage.setItem(pendingBookingKey(user.id), booking.bookingNumber);
    router.push("/checkout");
  }

  if (authLoading || loading || !user) return <main className="booking-detail-page"><div className="shell checkout-loading"><span /><p>Opening your booking…</p></div></main>;
  if (error || !booking) return <main className="booking-detail-page"><div className="shell empty-cart"><MountainIcon /><p className="eyebrow">Booking detail</p><h1>We couldn&apos;t find this booking.</h1><p>{error ?? "It may not belong to this account."}</p><Link className="button button--ink" href="/account">Back to My Bookings</Link></div></main>;

  return (
    <main className="booking-detail-page">
      <section className="booking-detail-hero"><div className="shell">
        <nav className="booking-detail-breadcrumb"><Link href="/account">My Bookings</Link><span>/</span><span>{booking.bookingNumber}</span></nav>
        <span className={`booking-status booking-status--${booking.status.toLowerCase()}`}>{booking.status}</span>
        <h1>{booking.status === "CONFIRMED" ? "Your mountain day." : booking.status === "PENDING" ? "Payment still pending." : "This booking was cancelled."}</h1>
        <p>Created {date.format(new Date(booking.createdAt))} · {booking.bookingNumber}</p>
      </div></section>
      <section className="booking-detail-content"><div className="shell booking-detail-layout">
        <div className="booking-detail-main">
          <div className="booking-detail-heading"><span>01</span><div><h2>Your itinerary</h2><p>{booking.items.length} reserved {booking.items.length === 1 ? "product" : "products"}</p></div></div>
          <div className="booking-detail-card booking-detail-items">
            {booking.items.map((item) => <article key={item.id}><div><small>{item.category.replaceAll("_", " ")}</small><h3>{item.productName}</h3>{detailLines(item).map((line) => <p key={line}>{line}</p>)}</div><span><em>Qty {item.quantity}</em><strong>{money.format(item.subtotal)}</strong></span></article>)}
          </div>
          <div className="booking-detail-heading"><span>02</span><div><h2>Guest details</h2><p>Information attached to this reservation</p></div></div>
          <div className="booking-detail-card booking-guest-details"><div><small>Guest</small><strong>{booking.customerFirstName} {booking.customerLastName}</strong></div><div><small>Email</small><strong>{booking.customerEmail}</strong></div><div><small>Phone</small><strong>{booking.customerPhone || "Not provided"}</strong></div></div>
        </div>
        <aside className="booking-detail-summary"><span>Booking total</span><strong>{money.format(booking.totalAmount)} <small>{booking.currency}</small></strong><p>Reference<br /><b>{booking.bookingNumber}</b></p>{booking.status === "PENDING" && <button className="button" type="button" onClick={continuePayment}>Continue secure payment <ArrowIcon /></button>}<Link href="/account">← Back to My Bookings</Link></aside>
      </div></section>
    </main>
  );
}
