"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { useAuth } from "@/components/auth-provider";
import { ArrowIcon, MountainIcon } from "@/components/icons";

export function AccountPage() {
  const router = useRouter();
  const { user, loading, logout } = useAuth();
  const [pending, setPending] = useState(false);

  async function signOut() {
    setPending(true);
    await logout();
    router.replace("/");
  }

  if (loading) return <main className="account-page"><div className="shell account-loading">Loading your account…</div></main>;

  if (!user) return (
    <main className="account-page"><div className="shell empty-cart"><MountainIcon /><p className="eyebrow">Your account</p><h1>Sign in for your snow days.</h1><p>Access checkout and keep your booking details connected to one account.</p><Link className="button button--ink" href="/login">Sign in <ArrowIcon /></Link></div></main>
  );

  return (
    <main className="account-page">
      <section className="account-hero"><div className="shell"><p className="eyebrow">Customer account</p><h1>Hi, {user.firstName}.<br />The mountain is waiting.</h1></div></section>
      <section className="account-content"><div className="shell account-grid">
        <div className="account-card"><span>Profile</span><h2>{user.firstName} {user.lastName}</h2><p>{user.email}<br />{user.phone || "No phone number added"}</p><small>Role · {user.role}</small></div>
        <div className="account-actions-card"><h2>Ready to continue?</h2><p>Your active cart is linked to this account and ready for checkout.</p><Link className="button button--ink" href="/checkout">Continue to checkout <ArrowIcon /></Link><Link href="/cart">Review cart</Link></div>
        <button className="account-logout" type="button" onClick={signOut} disabled={pending}>{pending ? "Signing out…" : "Sign out"}</button>
      </div></section>
    </main>
  );
}
