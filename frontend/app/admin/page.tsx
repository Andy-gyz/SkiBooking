import type { Metadata } from "next";

import { AdminDashboardPage } from "@/components/admin-dashboard-page";

export const metadata: Metadata = { title: "Admin dashboard" };

export default function Page() { return <AdminDashboardPage />; }
