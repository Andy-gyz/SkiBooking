"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";

import { useCart } from "@/components/cart-provider";
import { ArrowIcon, CheckIcon } from "@/components/icons";
import { getLessonSessions, type CartItemRequest, type LessonSession } from "@/lib/cart";
import type { Product } from "@/lib/catalog";

const defaultDate = "2026-08-25";

export function ProductConfigurator({ product }: { product: Product }) {
  const { addItem } = useCart();
  const [open, setOpen] = useState(false);
  const [quantity, setQuantity] = useState(1);
  const [bookingDate, setBookingDate] = useState(defaultDate);
  const [entryDate, setEntryDate] = useState(defaultDate);
  const [exitDate, setExitDate] = useState(defaultDate);
  const [vehicleRegistration, setVehicleRegistration] = useState("");
  const [vehicleType, setVehicleType] = useState("SUV");
  const [rentalStartDate, setRentalStartDate] = useState(defaultDate);
  const [rentalEndDate, setRentalEndDate] = useState(defaultDate);
  const [rentalSize, setRentalSize] = useState("Adult Medium");
  const [rentalBootSize, setRentalBootSize] = useState("");
  const [sessions, setSessions] = useState<LessonSession[]>([]);
  const [lessonSessionId, setLessonSessionId] = useState("");
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || product.category !== "LESSON" || !bookingDate) return;
    let active = true;
    Promise.resolve().then(() => {
      if (!active) return [];
      setSessionsLoading(true);
      setLessonSessionId("");
      return getLessonSessions(product.id, bookingDate);
    })
      .then((available) => {
        if (!active) return;
        setSessions(available);
        const firstAvailable = available.find((session) => session.availableCount > 0);
        setLessonSessionId(firstAvailable ? String(firstAvailable.id) : "");
      })
      .catch((caught) => { if (active) setFormError(caught instanceof Error ? caught.message : "Unable to load lesson times."); })
      .finally(() => { if (active) setSessionsLoading(false); });
    return () => { active = false; };
  }, [bookingDate, open, product.category, product.id]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setMessage(null);
    setFormError(null);

    const request: CartItemRequest = { productId: product.id, quantity };
    if (product.category === "RESORT_ACCESS") Object.assign(request, { vehicleRegistration, vehicleType, entryDate, exitDate });
    if (product.category === "LIFT_TICKET") request.bookingDate = bookingDate;
    if (product.category === "LESSON") request.lessonSessionId = Number(lessonSessionId);
    if (product.category === "RENTAL") Object.assign(request, { rentalStartDate, rentalEndDate, rentalSize, rentalBootSize: rentalBootSize || undefined });

    try {
      await addItem(request);
      setMessage(`${product.name} was added to your cart.`);
    } catch (caught) {
      setFormError(caught instanceof Error ? caught.message : "Unable to add this item.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={`configurator${open ? " configurator--open" : ""}`}>
      {!open ? (
        <button className="button button--ink configure-button" type="button" onClick={() => setOpen(true)}>Select options <ArrowIcon /></button>
      ) : (
        <form className="product-configurator" onSubmit={submit}>
          <div className="configurator__heading"><div><span>Configure your booking</span><strong>{product.name}</strong></div><button type="button" onClick={() => setOpen(false)} aria-label="Close product options">×</button></div>

          {product.category === "RESORT_ACCESS" && <div className="form-grid">
            <label><span>Entry date</span><input type="date" required value={entryDate} min="2026-08-20" onChange={(event) => setEntryDate(event.target.value)} /></label>
            <label><span>Exit date</span><input type="date" required value={exitDate} min={entryDate} onChange={(event) => setExitDate(event.target.value)} /></label>
            <label><span>Vehicle registration</span><input required maxLength={30} placeholder="ABC123" value={vehicleRegistration} onChange={(event) => setVehicleRegistration(event.target.value.toUpperCase())} /></label>
            <label><span>Vehicle type</span><select value={vehicleType} onChange={(event) => setVehicleType(event.target.value)}><option>Sedan</option><option>SUV</option><option>4WD</option><option>Van</option><option>Motorcycle</option></select></label>
          </div>}

          {product.category === "LIFT_TICKET" && <div className="form-grid form-grid--compact">
            <label><span>Lift date</span><input type="date" required value={bookingDate} min="2026-08-20" onChange={(event) => setBookingDate(event.target.value)} /></label>
            <QuantityField quantity={quantity} setQuantity={setQuantity} />
          </div>}

          {product.category === "LESSON" && <div className="form-grid form-grid--compact">
            <label><span>Lesson date</span><input type="date" required value={bookingDate} min="2026-08-20" onChange={(event) => setBookingDate(event.target.value)} /></label>
            <QuantityField quantity={quantity} setQuantity={setQuantity} />
            <label className="form-grid__full"><span>Available session</span><select required disabled={sessionsLoading || sessions.length === 0} value={lessonSessionId} onChange={(event) => setLessonSessionId(event.target.value)}><option value="">{sessionsLoading ? "Loading times…" : sessions.length === 0 ? "No sessions available" : "Choose a session"}</option>{sessions.map((session) => <option value={session.id} disabled={session.availableCount === 0} key={session.id}>{session.startTime.slice(0, 5)}–{session.endTime.slice(0, 5)} · {session.availableCount} spots left</option>)}</select></label>
          </div>}

          {product.category === "RENTAL" && <div className="form-grid">
            <label><span>Collection date</span><input type="date" required value={rentalStartDate} min="2026-08-20" onChange={(event) => setRentalStartDate(event.target.value)} /></label>
            <label><span>Return date</span><input type="date" required value={rentalEndDate} min={rentalStartDate} onChange={(event) => setRentalEndDate(event.target.value)} /></label>
            <label><span>Package size</span><select value={rentalSize} onChange={(event) => setRentalSize(event.target.value)}><option>Child Small</option><option>Child Large</option><option>Adult Small</option><option>Adult Medium</option><option>Adult Large</option><option>Adult XL</option></select></label>
            <label><span>Boot size (optional)</span><input maxLength={30} placeholder="e.g. AU 9" value={rentalBootSize} onChange={(event) => setRentalBootSize(event.target.value)} /></label>
            <QuantityField quantity={quantity} setQuantity={setQuantity} />
          </div>}

          {product.category === "RESORT_ACCESS" && <input type="hidden" value={quantity} readOnly />}
          {formError && <p className="form-message form-message--error" role="alert">{formError}</p>}
          {message && <p className="form-message form-message--success"><CheckIcon /> {message} <Link href="/cart">View cart</Link></p>}
          <div className="configurator__actions"><span>${product.price * quantity} AUD</span><button className="button button--ink" disabled={submitting || (product.category === "LESSON" && !lessonSessionId)} type="submit">{submitting ? "Adding…" : "Add to cart"} <ArrowIcon /></button></div>
        </form>
      )}
    </div>
  );
}

function QuantityField({ quantity, setQuantity }: { quantity: number; setQuantity: (quantity: number) => void }) {
  return <label><span>Quantity</span><select value={quantity} onChange={(event) => setQuantity(Number(event.target.value))}>{Array.from({ length: 10 }, (_, index) => index + 1).map((value) => <option value={value} key={value}>{value}</option>)}</select></label>;
}
