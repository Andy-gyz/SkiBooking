"use client";

import Link from "next/link";
import { useEffect } from "react";

export default function ErrorPage({
  error,
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main className="not-found shell" role="alert">
      <span>CONNECTION INTERRUPTED</span>
      <h1>We hit an unexpected patch of weather.</h1>
      <p>Your cart and account details are still safe. Try loading this page again, or return to the resort home page.</p>
      <div className="error-actions">
        <button className="button button--ink" onClick={() => retry()} type="button">Try again</button>
        <Link className="button button--outline" href="/">Return home</Link>
      </div>
    </main>
  );
}
