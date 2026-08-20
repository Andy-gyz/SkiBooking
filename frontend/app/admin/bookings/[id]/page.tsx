import type { Metadata } from "next";

import { AdminBookingPage } from "@/components/admin-booking-page";

export const metadata: Metadata = { title: "Admin booking record" };

export default async function Page({ params }: PageProps<"/admin/bookings/[id]">) {
  const { id } = await params;
  return <AdminBookingPage bookingId={Number(id)} />;
}
