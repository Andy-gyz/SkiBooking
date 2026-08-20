import Link from "next/link";

import { CategoryIcon } from "@/components/category-icon";
import { ArrowIcon, CheckIcon, MountainIcon } from "@/components/icons";
import { categories } from "@/lib/categories";

export default function Home() {
  return (
    <main>
      <section className="hero">
        <div className="shell hero__grid">
          <div className="hero__content">
            <p className="eyebrow eyebrow--light">Victoria&apos;s alpine escape</p>
            <h1>More mountain.<br />Less planning.</h1>
            <p className="hero__lede">Everything you need for a brilliant day on the snow, booked in one place. Entry, lift access, lessons and gear—sorted.</p>
            <div className="hero__actions">
              <Link className="button button--snow" href="/lift-tickets">Book lift tickets <ArrowIcon /></Link>
              <a className="button button--ghost" href="#plan">Explore the mountain</a>
            </div>
            <div className="hero__proof" aria-label="Booking benefits">
              <span><CheckIcon /> One simple checkout</span>
              <span><CheckIcon /> Secure Stripe payments</span>
            </div>
          </div>

          <div className="hero-card" aria-label="Snow Alpine Resort season information">
            <div className="hero-card__art" aria-hidden="true">
              <span className="sun" />
              <span className="ridge ridge--back" />
              <span className="ridge ridge--front" />
              <span className="lift-line" />
              <span className="lift-chair lift-chair--one" />
              <span className="lift-chair lift-chair--two" />
            </div>
            <div className="hero-card__body">
              <span className="season-pill">2026 snow season</span>
              <div><strong>Snow Alpine Resort</strong><span>Victoria, Australia</span></div>
              <div className="hero-card__meta"><span><b>4</b> ways to play</span><span><b>1</b> easy booking</span></div>
            </div>
          </div>
        </div>
      </section>

      <section className="plan-section" id="plan">
        <div className="shell">
          <div className="section-heading">
            <div><p className="eyebrow">Plan your snow day</p><h2>Start with what you need</h2></div>
            <p>Build your day your way. Add products from any category and pay for everything together when you&apos;re ready.</p>
          </div>
          <div className="category-grid">
            {categories.map((category, index) => (
              <Link className={`category-card category-card--${category.accent}`} href={`/${category.slug}`} key={category.slug}>
                <span className="category-card__number">0{index + 1}</span>
                <CategoryIcon category={category.category} />
                <div className="category-card__copy"><h3>{category.shortTitle}</h3><p>{category.cardDescription}</p></div>
                <span className="category-card__link">View &amp; book <ArrowIcon /></span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="promise-section">
        <div className="shell promise-grid">
          <div className="promise-art" aria-hidden="true"><MountainIcon /><span>SNOW<br />ALPINE</span></div>
          <div className="promise-copy">
            <p className="eyebrow">One mountain, one booking</p>
            <h2>Your whole alpine day, together.</h2>
            <p>No jumping between separate checkouts. Reserve your vehicle entry, passes, lesson and equipment in one cart, then manage the complete booking from your account.</p>
            <ul>
              <li><CheckIcon /> Browse and build your cart without an account</li>
              <li><CheckIcon /> Live lesson availability before you book</li>
              <li><CheckIcon /> Clear AUD pricing with secure payment</li>
            </ul>
          </div>
        </div>
      </section>
    </main>
  );
}
