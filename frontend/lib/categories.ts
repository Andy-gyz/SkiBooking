import type { ProductCategory } from "@/lib/catalog";

export type CategoryConfig = {
  slug: string;
  category: ProductCategory;
  navTitle: string;
  shortTitle: string;
  title: string;
  eyebrow: string;
  description: string;
  cardDescription: string;
  priceSuffix: string;
  accent: "ice" | "sun" | "coral" | "pine";
  photo: string;
  photoAlt: string;
};

export const categories: CategoryConfig[] = [
  { slug: "resort-entry", category: "RESORT_ACCESS", navTitle: "Resort Entry", shortTitle: "Resort Entry", title: "Arrive ready for the mountain", eyebrow: "Entry & parking", description: "Reserve vehicle access before you travel, so your snow day starts smoothly from the moment you arrive.", cardDescription: "Vehicle entry and mountain parking, organised before you arrive.", priceSuffix: "per vehicle", accent: "ice", photo: "/images/resort-entry-car.jpg", photoAlt: "A vehicle driving along a snow-covered mountain road" },
  { slug: "lift-tickets", category: "LIFT_TICKET", navTitle: "Lift Tickets", shortTitle: "Lift Tickets", title: "Your pass to more mountain", eyebrow: "Lift access", description: "Choose your lift access and spend less time in a queue—and more time chasing fresh turns.", cardDescription: "Full-day mountain access for skiers and snowboarders.", priceSuffix: "per person", accent: "sun", photo: "/images/lift-pass.jpg", photoAlt: "Chairlifts travelling above a snowy ski slope" },
  { slug: "lessons", category: "LESSON", navTitle: "Lessons", shortTitle: "Lessons", title: "Find your feet. Then your flow.", eyebrow: "Ski & snowboard school", description: "Friendly instruction, small groups and clear session availability for your next step on snow.", cardDescription: "Build confidence with friendly, session-based instruction.", priceSuffix: "per participant", accent: "coral", photo: "/images/ski-lesson.jpg", photoAlt: "A ski instructor helping a learner on the mountain" },
  { slug: "rentals", category: "RENTAL", navTitle: "Rentals", shortTitle: "Equipment Rental", title: "Great gear, ready when you are", eyebrow: "Mountain equipment", description: "Reserve your equipment ahead of time and collect everything you need when you reach the resort.", cardDescription: "Skis, snowboards and essential equipment, fitted for your day.", priceSuffix: "per package", accent: "pine", photo: "/images/equipment-rental.jpg", photoAlt: "Ski equipment ready for collection outside a mountain lodge" },
];

export function findCategory(slug: string) {
  return categories.find((category) => category.slug === slug);
}
