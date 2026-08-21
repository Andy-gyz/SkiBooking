import type { Metadata } from "next";

import { AuthProvider } from "@/components/auth-provider";
import { CartProvider } from "@/components/cart-provider";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

import "./globals.css";

const siteUrl = new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "https://snowalpineresort.com");

export const metadata: Metadata = {
  metadataBase: siteUrl,
  title: {
    default: "Snow Alpine Resort | Plan Your Snow Adventure",
    template: "%s | Snow Alpine Resort",
  },
  description: "Book resort entry, lift tickets, ski lessons and equipment rentals at Snow Alpine Resort.",
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    url: "/",
    siteName: "Snow Alpine Resort",
    title: "Snow Alpine Resort | Plan Your Snow Adventure",
    description: "Book resort entry, lift tickets, ski lessons and equipment rentals in one connected snow-day plan.",
    images: [{ url: "/images/hero-ski.jpg", alt: "A skier descending a snowy alpine mountain" }],
  },
  twitter: {
    card: "summary_large_image",
    title: "Snow Alpine Resort | Plan Your Snow Adventure",
    description: "Book resort entry, lift tickets, ski lessons and equipment rentals in one place.",
    images: ["/images/hero-ski.jpg"],
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" data-scroll-behavior="smooth">
      <body>
        <AuthProvider>
          <CartProvider>
            <SiteHeader />
            {children}
            <SiteFooter />
          </CartProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
