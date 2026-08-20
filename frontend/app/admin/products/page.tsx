import type { Metadata } from "next";

import { AdminProductsPage } from "@/components/admin-products-page";

export const metadata: Metadata = { title: "Admin products" };

export default function Page() { return <AdminProductsPage />; }
