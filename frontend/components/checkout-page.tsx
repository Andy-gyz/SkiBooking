"use client";

import { Elements, PaymentElement, useElements, useStripe } from "@stripe/react-stripe-js";
import { loadStripe } from "@stripe/stripe-js";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";

import { useAuth } from "@/components/auth-provider";
import { useCart } from "@/components/cart-provider";
import { ArrowIcon, CheckIcon, MountainIcon } from "@/components/icons";
import { createBooking, createPayment, getBooking, pendingBookingKey, confirmPayment as reconcilePayment, type Booking, type Payment } from "@/lib/booking";

const money = new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 0 });
const publishableKey = process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY ?? "";
const stripePromise = publishableKey ? loadStripe(publishableKey) : null;

function StripePaymentForm({ bookingNumber }: { bookingNumber: string }) {
  const stripe = useStripe();
  const elements = useElements();
  const router = useRouter();
  const { accessToken } = useAuth();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!stripe || !elements || !accessToken) return;
    setSubmitting(true);
    setError(null);
    const result = await stripe.confirmPayment({
      elements,
      confirmParams: { return_url: `${window.location.origin}/booking-confirmation/${encodeURIComponent(bookingNumber)}` },
      redirect: "if_required",
    });
    if (result.error) {
      setError(result.error.message ?? "Your payment could not be completed. Please check the card details and try again.");
      setSubmitting(false);
      return;
    }
    try {
      const payment = await reconcilePayment(bookingNumber, accessToken);
      if (payment.paymentStatus === "SUCCEEDED" || payment.bookingStatus === "CONFIRMED") {
        router.push(`/booking-confirmation/${encodeURIComponent(bookingNumber)}`);
      } else {
        setError("Stripe is still processing this payment. You can safely refresh the confirmation in a moment.");
        setSubmitting(false);
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "We could not confirm the payment status.");
      setSubmitting(false);
    }
  }

  return (
    <form className="stripe-payment-form" onSubmit={submit}>
      <PaymentElement options={{ layout: "tabs" }} />
      {error && <p className="checkout-error" role="alert">{error}</p>}
      <button className="button button--ink" type="submit" disabled={!stripe || !elements || submitting}>
        {submitting ? "Confirming payment…" : "Pay securely"} {!submitting && <ArrowIcon />}
      </button>
      <small>Payments are encrypted and processed by Stripe. Snow Alpine never stores your card details.</small>
    </form>
  );
}

