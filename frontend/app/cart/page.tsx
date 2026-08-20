import type { Metadata } from "next";

import { CartPage } from "@/components/cart-page";

export const metadata: Metadata = { title: "Your Cart", description: "Review your Snow Alpine Resort selections." };

export default function CartRoute() {
  return <CartPage />;
}
