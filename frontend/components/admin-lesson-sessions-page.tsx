"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";

import { AdminRouteState, AdminShell } from "@/components/admin-shell";
import { ArrowIcon } from "@/components/icons";
import { useAuth } from "@/components/auth-provider";
import { createAdminLessonSession, generateAdminLessonSessions, getAdminLessonSessions, getAdminProducts, updateAdminLessonSession, type AdminLessonSession, type AdminLessonSessionBulkInput, type AdminLessonSessionInput, type AdminProduct } from "@/lib/admin";

const date = new Intl.DateTimeFormat("en-AU", { weekday: "short", day: "numeric", month: "short", year: "numeric" });
const today = new Date().toISOString().slice(0, 10);
const weekdays = [
  ["MONDAY", "Mon"], ["TUESDAY", "Tue"], ["WEDNESDAY", "Wed"], ["THURSDAY", "Thu"],
  ["FRIDAY", "Fri"], ["SATURDAY", "Sat"], ["SUNDAY", "Sun"],
] as const;
type ScheduleSlot = { startTime: string; endTime: string; capacity: number };

function inputDate(daysFromToday: number) {
  const value = new Date();
  value.setDate(value.getDate() + daysFromToday);
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}

function sortSessions(items: AdminLessonSession[]) {
  return items.sort((a, b) => `${a.date}${a.startTime}`.localeCompare(`${b.date}${b.startTime}`));
}

