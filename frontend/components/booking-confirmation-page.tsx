"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { useAuth } from "@/components/auth-provider";
import { CheckIcon, MountainIcon } from "@/components/icons";
import { confirmPayment, getBooking, pendingBookingKey, type Booking } from "@/lib/booking";

const money = new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 0 });

export function BookingConfirmationPage({ bookingNumber }: { bookingNumber: string }) {
  const router = useRouter();
  const { user, accessToken, loading: authLoading } = useAuth();
  const [booking, setBooking] = useState<Booking | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authLoading && !user) router.replace(`/login?next=${encodeURIComponent(`/booking-confirmation/${bookingNumber}`)}`);
  }, [authLoading, bookingNumber, router, user]);

  useEffect(() => {
    if (!user || !accessToken) return;
    let active = true;
    confirmPayment(bookingNumber, accessToken)
      .catch(() => null)
      .then(() => getBooking(bookingNumber, accessToken))
      .then((nextBooking) => {
        if (!active) return;
        setBooking(nextBooking);
        if (nextBooking.status === "CONFIRMED") window.localStorage.removeItem(pendingBookingKey(user.id));
      })
      .catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : "We could not load this booking."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [accessToken, bookingNumber, user]);

  if (authLoading || loading || !user) return <main className="confirmation-page"><div className="shell checkout-loading"><span /><p>Confirming your mountain day…</p></div></main>;

  if (error || !booking) return <main className="confirmation-page"><div className="shell empty-cart"><MountainIcon /><p className="eyebrow">Booking status</p><h1>We couldn&apos;t load this booking.</h1><p>{error ?? "Please try again from your account."}</p><Link className="button button--ink" href="/account">Go to account</Link></div></main>;

  const confirmed = booking.status === "CONFIRMED";
  return (
    <main className="confirmation-page">
      <section className="confirmation-hero"><div className="shell confirmation-hero__inner">
        <div className={`confirmation-mark ${confirmed ? "is-confirmed" : ""}`}>{confirmed ? <CheckIcon /> : <MountainIcon />}</div>
        <p className="eyebrow">{confirmed ? "Payment confirmed" : "Payment processing"}</p>
        <h1>{confirmed ? "Your snow day is ready." : "Your payment is on its way."}</h1>
        <p>{confirmed ? `Thanks, ${booking.customerFirstName}. Everything is booked and connected under one reservation.` : "Stripe is still processing this payment. Your reservation remains safely held."}</p>
      </div></section>
      <section className="confirmation-content"><div className="shell confirmation-layout">
        <div className="confirmation-booking">
          <span>Booking reference</span><strong>{booking.bookingNumber}</strong><small>Keep this number for check-in and support.</small>
        </div>
        <div className="confirmation-receipt">
          <div className="confirmation-receipt__head"><div><span>Snow Alpine Resort</span><h2>Your itinerary</h2></div><b>{booking.status}</b></div>
          {booking.items.map((item) => <div className="confirmation-item" key={item.id}><span><small>{item.category.replaceAll("_", " ")}</small><strong>{item.productName}</strong><em>Quantity {item.quantity}</em></span><b>{money.format(item.subtotal)}</b></div>)}
          <div className="confirmation-total"><span>Total paid</span><strong>{money.format(booking.totalAmount)} AUD</strong></div>
        </div>
        <div className="confirmation-actions"><Link className="button button--ink" href="/account">View your account</Link><Link href="/">Return home</Link></div>
      </div></section>
    </main>
  );
}
