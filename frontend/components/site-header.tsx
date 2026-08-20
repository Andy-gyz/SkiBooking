import Link from "next/link";

import { AccountStatusButton } from "@/components/account-status-button";
import { CartStatusButton } from "@/components/cart-status-button";
import { MountainIcon } from "@/components/icons";
import { categories } from "@/lib/categories";

export function SiteHeader() {
  return (
    <header className="site-header">
      <Link className="announcement" href="/#plan">
        <span>2026 season bookings are open</span>
        <b>Plan your snow day →</b>
      </Link>
      <div className="site-nav">
        <div className="shell site-header__inner">
          <Link className="brand" href="/" aria-label="Snow Alpine Resort home">
            <MountainIcon />
            <span><b>Snow</b> Alpine</span>
          </Link>
          <nav className="desktop-nav" aria-label="Primary navigation">
            {categories.map((category) => <Link href={`/${category.slug}`} key={category.slug}>{category.navTitle}</Link>)}
          </nav>
          <div className="header-actions">
            <AccountStatusButton />
            <CartStatusButton />
            <details className="mobile-menu">
              <summary aria-label="Open navigation"><i /><i /><i /></summary>
              <nav aria-label="Mobile navigation">
                <Link href="/">Home</Link>
                {categories.map((category) => <Link href={`/${category.slug}`} key={category.slug}>{category.navTitle}</Link>)}
                <Link href="/cart">Cart</Link>
                <Link href="/account">Account</Link>
              </nav>
            </details>
          </div>
        </div>
      </div>
    </header>
  );
}
