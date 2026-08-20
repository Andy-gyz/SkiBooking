import Link from "next/link";

import { CategoryIcon } from "@/components/category-icon";
import { ArrowIcon, CheckIcon, MountainIcon } from "@/components/icons";
import type { ProductCategory } from "@/lib/catalog";

export type PlannerItem = {
  slug: string;
  category: ProductCategory;
  title: string;
  accent: "ice" | "sun" | "coral" | "pine";
  unitPrice: number;
  perGuest: boolean;
};

type DayPlannerProps = { items: PlannerItem[] };

export function DayPlanner({ items }: DayPlannerProps) {
  const total = items.reduce((sum, item) => sum + item.unitPrice, 0);
  const firstItem = items[0];

  return (
    <div className="booking-showcase" aria-label="Build your Snow Alpine day">
      <div className="showcase-glow" />
      <div className="showcase-mountain" aria-hidden="true"><MountainIcon /></div>
      <div className="day-card">
        <div className="day-card__top">
          <div><MountainIcon /><span><b>Snow Alpine</b><small>Victoria, Australia</small></span></div>
          <span className="guest-count"><small>Guests</small><b>1</b></span>
        </div>
        <div className="day-card__date">
          <span>YOUR SNOW DAY</span>
          <strong>25 AUG</strong>
        </div>
        <div className="day-card__status"><i /> Everything is ready</div>
        <div className="day-card__items">
          {items.map((item) => (
              <div key={item.slug}>
                <span className={`mini-icon mini-icon--${item.accent}`}><CategoryIcon category={item.category} /></span>
                <span><b>{item.title}</b><small>{item.perGuest ? `$${item.unitPrice} per guest` : `$${item.unitPrice} per vehicle`}</small></span>
                <span className="item-toggle is-complete" aria-hidden="true"><CheckIcon /></span>
              </div>
          ))}
        </div>
        <div className="day-card__total">
          <span><small>Estimated total</small><strong>${total} AUD</strong></span>
          <Link className="start-booking" href={firstItem ? `/${firstItem.slug}` : "/#plan"}>Start booking <ArrowIcon /></Link>
        </div>
      </div>
      <div className="float-note float-note--left"><span>YOUR ITINERARY</span><b>Four essentials together</b><small>One clear place to begin</small></div>
      <div className="float-note float-note--right"><span>NEXT STEP</span><b>Start with Resort Entry</b><small>1 guest · 25 AUG</small></div>
    </div>
  );
}
