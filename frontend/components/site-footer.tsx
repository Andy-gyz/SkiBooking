import Link from "next/link";

import { MountainIcon } from "@/components/icons";
import { categories } from "@/lib/categories";

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="shell footer-grid">
        <div>
          <Link className="brand brand--footer" href="/"><MountainIcon /><span><b>SNOW</b> ALPINE</span></Link>
          <p>Four ways to enjoy the mountain. One simple place to book.</p>
        </div>
        <div className="footer-links">
          <strong>Plan your visit</strong>
          {categories.map((category) => <Link href={`/${category.slug}`} key={category.slug}>{category.navTitle}</Link>)}
        </div>
        <div className="footer-location">
          <strong>Snow Alpine Resort</strong>
          <span>Victoria, Australia</span>
          <span>Open for the 2026 snow season</span>
        </div>
      </div>
      <div className="shell footer-bottom"><span>© 2026 Snow Alpine Resort</span><span>Built for better snow days.</span></div>
    </footer>
  );
}
