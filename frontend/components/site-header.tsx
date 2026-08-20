import Link from "next/link";

import { CartIcon, MountainIcon, UserIcon } from "@/components/icons";
import { categories } from "@/lib/categories";

export function SiteHeader() {
  return (
    <header className="site-header">
      <div className="shell site-header__inner">
        <Link className="brand" href="/" aria-label="Snow Alpine Resort home">
          <MountainIcon />
          <span><b>SNOW</b> ALPINE</span>
        </Link>
        <nav className="desktop-nav" aria-label="Primary navigation">
          <Link href="/">Home</Link>
          {categories.map((category) => <Link href={`/${category.slug}`} key={category.slug}>{category.navTitle}</Link>)}
        </nav>
        <div className="header-actions">
          <span className="header-icon" aria-label="Account coming in a later milestone" title="Account coming soon"><UserIcon /></span>
          <span className="header-icon cart-button" aria-label="Cart coming in a later milestone" title="Cart coming soon"><CartIcon /><span>0</span></span>
          <details className="mobile-menu">
            <summary aria-label="Open navigation"><i /><i /><i /></summary>
            <nav aria-label="Mobile navigation">
              <Link href="/">Home</Link>
              {categories.map((category) => <Link href={`/${category.slug}`} key={category.slug}>{category.navTitle}</Link>)}
            </nav>
          </details>
        </div>
      </div>
    </header>
  );
}
