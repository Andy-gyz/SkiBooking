"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { useAuth } from "@/components/auth-provider";
import { useCart } from "@/components/cart-provider";
import { ArrowIcon, CheckIcon, MountainIcon } from "@/components/icons";

const money = new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 0 });

export function CheckoutPage() {
  const router = useRouter();
  const { user, cartId, loading: authLoading } = useAuth();
  const { cart, loading: cartLoading } = useCart();

  useEffect(() => {
    if (!authLoading && !user) router.replace("/login?next=%2Fcheckout");
  }, [authLoading, router, user]);

  const cartIsSyncing = Boolean(user && cartId && cart?.id !== cartId);
  if (authLoading || cartLoading || !user || cartIsSyncing) return (
    <main className="checkout-page"><div className="shell checkout-loading"><span /><p>Preparing your secure checkout…</p></div></main>
  );

  if (!cart || cart.items.length === 0) return (
    <main className="checkout-page"><div className="shell empty-cart"><MountainIcon /><p className="eyebrow">Checkout</p><h1>Your cart is empty.</h1><p>Add your mountain essentials before continuing to checkout.</p><Link className="button button--ink" href="/#plan">Build your snow day <ArrowIcon /></Link></div></main>
  );

  return (
    <main className="checkout-page">
      <section className="checkout-hero"><div className="shell"><p className="eyebrow">Secure checkout</p><h1>Review every detail.<br />Then you&apos;re ready.</h1><p>Signed in as {user.email}. Your anonymous selections are now attached to your account.</p></div></section>
      <section className="checkout-content"><div className="shell checkout-layout">
        <div className="checkout-main">
          <div className="checkout-section-heading"><span>01</span><div><h2>Customer details</h2><p>These details will appear on your booking.</p></div></div>
          <div className="checkout-card checkout-details">
            <label><span>First name</span><input defaultValue={user.firstName} /></label>
            <label><span>Last name</span><input defaultValue={user.lastName} /></label>
            <label><span>Email address</span><input type="email" defaultValue={user.email} /></label>
            <label><span>Phone <small>Optional</small></span><input type="tel" defaultValue={user.phone ?? ""} /></label>
          </div>

          <div className="checkout-section-heading"><span>02</span><div><h2>Booking review</h2><p>All prices come from the booking service.</p></div></div>
          <div className="checkout-card checkout-items">
            {cart.items.map((item) => <div key={item.id}><span><small>{item.product.category.replaceAll("_", " ")}</small><strong>{item.product.name}</strong><em>Quantity {item.quantity}</em></span><b>{money.format(item.subtotal)}</b></div>)}
          </div>
        </div>

        <aside className="checkout-summary">
          <span className="cart-summary__eyebrow">Order total</span>
          <div><span>{cart.itemCount} items</span><strong>{money.format(cart.subtotal)}</strong></div>
          <div className="checkout-summary__total"><span>Total</span><strong>{money.format(cart.total)} AUD</strong></div>
          <p><CheckIcon /> Account and cart verified</p>
          <button className="button button--ink" type="button" disabled>Secure payment in Milestone 12 <ArrowIcon /></button>
          <small>No payment details are collected yet. Stripe payment is the next frontend milestone.</small>
          <Link href="/cart">← Edit your cart</Link>
        </aside>
      </div></section>
    </main>
  );
}
