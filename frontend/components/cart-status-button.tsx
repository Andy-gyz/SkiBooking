"use client";

import Link from "next/link";

import { CartIcon } from "@/components/icons";
import { useCart } from "@/components/cart-provider";

export function CartStatusButton() {
  const { cart } = useCart();
  return <Link className="header-icon cart-button" href="/cart" aria-label={`Cart with ${cart?.itemCount ?? 0} items`}><CartIcon /><span>{cart?.itemCount ?? 0}</span></Link>;
}
