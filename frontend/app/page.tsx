import Image from "next/image";
import Link from "next/link";

import { DayPlanner, type PlannerItem } from "@/components/day-planner";
import { ArrowIcon, CheckIcon, MountainIcon } from "@/components/icons";
import { MountainBriefing } from "@/components/mountain-briefing";
import { categories } from "@/lib/categories";
import { getProducts } from "@/lib/catalog";

const activities = [
  { image: "/images/activity-family.jpg", alt: "A family enjoying a winter day together", tag: "Family weekend", title: "First tracks together" },
  { image: "/images/activity-snowboard.jpg", alt: "A snowboarder riding fresh mountain snow", tag: "Freeride day", title: "Fresh snow, wide smiles" },
  { image: "/images/activity-lift.jpg", alt: "Guests riding a chairlift above the snow", tag: "Mountain moments", title: "The ride back to the top" },
  { image: "/images/activity-couple.jpg", alt: "Two skiers spending a day in the alpine", tag: "Winter escape", title: "A day worth remembering" },
];

const fallbackPrices = { RESORT_ACCESS: 55, LIFT_TICKET: 135, LESSON: 120, RENTAL: 65 } as const;

export default async function Home() {
  const plannerItems: PlannerItem[] = await Promise.all(categories.map(async (category) => {
    let unitPrice: number = fallbackPrices[category.category];
    try {
      const products = await getProducts(category.category);
      if (products[0]) unitPrice = products[0].price;
    } catch {
      // Keep the homepage planner useful when the local API is not running.
    }
    return { slug: category.slug, category: category.category, title: category.navTitle, accent: category.accent, unitPrice, perGuest: category.category !== "RESORT_ACCESS" };
  }));

  return (
    <main>
      <section className="hero">
        <div className="hero__media" aria-hidden="true">
          <div className="hero__photo" />
          <div className="hero__veil" />
          <div className="hero__snow"><i /><i /><i /><i /><i /><i /><i /><i /><i /><i /></div>
        </div>
        <div className="shell hero__content">
          <span className="season-chip"><i /> Live for winter 2026</span>
          <h1>Your whole snow day.<br />One simple booking.</h1>
          <p className="hero__lede">
            Entry, lift passes, lessons and equipment—planned together in one
            clear, effortless experience at Snow Alpine Resort.
          </p>
          <div className="hero__actions">
            <a className="button button--ink" href="#plan">Plan your day <ArrowIcon /></a>
            <Link className="button button--soft" href="/lift-tickets">View lift tickets</Link>
          </div>
          <div className="hero__proof" aria-label="Booking benefits">
            <span><CheckIcon /> One cart for everything</span>
            <span><CheckIcon /> Secure Stripe checkout</span>
            <span><CheckIcon /> Prices in AUD</span>
          </div>

        </div>
      </section>

      <section className="mountain-status" aria-label="Mountain status">
        <div className="shell mountain-status__grid">
          <div><span className="status-live"><i /> Resort status</span><strong>Open daily</strong><small>8:30 am – 4:30 pm</small></div>
          <div><span>Mountain weather</span><strong>−2° · Snowing</strong><small>Fresh alpine conditions</small></div>
          <div><span>Today&apos;s outlook</span><strong>Perfect for turns</strong><small>Check updates before travel</small></div>
          <a href="#plan"><span>Plan your visit</span><strong>Book all four</strong><small>Entry, lifts, lessons & gear <ArrowIcon /></small></a>
        </div>
      </section>

      <section className="booking-preview">
        <div className="shell booking-preview__inner">
          <div className="booking-preview__copy">
            <p className="eyebrow">Build your snow day</p>
            <h2>Choose what you need before you reach the snow.</h2>
            <p>See the four essentials and their starting prices together in one clear itinerary. When you&apos;re ready, use Start booking to begin with resort entry.</p>
          </div>
          <DayPlanner items={plannerItems} />
        </div>
      </section>

      <section className="experience-section" id="plan">
        <div className="shell">
          <div className="experience-intro">
            <p className="eyebrow eyebrow--light">Everything you need</p>
            <h2>Less organising.<br />More mountain.</h2>
            <p>Four essential parts of your snow day, designed to work as one booking from the very beginning.</p>
          </div>

          <div className="category-bento">
            {categories.map((category, index) => (
              <Link className={`bento-card bento-card--${category.accent}`} href={`/${category.slug}`} key={category.slug}>
                <div className="bento-card__copy">
                  <span>0{index + 1} · {category.eyebrow}</span>
                  <h3>{category.shortTitle}</h3>
                  <p>{category.cardDescription}</p>
                </div>
                <div className="bento-card__visual">
                  <Image src={category.photo} alt={category.photoAlt} fill sizes="(max-width: 960px) 100vw, 50vw" />
                  <div className="mini-product">
                    <small>SNOW ALPINE</small>
                    <b>{category.navTitle}</b>
                    <span>{index === 0 ? "Arrival ready" : index === 1 ? "All-day access" : index === 2 ? "Live availability" : "Collect on arrival"}</span>
                  </div>
                </div>
                <span className="bento-card__link">Explore <ArrowIcon /></span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="promise-section">
        <div className="shell promise-grid">
          <div className="promise-copy">
            <p className="eyebrow">Before you leave</p>
            <h2>One last check before you head uphill.</h2>
            <p>Mountain conditions can change quickly. Switch between the latest mountain, road and weather briefing so you know what to book, carry and wear.</p>
            <ul>
              <li><CheckIcon /> Check operating lifts and terrain</li>
              <li><CheckIcon /> Review road and chain requirements</li>
              <li><CheckIcon /> Dress for the mountain forecast</li>
            </ul>
          </div>
          <MountainBriefing />
        </div>
      </section>

      <section className="activity-section">
        <div className="shell">
          <div className="activity-heading">
            <div><p className="eyebrow">Winters past</p><h2>Made on the mountain.</h2></div>
            <p>Real days, first turns and the quiet moments in between—this is what a Snow Alpine winter feels like.</p>
          </div>
          <div className="activity-grid">
            {activities.map((activity, index) => (
              <figure className={`activity-card activity-card--${index + 1}`} key={activity.image}>
                <Image src={activity.image} alt={activity.alt} fill sizes="(max-width: 720px) 100vw, 50vw" />
                <figcaption><span>{activity.tag}</span><strong>{activity.title}</strong></figcaption>
              </figure>
            ))}
          </div>
        </div>
      </section>

      <section className="closing-cta">
        <div className="shell">
          <MountainIcon />
          <h2>Have a brilliant snow day.</h2>
          <p>We&apos;ll keep the booking simple. You enjoy the mountain.</p>
          <a className="button button--ink" href="#plan">Start planning <ArrowIcon /></a>
        </div>
      </section>
    </main>
  );
}
