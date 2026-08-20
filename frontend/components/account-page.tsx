"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { useAuth } from "@/components/auth-provider";
import { useCart } from "@/components/cart-provider";
import { ArrowIcon, MountainIcon } from "@/components/icons";
import { getMyBookings, type BookingSummary } from "@/lib/booking";

const money = new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 0 });
const date = new Intl.DateTimeFormat("en-AU", { day: "numeric", month: "short", year: "numeric" });

export function AccountPage() {
  const router = useRouter();
  const { user, accessToken, loading, logout } = useAuth();
  const { cart } = useCart();
  const [pending, setPending] = useState(false);
  const [bookings, setBookings] = useState<BookingSummary[]>([]);
  const [bookingsLoading, setBookingsLoading] = useState(true);
  const [bookingsError, setBookingsError] = useState<string | null>(null);

  useEffect(() => {
    if (loading) return;
    if (!accessToken) {
      queueMicrotask(() => setBookingsLoading(false));
      return;
    }
    let active = true;
    queueMicrotask(() => {
      if (!active) return;
      setBookingsLoading(true);
      setBookingsError(null);
    });
    getMyBookings(accessToken)
      .then((result) => { if (active) setBookings(result); })
      .catch((caught) => { if (active) setBookingsError(caught instanceof Error ? caught.message : "We could not load your bookings."); })
      .finally(() => { if (active) setBookingsLoading(false); });
    return () => { active = false; };
  }, [accessToken, loading]);

  async function signOut() {
    setPending(true);
    await logout();
    router.replace("/");
  }

  if (loading) return <main className="account-page"><div className="shell account-loading">Loading your account…</div></main>;

  if (!user) return (
    <main className="account-page"><div className="shell empty-cart"><MountainIcon /><p className="eyebrow">Your account</p><h1>Sign in for your snow days.</h1><p>Access checkout and keep your booking details connected to one account.</p><Link className="button button--ink" href="/login">Sign in <ArrowIcon /></Link></div></main>
  );

  const hasCart = Boolean(cart?.items.length);

  return (
    <main className="account-page">
      <section className="account-hero"><div className="shell"><p className="eyebrow">Customer account</p><h1>Hi, {user.firstName}.<br />Your snow days live here.</h1></div></section>
      <section className="account-content"><div className="shell account-grid">
        <div className="account-card"><span>Profile</span><h2>{user.firstName} {user.lastName}</h2><p>{user.email}<br />{user.phone || "No phone number added"}</p><small>Role · {user.role}</small></div>
        <div className="account-actions-card"><span>{hasCart ? "Active cart" : "Plan another visit"}</span><h2>{hasCart ? `${cart?.itemCount} items are ready.` : "The mountain is waiting."}</h2><p>{hasCart ? "Your selected products are saved and ready for secure checkout." : "Build a new visit from resort entry, lift passes, lessons and rentals."}</p><Link className="button button--ink" href={hasCart ? "/checkout" : "/#plan"}>{hasCart ? "Continue to checkout" : "Start a new booking"} <ArrowIcon /></Link>{hasCart && <Link href="/cart">Review cart</Link>}</div>

        <section className="my-bookings" aria-labelledby="my-bookings-title">
          <div className="my-bookings__heading"><div><p className="eyebrow">Your history</p><h2 id="my-bookings-title">My bookings</h2></div>{!bookingsLoading && !bookingsError && <span>{bookings.length} {bookings.length === 1 ? "booking" : "bookings"}</span>}</div>
          {bookingsLoading ? <div className="bookings-state"><span className="bookings-state__spinner" /><p>Gathering your mountain days…</p></div> : bookingsError ? <div className="bookings-state bookings-state--error"><strong>Bookings are temporarily unavailable.</strong><p>{bookingsError}</p></div> : bookings.length === 0 ? <div className="bookings-empty"><MountainIcon /><div><h3>Your first snow day starts here.</h3><p>Completed and pending reservations will appear in this account.</p></div><Link className="button button--ink" href="/#plan">Explore bookings <ArrowIcon /></Link></div> : <div className="booking-list">
            {bookings.map((booking) => <Link className="booking-row" href={`/bookings/${encodeURIComponent(booking.bookingNumber)}`} key={booking.bookingNumber}>
              <span className={`booking-status booking-status--${booking.status.toLowerCase()}`}>{booking.status}</span>
              <span className="booking-row__reference"><small>Booking reference</small><strong>{booking.bookingNumber}</strong></span>
              <span><small>Booked</small><strong>{date.format(new Date(booking.createdAt))}</strong></span>
              <span><small>Items</small><strong>{booking.itemCount}</strong></span>
              <span className="booking-row__total"><small>Total</small><strong>{money.format(booking.totalAmount)}</strong></span>
              <ArrowIcon />
            </Link>)}
          </div>}
        </section>
        <button className="account-logout" type="button" onClick={signOut} disabled={pending}>{pending ? "Signing out…" : "Sign out"}</button>
      </div></section>
    </main>
  );
}
