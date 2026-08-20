"use client";

import Link from "next/link";

import { useAuth } from "@/components/auth-provider";
import { UserIcon } from "@/components/icons";

export function AccountStatusButton() {
  const { user, loading } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const label = user ? isAdmin ? `Admin dashboard for ${user.firstName}` : `Account for ${user.firstName}` : "Sign in";

  return (
    <Link className={`header-icon account-button${user ? " is-authenticated" : ""}`} href={user ? isAdmin ? "/admin" : "/account" : "/login"} aria-label={loading ? "Loading account" : label} title={loading ? "Loading account" : label}>
      <UserIcon />
      {user && <span aria-hidden="true">{user.firstName.charAt(0)}{user.lastName.charAt(0)}</span>}
    </Link>
  );
}
