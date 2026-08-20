"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";

import { useAuth } from "@/components/auth-provider";
import { ArrowIcon, MountainIcon } from "@/components/icons";
import { adminCategories } from "@/lib/admin";

export function AdminShell({ eyebrow, title, description, children }: { eyebrow: string; title: string; description: string; children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();
  const [signingOut, setSigningOut] = useState(false);

  async function signOut() {
    setSigningOut(true);
    await logout();
    router.replace("/");
  }

  return (
    <main className="admin-page">
      <div className="admin-frame">
        <aside className="admin-sidebar">
          <Link className="admin-brand" href="/admin"><MountainIcon /><span><b>Snow Alpine</b><small>Operations</small></span></Link>
          <nav aria-label="Admin navigation">
            <Link className={pathname === "/admin" ? "is-active" : ""} href="/admin"><span>Overview</span><small>Dashboard</small></Link>
            <p>Reservations</p>
            {adminCategories.map((item) => <Link className={pathname.includes(`/admin/reservations/${item.slug}`) ? "is-active" : ""} href={`/admin/reservations/${item.slug}`} key={item.category}><span>{item.shortTitle}</span><small>{item.title}</small></Link>)}
            <p>Inventory</p>
            <Link className={pathname === "/admin/products" ? "is-active" : ""} href="/admin/products"><span>Products</span><small>Catalog</small></Link>
            <Link className={pathname === "/admin/lesson-sessions" ? "is-active" : ""} href="/admin/lesson-sessions"><span>Sessions</span><small>Capacity</small></Link>
          </nav>
          <div className="admin-user"><span>{user?.firstName.charAt(0)}{user?.lastName.charAt(0)}</span><div><strong>{user?.firstName} {user?.lastName}</strong><small>Administrator</small></div><button type="button" onClick={signOut} disabled={signingOut} aria-label="Sign out">↗</button></div>
        </aside>
        <div className="admin-workspace">
          <header className="admin-header"><div><p>{eyebrow}</p><h1>{title}</h1><span>{description}</span></div><Link href="/" target="_blank">View customer site <ArrowIcon /></Link></header>
          <div className="admin-content">{children}</div>
        </div>
      </div>
    </main>
  );
}

export function AdminRouteState({ kind, message }: { kind: "loading" | "signed-out" | "forbidden" | "error"; message?: string }) {
  return <main className="admin-route-state"><div>{kind === "loading" ? <span className="bookings-state__spinner" /> : <MountainIcon />}<p className="eyebrow">Admin operations</p><h1>{kind === "loading" ? "Loading dashboard…" : kind === "signed-out" ? "Sign in to continue." : kind === "forbidden" ? "Administrator access only." : "Operations are temporarily unavailable."}</h1>{message && <p>{message}</p>}{kind === "signed-out" && <Link className="button button--ink" href="/login?next=%2Fadmin">Admin sign in <ArrowIcon /></Link>}{kind === "forbidden" && <Link className="button button--ink" href="/account">Return to your account</Link>}</div></main>;
}
