import Link from "next/link";

export default function NotFound() {
  return (
    <main className="not-found shell">
      <span>404</span><h1>This trail isn&apos;t on the map.</h1>
      <p>The page you&apos;re looking for may have moved, or the snow has covered our tracks.</p>
      <Link className="button button--ink" href="/">Return home</Link>
    </main>
  );
}