export function CheckoutPage() {
  const router = useRouter();
  const { user, accessToken, cartId, loading: authLoading } = useAuth();
  const { cart, loading: cartLoading, markCheckedOut } = useCart();
  const [booking, setBooking] = useState<Booking | null>(null);
  const [payment, setPayment] = useState<Payment | null>(null);
  const [restoring, setRestoring] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authLoading && !user) router.replace("/login?next=%2Fcheckout");
  }, [authLoading, router, user]);

  useEffect(() => {
    if (!user || !accessToken) return;
    let active = true;
    const storedBookingNumber = window.localStorage.getItem(pendingBookingKey(user.id));
    if (!storedBookingNumber) {
      queueMicrotask(() => { if (active) setRestoring(false); });
      return () => { active = false; };
    }
    getBooking(storedBookingNumber, accessToken)
      .then(async (storedBooking) => {
        if (!active) return;
        if (storedBooking.status === "CONFIRMED") {
          router.replace(`/booking-confirmation/${encodeURIComponent(storedBooking.bookingNumber)}`);
          return;
        }
        setBooking(storedBooking);
        markCheckedOut();
        const storedPayment = await createPayment(storedBooking.bookingNumber, accessToken);
        if (active) setPayment(storedPayment);
      })
      .catch((caught) => {
        if (!active) return;
        setError(caught instanceof Error ? caught.message : "We could not resume your pending payment.");
      })
      .finally(() => { if (active) setRestoring(false); });
    return () => { active = false; };
  }, [accessToken, markCheckedOut, router, user]);

  async function beginPayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!user || !accessToken || !cartId || !cart) return;
    const data = new FormData(event.currentTarget);
    setStarting(true);
    setError(null);
    try {
      const createdBooking = await createBooking({
        cartId,
        firstName: String(data.get("firstName") ?? "").trim(),
        lastName: String(data.get("lastName") ?? "").trim(),
        email: String(data.get("email") ?? "").trim(),
        phone: String(data.get("phone") ?? "").trim() || undefined,
      }, accessToken);
      setBooking(createdBooking);
      window.localStorage.setItem(pendingBookingKey(user.id), createdBooking.bookingNumber);
      markCheckedOut();
      setPayment(await createPayment(createdBooking.bookingNumber, accessToken));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "We could not start secure payment.");
    } finally {
      setStarting(false);
    }
  }

  async function retryPayment() {
    if (!booking || !accessToken) return;
    setStarting(true);
    setError(null);
    try { setPayment(await createPayment(booking.bookingNumber, accessToken)); }
    catch (caught) { setError(caught instanceof Error ? caught.message : "We could not reconnect to Stripe."); }
    finally { setStarting(false); }
  }

  const cartIsSyncing = Boolean(user && cartId && cart?.id !== cartId);
  if (authLoading || cartLoading || restoring || !user || cartIsSyncing) return (
    <main className="checkout-page"><div className="shell checkout-loading"><span /><p>Preparing your secure checkout…</p></div></main>
  );

  if (!cart && !booking) return (
    <main className="checkout-page"><div className="shell empty-cart"><MountainIcon /><p className="eyebrow">Checkout</p><h1>Your cart is empty.</h1><p>Add your mountain essentials before continuing to checkout.</p><Link className="button button--ink" href="/#plan">Build your snow day <ArrowIcon /></Link></div></main>
  );

  const items = booking?.items ?? cart?.items ?? [];
  const itemCount = items.reduce((total, item) => total + item.quantity, 0);
  const total = booking?.totalAmount ?? cart?.total ?? 0;

  return (
    <main className="checkout-page">
      <section className="checkout-hero"><div className="shell"><p className="eyebrow">Secure checkout</p><h1>Review every detail.<br />Then you&apos;re ready.</h1><p>Signed in as {user.email}. Your booking and payment are protected by Snow Alpine and Stripe.</p></div></section>
      <section className="checkout-content"><div className="shell checkout-layout">
        <div className="checkout-main">
          <form className="checkout-booking-form" id="booking-form" onSubmit={beginPayment}>
          <div className="checkout-section-heading"><span>01</span><div><h2>Customer details</h2><p>These details will appear on your booking.</p></div></div>
          <div className="checkout-card checkout-details">
            <label><span>First name</span><input name="firstName" defaultValue={booking?.customerFirstName ?? user.firstName} required maxLength={100} disabled={Boolean(booking)} /></label>
            <label><span>Last name</span><input name="lastName" defaultValue={booking?.customerLastName ?? user.lastName} required maxLength={100} disabled={Boolean(booking)} /></label>
            <label><span>Email address</span><input name="email" type="email" defaultValue={booking?.customerEmail ?? user.email} required maxLength={255} disabled={Boolean(booking)} /></label>
            <label><span>Phone <small>Optional</small></span><input name="phone" type="tel" defaultValue={booking?.customerPhone ?? user.phone ?? ""} maxLength={30} disabled={Boolean(booking)} /></label>
          </div>

          <div className="checkout-section-heading"><span>02</span><div><h2>Booking review</h2><p>All prices come from the booking service.</p></div></div>
          <div className="checkout-card checkout-items">
            {items.map((item) => <div key={item.id}><span><small>{("product" in item ? item.product.category : item.category).replaceAll("_", " ")}</small><strong>{"product" in item ? item.product.name : item.productName}</strong><em>Quantity {item.quantity}</em></span><b>{money.format(item.subtotal)}</b></div>)}
          </div>

          </form>
          {booking && <>
            <div className="checkout-section-heading"><span>03</span><div><h2>Secure payment</h2><p>Booking {booking.bookingNumber}</p></div></div>
            <div className="checkout-card checkout-payment-card">
              {!publishableKey && <p className="checkout-error" role="alert">Stripe&apos;s publishable key is missing. Restart the frontend after loading the project .env file.</p>}
              {publishableKey && payment?.clientSecret && stripePromise && (
                <Elements stripe={stripePromise} options={{
                  clientSecret: payment.clientSecret,
                  appearance: { theme: "stripe", variables: { colorPrimary: "#3048e8", borderRadius: "13px", fontFamily: "Arial, Helvetica, sans-serif", colorText: "#101116" } },
                }}>
                  <StripePaymentForm bookingNumber={booking.bookingNumber} />
                </Elements>
              )}
              {!payment?.clientSecret && publishableKey && <button className="button button--ink" type="button" onClick={retryPayment} disabled={starting}>{starting ? "Connecting to Stripe…" : "Retry secure payment"}</button>}
            </div>
          </>}
        </div>

        <aside className="checkout-summary">
          <span className="cart-summary__eyebrow">Order total</span>
          <div><span>{itemCount} items</span><strong>{money.format(total)}</strong></div>
          <div className="checkout-summary__total"><span>Total</span><strong>{money.format(total)} AUD</strong></div>
          <p><CheckIcon /> {booking ? `Booking ${booking.bookingNumber}` : "Account and cart verified"}</p>
          {!booking && <button className="button button--ink" type="submit" form="booking-form" disabled={starting}>{starting ? "Preparing payment…" : "Continue to secure payment"} {!starting && <ArrowIcon />}</button>}
          <small>{booking ? "Your order is reserved while payment is pending." : "Your booking is created only after you continue. You can review the total before entering a card."}</small>
          {!booking && <Link href="/cart">← Edit your cart</Link>}
        </aside>
        {error && <p className="checkout-error checkout-error--layout" role="alert">{error}</p>}
      </div></section>
    </main>
  );
}
