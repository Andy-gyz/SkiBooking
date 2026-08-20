"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";

import { useAuth } from "@/components/auth-provider";
import { useCart } from "@/components/cart-provider";
import { ArrowIcon, CheckIcon, MountainIcon } from "@/components/icons";
import { categories } from "@/lib/categories";
import type { CartItem } from "@/lib/cart";

const money = new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 0 });

function itemDetails(item: CartItem) {
  if (item.product.category === "RESORT_ACCESS") return [`${item.entryDate} to ${item.exitDate}`, `${item.vehicleType} · ${item.vehicleRegistration}`];
  if (item.product.category === "LIFT_TICKET") return [`Lift date · ${item.bookingDate}`, `${item.quantity} ${item.quantity === 1 ? "guest" : "guests"}`];
  if (item.product.category === "LESSON") return [`Lesson date · ${item.bookingDate}`, `${item.quantity} ${item.quantity === 1 ? "participant" : "participants"}`];
  return [`${item.rentalStartDate} to ${item.rentalEndDate}`, [item.rentalSize, item.rentalBootSize].filter(Boolean).join(" · ")];
}

export function CartPage() {
  const { user } = useAuth();
  const { cart, loading, error, changeQuantity, removeItem } = useCart();
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  async function change(item: CartItem, quantity: number) {
    setPendingId(item.id);
    setActionError(null);
    try { await changeQuantity(item, quantity); }
    catch (caught) { setActionError(caught instanceof Error ? caught.message : "Unable to update this item."); }
    finally { setPendingId(null); }
  }

  async function remove(itemId: number) {
    setPendingId(itemId);
    setActionError(null);
    try { await removeItem(itemId); }
    catch (caught) { setActionError(caught instanceof Error ? caught.message : "Unable to remove this item."); }
    finally { setPendingId(null); }
  }

  if (loading) return <main className="cart-page"><div className="shell"><div className="cart-loading"><span /><span /><span /></div></div></main>;

  if (!cart || cart.items.length === 0) return (
    <main className="cart-page">
      <div className="shell empty-cart">
        <MountainIcon />
        <p className="eyebrow">Your cart</p>
        <h1>Plenty of room<br />for a snow day.</h1>
        <p>Your cart is empty. Start with resort entry, a lift pass, a lesson or equipment for the mountain.</p>
        <Link className="button button--ink" href="/#plan">Explore booking options <ArrowIcon /></Link>
      </div>
    </main>
  );

  return (
    <main className="cart-page">
      <section className="cart-hero"><div className="shell"><p className="eyebrow">{user ? `Saved to ${user.firstName}'s account` : "Anonymous cart · saved on this device"}</p><h1>Your snow day,<br />all together.</h1><p>{cart.itemCount} {cart.itemCount === 1 ? "reservation" : "reservations"} ready to review before checkout.</p></div></section>
      <section className="cart-content"><div className="shell cart-layout">
        <div className="cart-list">
          <div className="cart-list__heading"><h2>Your selections</h2><Link href="/#plan">Add another item <ArrowIcon /></Link></div>
          {(actionError || error) && <div className="cart-error" role="alert">{actionError ?? error}</div>}
          {cart.items.map((item) => {
            const config = categories.find((category) => category.category === item.product.category)!;
            const details = itemDetails(item);
            const pending = pendingId === item.id;
            return <article className={`cart-item${pending ? " is-pending" : ""}`} key={item.id}>
              <div className="cart-item__image"><Image src={config.photo} alt="" fill sizes="150px" /></div>
              <div className="cart-item__main"><span>{config.eyebrow}</span><h3>{item.product.name}</h3><p>{details[0]}<br />{details[1]}</p><button type="button" disabled={pending} onClick={() => remove(item.id)}>Remove</button></div>
              <div className="cart-item__controls">
                {item.product.category !== "RESORT_ACCESS" && <div className="quantity-control" aria-label={`Quantity for ${item.product.name}`}><button type="button" aria-label="Decrease quantity" disabled={pending || item.quantity <= 1} onClick={() => change(item, item.quantity - 1)}>−</button><span>{item.quantity}</span><button type="button" aria-label="Increase quantity" disabled={pending || item.quantity >= 20} onClick={() => change(item, item.quantity + 1)}>+</button></div>}
                <strong>{money.format(item.subtotal)}</strong><small>{money.format(item.unitPrice)} each</small>
              </div>
            </article>;
          })}
        </div>
        <aside className="cart-summary">
          <span className="cart-summary__eyebrow">Booking summary</span>
          <div><span>Items</span><strong>{cart.itemCount}</strong></div>
          <div><span>Subtotal</span><strong>{money.format(cart.subtotal)}</strong></div>
          <div className="cart-summary__total"><span>Total</span><strong>{money.format(cart.total)} AUD</strong></div>
          <p><CheckIcon /> Prices are confirmed by the booking service.</p>
          <Link className="button button--ink" href="/checkout">Proceed to checkout <ArrowIcon /></Link>
          <small>{user ? "Your cart is attached to your account." : "You'll sign in or create an account next. This anonymous cart will stay with you."}</small>
        </aside>
      </div></section>
    </main>
  );
}
