import type { Metadata } from "next";

import { CartProvider } from "@/components/cart-provider";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Snow Alpine Resort | Plan Your Snow Adventure",
    template: "%s | Snow Alpine Resort",
  },
  description: "Book resort entry, lift tickets, ski lessons and equipment rentals at Snow Alpine Resort.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" data-scroll-behavior="smooth">
      <body>
        <CartProvider>
          <SiteHeader />
          {children}
          <SiteFooter />
        </CartProvider>
      </body>
    </html>
  );
}
