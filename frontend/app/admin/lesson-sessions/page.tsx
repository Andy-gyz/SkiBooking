import type { Metadata } from "next";

import { AdminLessonSessionsPage } from "@/components/admin-lesson-sessions-page";

export const metadata: Metadata = { title: "Admin lesson sessions" };

export default function Page() { return <AdminLessonSessionsPage />; }
