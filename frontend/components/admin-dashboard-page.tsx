"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { AdminRouteState, AdminShell } from "@/components/admin-shell";
import { ArrowIcon } from "@/components/icons";
import { useAuth } from "@/components/auth-provider";
import { adminCategories, getAdminDashboard, type AdminDashboard } from "@/lib/admin";

export function AdminDashboardPage() {
  const { user, accessToken, loading: authLoading } = useAuth();
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken || user?.role !== "ADMIN") return;
    let active = true;
    getAdminDashboard(accessToken)
      .then((result) => { if (active) setDashboard(result); })
      .catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : "We could not load the dashboard."); });
    return () => { active = false; };
  }, [accessToken, user]);

  if (authLoading) return <AdminRouteState kind="loading" />;
  if (!user || !accessToken) return <AdminRouteState kind="signed-out" />;
  if (user.role !== "ADMIN") return <AdminRouteState kind="forbidden" />;
  if (error) return <AdminRouteState kind="error" message={error} />;
  if (!dashboard) return <AdminRouteState kind="loading" />;

  const total = adminCategories.reduce((sum, item) => sum + dashboard[item.countKey], 0);

  return (
    <AdminShell eyebrow="Live operations" title="Reservation overview" description="Confirmed mountain activity across every booking category.">
      <section className="admin-summary"><div><span>Total reservations</span><strong>{total}</strong><small>Confirmed purchased places</small></div><p><i /> Live from the booking service</p></section>
      <section className="admin-category-grid">
        {adminCategories.map((item, index) => <Link className={`admin-category-card admin-category-card--${item.accent}`} href={`/admin/reservations/${item.slug}`} key={item.category}>
          <div><span>0{index + 1}</span><small>{item.category.replaceAll("_", " ")}</small></div><strong>{dashboard[item.countKey]}</strong><h2>{item.title}</h2><p>{item.description}</p><span className="admin-card-link">Open reservations <ArrowIcon /></span>
        </Link>)}
      </section>
      <section className="admin-note"><div><span>How counts work</span><h2>Only active mountain days.</h2></div><p>Dashboard totals count purchased quantities from confirmed or completed bookings. Pending payments and cancelled orders stay outside operational capacity.</p></section>
    </AdminShell>
  );
}
