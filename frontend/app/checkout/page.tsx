import type { Metadata } from "next";

import { CheckoutPage } from "@/components/checkout-page";

export const metadata: Metadata = { title: "Checkout" };

export default function Page() {
  return <CheckoutPage />;
}
