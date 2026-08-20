"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";

import { AdminRouteState, AdminShell } from "@/components/admin-shell";
import { ArrowIcon } from "@/components/icons";
import { useAuth } from "@/components/auth-provider";
import { adminCategories, createAdminProduct, deactivateAdminProduct, getAdminProducts, getResorts, updateAdminProduct, type AdminCategory, type AdminProduct, type AdminProductInput, type Resort } from "@/lib/admin";

const money = new Intl.NumberFormat("en-AU", { style: "currency", currency: "AUD", minimumFractionDigits: 0 });

export function AdminProductsPage() {
  const { user, accessToken, loading: authLoading } = useAuth();
  const [products, setProducts] = useState<AdminProduct[] | null>(null);
  const [resorts, setResorts] = useState<Resort[]>([]);
  const [filter, setFilter] = useState<AdminCategory | "ALL">("ALL");
  const [editing, setEditing] = useState<AdminProduct | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken || user?.role !== "ADMIN") return;
    let active = true;
    Promise.all([getAdminProducts(accessToken), getResorts()])
      .then(([productResult, resortResult]) => { if (active) { setProducts(productResult); setResorts(resortResult); } })
      .catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : "We could not load inventory."); });
    return () => { active = false; };
  }, [accessToken, user]);

  const visibleProducts = useMemo(() => products?.filter((product) => filter === "ALL" || product.category === filter) ?? [], [filter, products]);

  function openNewProduct() { setEditing(null); setFormError(null); setFormOpen(true); }
  function openEditProduct(product: AdminProduct) { setEditing(product); setFormError(null); setFormOpen(true); }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken) return;
    const form = new FormData(event.currentTarget);
    const input: AdminProductInput = {
      resortId: Number(form.get("resortId")),
      name: String(form.get("name") ?? "").trim(),
      category: String(form.get("category")) as AdminCategory,
      description: String(form.get("description") ?? "").trim() || undefined,
      price: Number(form.get("price")),
      imageUrl: String(form.get("imageUrl") ?? "").trim() || undefined,
      active: form.get("active") === "on",
    };
    setSaving(true);
    setFormError(null);
    try {
      const saved = editing ? await updateAdminProduct(editing.id, input, accessToken) : await createAdminProduct(input, accessToken);
      setProducts((current) => current ? editing ? current.map((product) => product.id === saved.id ? saved : product) : [...current, saved].sort((a, b) => a.name.localeCompare(b.name)) : [saved]);
      setFormOpen(false);
      setEditing(null);
    } catch (caught) {
      setFormError(caught instanceof Error ? caught.message : "We could not save this product.");
    } finally { setSaving(false); }
  }

  async function deactivate(product: AdminProduct) {
    if (!accessToken || !window.confirm(`Deactivate ${product.name}? Existing carts and bookings are preserved.`)) return;
    setFormError(null);
    try {
      await deactivateAdminProduct(product.id, accessToken);
      setProducts((current) => current?.map((item) => item.id === product.id ? { ...item, active: false } : item) ?? null);
      setEditing((current) => current?.id === product.id ? { ...current, active: false } : current);
    } catch (caught) { setFormError(caught instanceof Error ? caught.message : "We could not deactivate this product."); }
  }

  if (authLoading) return <AdminRouteState kind="loading" />;
  if (!user || !accessToken) return <AdminRouteState kind="signed-out" />;
  if (user.role !== "ADMIN") return <AdminRouteState kind="forbidden" />;
  if (error) return <AdminRouteState kind="error" message={error} />;
  if (!products) return <AdminRouteState kind="loading" />;

  return (
    <AdminShell eyebrow="Inventory" title="Products" description="Control what customers can book and the trusted price used at checkout.">
      <div className="inventory-toolbar"><div className="inventory-tabs"><button className={filter === "ALL" ? "is-active" : ""} onClick={() => setFilter("ALL")} type="button">All <span>{products.length}</span></button>{adminCategories.map((item) => <button className={filter === item.category ? "is-active" : ""} onClick={() => setFilter(item.category)} type="button" key={item.category}>{item.shortTitle}</button>)}</div><button className="button button--ink" type="button" onClick={openNewProduct}>New product <ArrowIcon /></button></div>
      <div className="inventory-table-wrap"><table className="inventory-table"><thead><tr><th>Product</th><th>Category</th><th>Resort</th><th>Price</th><th>Status</th><th /></tr></thead><tbody>{visibleProducts.map((product) => <tr key={product.id}><td><strong>{product.name}</strong><small>{product.description || "No description"}</small></td><td><span>{product.category.replaceAll("_", " ")}</span></td><td><strong>{product.resort.name}</strong><small>{product.resort.location}</small></td><td><b>{money.format(product.price)}</b><small>{product.currency}</small></td><td><span className={`inventory-status ${product.active ? "is-active" : "is-inactive"}`}>{product.active ? "ACTIVE" : "INACTIVE"}</span></td><td><button type="button" onClick={() => openEditProduct(product)}>Edit</button></td></tr>)}</tbody></table>{visibleProducts.length === 0 && <div className="inventory-empty">No products match this category.</div>}</div>

      {formOpen && <div className="inventory-drawer-backdrop" onMouseDown={(event) => { if (event.currentTarget === event.target) setFormOpen(false); }}><aside className="inventory-drawer" role="dialog" aria-modal="true" aria-labelledby="product-form-title"><div className="inventory-drawer__heading"><div><span>{editing ? `Product #${editing.id}` : "New catalog item"}</span><h2 id="product-form-title">{editing ? "Edit product" : "Create product"}</h2></div><button type="button" onClick={() => setFormOpen(false)} aria-label="Close product form">×</button></div>
        <form className="inventory-form" key={editing?.id ?? "new"} onSubmit={submit}>
          <label><span>Product name</span><input name="name" defaultValue={editing?.name ?? ""} maxLength={150} required /></label>
          <div className="inventory-form__row"><label><span>Category</span><select name="category" defaultValue={editing?.category ?? "LIFT_TICKET"} required>{adminCategories.map((item) => <option value={item.category} key={item.category}>{item.title}</option>)}</select></label><label><span>Resort</span><select name="resortId" defaultValue={editing?.resort.id ?? resorts[0]?.id} required>{resorts.map((resort) => <option value={resort.id} key={resort.id}>{resort.name}</option>)}</select></label></div>
          <label><span>Description</span><textarea name="description" defaultValue={editing?.description ?? ""} rows={4} /></label>
          <label><span>Price · AUD</span><input name="price" type="number" min="0.01" step="0.01" defaultValue={editing?.price ?? ""} required /></label>
          <label><span>Image URL <small>Optional</small></span><input name="imageUrl" type="url" maxLength={500} defaultValue={editing?.imageUrl ?? ""} placeholder="https://…" /></label>
          <label className="inventory-switch"><input name="active" type="checkbox" defaultChecked={editing?.active ?? true} /><span><b>Available to customers</b><small>Inactive products remain in historical bookings.</small></span></label>
          {formError && <p className="inventory-form-error" role="alert">{formError}</p>}
          <div className="inventory-form__actions">{editing?.active && <button className="inventory-danger" type="button" onClick={() => deactivate(editing)}>Deactivate</button>}<button className="button button--ink" type="submit" disabled={saving}>{saving ? "Saving…" : editing ? "Save changes" : "Create product"}</button></div>
        </form>
      </aside></div>}
    </AdminShell>
  );
}
