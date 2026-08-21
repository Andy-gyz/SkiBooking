"use client";

import { useEffect } from "react";

export default function GlobalError({
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
    <html lang="en">
      <body style={{ margin: 0, background: "#f5f6f8", color: "#101116", fontFamily: "Arial, sans-serif" }}>
        <main style={{ minHeight: "100vh", display: "grid", placeItems: "center", padding: 24 }}>
          <section style={{ width: "min(680px, 100%)", padding: "56px", borderRadius: 28, background: "white", boxShadow: "0 30px 80px rgba(17, 20, 35, .12)" }}>
            <p style={{ margin: "0 0 16px", color: "#2f55ff", fontSize: 12, fontWeight: 800, letterSpacing: ".14em" }}>SNOW ALPINE</p>
            <h1 style={{ margin: "0 0 18px", fontSize: "clamp(42px, 8vw, 68px)", lineHeight: .98, letterSpacing: "-.05em" }}>The mountain service needs a moment.</h1>
            <p style={{ margin: "0 0 28px", color: "#696d76", fontSize: 17, lineHeight: 1.65 }}>Please try again. Your browser will keep your existing cart and sign-in details.</p>
            <button onClick={() => retry()} style={{ minHeight: 50, padding: "0 24px", border: 0, borderRadius: 999, background: "#101116", color: "white", fontWeight: 750, cursor: "pointer" }} type="button">Try again</button>
          </section>
        </main>
      </body>
    </html>
  );
}
