import type { Metadata } from "next";

import { BookingDetailPage } from "@/components/booking-detail-page";

export const metadata: Metadata = { title: "Booking details" };

export default async function Page({ params }: PageProps<"/bookings/[bookingNumber]">) {
  const { bookingNumber } = await params;
  return <BookingDetailPage bookingNumber={bookingNumber} />;
}
