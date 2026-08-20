import type { Metadata } from "next";

import { AdminReservationsPage } from "@/components/admin-reservations-page";

export const metadata: Metadata = { title: "Admin reservations" };

export default async function Page({ params }: PageProps<"/admin/reservations/[category]">) {
  const { category } = await params;
  return <AdminReservationsPage categorySlug={category} />;
}
