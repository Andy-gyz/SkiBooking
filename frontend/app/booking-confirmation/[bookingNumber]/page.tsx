import type { Metadata } from "next";

import { BookingConfirmationPage } from "@/components/booking-confirmation-page";

export const metadata: Metadata = { title: "Booking confirmation" };

export default async function Page({ params }: PageProps<"/booking-confirmation/[bookingNumber]">) {
  const { bookingNumber } = await params;
  return <BookingConfirmationPage bookingNumber={bookingNumber} />;
}