export function AdminLessonSessionsPage() {
  const { user, accessToken, loading: authLoading } = useAuth();
  const [sessions, setSessions] = useState<AdminLessonSession[] | null>(null);
  const [lessonProducts, setLessonProducts] = useState<AdminProduct[]>([]);
  const [filter, setFilter] = useState<number | "ALL">("ALL");
  const [editing, setEditing] = useState<AdminLessonSession | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [generatorOpen, setGeneratorOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [scheduleSlots, setScheduleSlots] = useState<ScheduleSlot[]>([
    { startTime: "09:00", endTime: "11:00", capacity: 8 },
    { startTime: "13:00", endTime: "15:00", capacity: 8 },
  ]);
  const [generationResult, setGenerationResult] = useState<{ created: number; skipped: number } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken || user?.role !== "ADMIN") return;
    let active = true;
    Promise.all([getAdminLessonSessions(accessToken), getAdminProducts(accessToken)])
      .then(([sessionResult, productResult]) => { if (active) { setSessions(sessionResult); setLessonProducts(productResult.filter((product) => product.category === "LESSON")); } })
      .catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : "We could not load lesson capacity."); });
    return () => { active = false; };
  }, [accessToken, user]);

  const visibleSessions = useMemo(() => sessions?.filter((session) => filter === "ALL" || session.productId === filter) ?? [], [filter, sessions]);
  const activeLessonProducts = useMemo(() => lessonProducts.filter((product) => product.active), [lessonProducts]);
  const formLessonProducts = useMemo(() => {
    const editingProduct = editing ? lessonProducts.find((product) => product.id === editing.productId) : undefined;
    return editingProduct && !editingProduct.active ? [editingProduct, ...activeLessonProducts] : activeLessonProducts;
  }, [activeLessonProducts, editing, lessonProducts]);
  const totalCapacity = visibleSessions.reduce((sum, session) => sum + session.capacity, 0);
  const totalBooked = visibleSessions.reduce((sum, session) => sum + session.bookedCount, 0);

  function openNewSession() { setEditing(null); setFormError(null); setFormOpen(true); }
  function openEditSession(session: AdminLessonSession) { setEditing(session); setFormError(null); setFormOpen(true); }
  function openGenerator() {
    setFormError(null);
    setGenerationResult(null);
    setScheduleSlots([{ startTime: "09:00", endTime: "11:00", capacity: 8 }, { startTime: "13:00", endTime: "15:00", capacity: 8 }]);
    setGeneratorOpen(true);
  }

  function updateScheduleSlot(index: number, field: keyof ScheduleSlot, value: string) {
    setScheduleSlots((current) => current.map((slot, slotIndex) => slotIndex === index
      ? { ...slot, [field]: field === "capacity" ? Number(value) : value }
      : slot));
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken) return;
    const form = new FormData(event.currentTarget);
    const input: AdminLessonSessionInput = {
      productId: Number(form.get("productId") ?? editing?.productId),
      date: String(form.get("date") ?? editing?.date ?? ""),
      startTime: String(form.get("startTime") ?? editing?.startTime ?? ""),
      endTime: String(form.get("endTime") ?? editing?.endTime ?? ""),
      capacity: Number(form.get("capacity")),
      status: String(form.get("status") ?? editing?.status ?? "ACTIVE") as "ACTIVE" | "CANCELLED",
    };
    setSaving(true);
    setFormError(null);
    try {
      const saved = editing ? await updateAdminLessonSession(editing.id, input, accessToken) : await createAdminLessonSession(input, accessToken);
      setSessions((current) => current ? editing ? current.map((session) => session.id === saved.id ? saved : session) : sortSessions([...current, saved]) : [saved]);
      setFormOpen(false);
      setEditing(null);
    } catch (caught) { setFormError(caught instanceof Error ? caught.message : "We could not save this session."); }
    finally { setSaving(false); }
  }

  async function generateSchedule(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken) return;
    const form = new FormData(event.currentTarget);
    const input: AdminLessonSessionBulkInput = {
      productId: Number(form.get("productId")),
      startDate: String(form.get("startDate")),
      endDate: String(form.get("endDate")),
      daysOfWeek: form.getAll("daysOfWeek") as AdminLessonSessionBulkInput["daysOfWeek"],
      slots: scheduleSlots,
    };
    if (input.daysOfWeek.length === 0) { setFormError("Choose at least one day of the week."); return; }
    setGenerating(true);
    setFormError(null);
    setGenerationResult(null);
    try {
      const result = await generateAdminLessonSessions(input, accessToken);
      setSessions((current) => sortSessions(Array.from(new Map([...(current ?? []), ...result.sessions].map((session) => [session.id, session])).values())));
      setGenerationResult({ created: result.createdCount, skipped: result.skippedCount });
    } catch (caught) { setFormError(caught instanceof Error ? caught.message : "We could not generate this schedule."); }
    finally { setGenerating(false); }
  }

  if (authLoading) return <AdminRouteState kind="loading" />;
  if (!user || !accessToken) return <AdminRouteState kind="signed-out" />;
  if (user.role !== "ADMIN") return <AdminRouteState kind="forbidden" />;
  if (error) return <AdminRouteState kind="error" message={error} />;
  if (!sessions) return <AdminRouteState kind="loading" />;

  return (
    <AdminShell eyebrow="Inventory" title="Lesson sessions" description="Protect class capacity while keeping every scheduled lesson visible.">
      <div className="session-summary"><div><span>Scheduled sessions</span><strong>{visibleSessions.length}</strong></div><div><span>Booked places</span><strong>{totalBooked}<small> / {totalCapacity}</small></strong></div><label><span>Lesson product</span><select value={filter} onChange={(event) => setFilter(event.target.value === "ALL" ? "ALL" : Number(event.target.value))}><option value="ALL">All lesson products</option>{lessonProducts.map((product) => <option value={product.id} key={product.id}>{product.name}</option>)}</select></label><div className="session-summary__actions"><button className="button button--soft" type="button" onClick={openNewSession} disabled={activeLessonProducts.length === 0}>One session</button><button className="button button--ink" type="button" onClick={openGenerator} disabled={activeLessonProducts.length === 0}>Generate schedule <ArrowIcon /></button></div></div>
      {lessonProducts.length === 0 ? <div className="admin-empty"><span>0</span><h2>Create an active lesson product first.</h2><p>Lesson sessions can only be attached to products in the LESSON category.</p></div> : visibleSessions.length === 0 ? <div className="admin-empty"><span>0</span><h2>No lesson sessions yet.</h2><p>Create the first date, time and capacity for this product.</p></div> : <div className="session-grid">{visibleSessions.map((session) => {
        const fill = session.capacity ? Math.min(100, Math.round(session.bookedCount / session.capacity * 100)) : 0;
        return <article className="session-card" key={session.id}><div className="session-card__top"><span className={`inventory-status ${session.status === "ACTIVE" ? "is-active" : "is-inactive"}`}>{session.status}</span><small>Session #{session.id}</small></div><p>{session.productName}</p><h2>{date.format(new Date(`${session.date}T00:00:00`))}</h2><strong>{session.startTime.slice(0, 5)} – {session.endTime.slice(0, 5)}</strong><div className="capacity-meter"><div><span>Booked {session.bookedCount}</span><span>{session.availableCount} available</span></div><i><b style={{ width: `${fill}%` }} /></i></div><div className="session-card__footer"><span><small>Capacity</small><strong>{session.capacity}</strong></span><button type="button" onClick={() => openEditSession(session)}>Manage</button></div></article>;
      })}</div>}

      {formOpen && <div className="inventory-drawer-backdrop" onMouseDown={(event) => { if (event.currentTarget === event.target) setFormOpen(false); }}><aside className="inventory-drawer" role="dialog" aria-modal="true" aria-labelledby="session-form-title"><div className="inventory-drawer__heading"><div><span>{editing ? `Session #${editing.id}` : "New timetable slot"}</span><h2 id="session-form-title">{editing ? "Manage session" : "Create session"}</h2></div><button type="button" onClick={() => setFormOpen(false)} aria-label="Close session form">×</button></div>
        <form className="inventory-form" key={editing?.id ?? "new-session"} onSubmit={submit}>
          <label><span>Lesson product</span><select name="productId" defaultValue={editing?.productId ?? activeLessonProducts[0]?.id} disabled={Boolean(editing?.bookedCount)} required>{formLessonProducts.map((product) => <option value={product.id} key={product.id}>{product.name}{product.active ? "" : " (inactive)"}</option>)}</select></label>
          <label><span>Session date</span><input name="date" type="date" min={editing?.date ?? today} defaultValue={editing?.date ?? ""} disabled={Boolean(editing?.bookedCount)} required /></label>
          <div className="inventory-form__row"><label><span>Start time</span><input name="startTime" type="time" defaultValue={editing?.startTime.slice(0, 5) ?? "09:00"} disabled={Boolean(editing?.bookedCount)} required /></label><label><span>End time</span><input name="endTime" type="time" defaultValue={editing?.endTime.slice(0, 5) ?? "11:00"} disabled={Boolean(editing?.bookedCount)} required /></label></div>
          <label><span>Total capacity</span><input name="capacity" type="number" min={Math.max(1, editing?.bookedCount ?? 1)} step="1" defaultValue={editing?.capacity ?? 8} required /><small>Cannot be lower than {editing?.bookedCount ?? 0} currently booked places.</small></label>
          <label><span>Status</span><select name="status" defaultValue={editing?.status ?? "ACTIVE"} disabled={Boolean(editing?.bookedCount)}><option value="ACTIVE">Active</option><option value="CANCELLED">Cancelled</option></select></label>
          {Boolean(editing?.bookedCount) && <p className="inventory-lock-note">This session has bookings. Its product, date, time and status are locked; capacity can still be increased.</p>}
          {formError && <p className="inventory-form-error" role="alert">{formError}</p>}
          <div className="inventory-form__actions"><button className="button button--ink" type="submit" disabled={saving}>{saving ? "Saving…" : editing ? "Save capacity" : "Create session"}</button></div>
        </form>
      </aside></div>}

      {generatorOpen && <div className="inventory-drawer-backdrop" onMouseDown={(event) => { if (event.currentTarget === event.target) setGeneratorOpen(false); }}><aside className="inventory-drawer inventory-drawer--schedule" role="dialog" aria-modal="true" aria-labelledby="schedule-form-title"><div className="inventory-drawer__heading"><div><span>Bulk scheduling</span><h2 id="schedule-form-title">Generate sessions</h2></div><button type="button" onClick={() => setGeneratorOpen(false)} aria-label="Close schedule generator">×</button></div>
        <form className="inventory-form" onSubmit={generateSchedule}>
          <p className="schedule-intro">Build up to 63 days at once. Existing sessions with the same product, date and start time are kept and skipped automatically.</p>
          <label><span>Lesson product</span><select name="productId" defaultValue={filter === "ALL" || !activeLessonProducts.some((product) => product.id === filter) ? activeLessonProducts[0]?.id : filter} required>{activeLessonProducts.map((product) => <option value={product.id} key={product.id}>{product.name}</option>)}</select></label>
          <div className="inventory-form__row"><label><span>Start date</span><input name="startDate" type="date" min={today} defaultValue={inputDate(1)} required /></label><label><span>End date</span><input name="endDate" type="date" min={today} defaultValue={inputDate(8)} required /></label></div>
          <fieldset className="schedule-weekdays"><legend>Run on</legend><div>{weekdays.map(([value, label]) => <label key={value}><input type="checkbox" name="daysOfWeek" value={value} defaultChecked /><span>{label}</span></label>)}</div></fieldset>
          <fieldset className="schedule-slots"><legend>Daily time slots</legend>{scheduleSlots.map((slot, index) => <div className="schedule-slot" key={`${index}-${scheduleSlots.length}`}><label><span>Start</span><input type="time" value={slot.startTime} onChange={(event) => updateScheduleSlot(index, "startTime", event.target.value)} required /></label><label><span>End</span><input type="time" value={slot.endTime} onChange={(event) => updateScheduleSlot(index, "endTime", event.target.value)} required /></label><label><span>Places</span><input type="number" min="1" max="100" step="1" value={slot.capacity} onChange={(event) => updateScheduleSlot(index, "capacity", event.target.value)} required /></label>{scheduleSlots.length > 1 && <button type="button" onClick={() => setScheduleSlots((current) => current.filter((_, slotIndex) => slotIndex !== index))} aria-label={`Remove time slot ${index + 1}`}>×</button>}</div>)}<button className="schedule-add-slot" type="button" onClick={() => setScheduleSlots((current) => [...current, { startTime: "09:00", endTime: "11:00", capacity: 8 }])}>+ Add another time slot</button></fieldset>
          {generationResult && <div className="schedule-result" role="status"><strong>{generationResult.created} sessions created</strong><span>{generationResult.skipped} existing {generationResult.skipped === 1 ? "session was" : "sessions were"} left unchanged.</span></div>}
          {formError && <p className="inventory-form-error" role="alert">{formError}</p>}
          <div className="inventory-form__actions"><button className="button button--ink" type="submit" disabled={generating}>{generating ? "Generating…" : "Generate schedule"}</button></div>
        </form>
      </aside></div>}
    </AdminShell>
  );
}
