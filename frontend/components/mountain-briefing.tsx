"use client";

import Link from "next/link";
import { useState } from "react";

import { ArrowIcon } from "@/components/icons";

const briefing = {
  mountain: { label: "Mountain", value: "18/22", unit: "lifts open", eyebrow: "Terrain update", title: "Most of the mountain is ready.", detail: "North Ridge and the beginner zone are operating today.", href: "/lift-tickets", action: "Choose a lift pass" },
  roads: { label: "Roads", value: "OPEN", unit: "alpine road", eyebrow: "Road update", title: "The resort approach is open.", detail: "Carry snow chains and fit them when directed by resort staff.", href: "/resort-entry", action: "Organise resort entry" },
  weather: { label: "Weather", value: "−2°", unit: "snow showers", eyebrow: "Weather update", title: "Cold turns are on the way.", detail: "Layer up for light snow and a moderate north-westerly wind.", href: "/rentals", action: "Reserve your equipment" },
} as const;

type BriefingKey = keyof typeof briefing;

export function MountainBriefing() {
  const [active, setActive] = useState<BriefingKey>("mountain");
  const current = briefing[active];

  return (
    <div className="promise-panel mountain-briefing">
      <div className="briefing-tabs" role="tablist" aria-label="Mountain briefing">
        {(Object.keys(briefing) as BriefingKey[]).map((key) => (
          <button type="button" role="tab" aria-selected={active === key} onClick={() => setActive(key)} key={key}>{briefing[key].label}</button>
        ))}
      </div>
      <div className="promise-panel__orb briefing-reading" aria-live="polite"><strong>{current.value}</strong><span>{current.unit}</span></div>
      <div className="promise-panel__message" aria-live="polite">
        <span>{current.eyebrow}</span>
        <b>{current.title}</b>
        <small>{current.detail}</small>
        <Link href={current.href}>{current.action} <ArrowIcon /></Link>
      </div>
    </div>
  );
}
