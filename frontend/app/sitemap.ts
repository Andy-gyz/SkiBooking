import type { MetadataRoute } from "next";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "https://snowalpineresort.com";

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: siteUrl, changeFrequency: "weekly", priority: 1 },
    { url: `${siteUrl}/resort-entry`, changeFrequency: "daily", priority: 0.9 },
    { url: `${siteUrl}/lift-tickets`, changeFrequency: "daily", priority: 0.9 },
    { url: `${siteUrl}/lessons`, changeFrequency: "daily", priority: 0.9 },
    { url: `${siteUrl}/rentals`, changeFrequency: "daily", priority: 0.9 },
  ];
}
