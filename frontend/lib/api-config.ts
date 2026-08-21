const LOCAL_API_BASE_URL = "http://localhost:8080";
const PRODUCTION_API_BASE_URL = "https://api.snowalpineresort.com";

function normalizeBaseUrl(value: string) {
  return value.replace(/\/+$/, "");
}

export const publicApiBaseUrl = normalizeBaseUrl(
  process.env.NEXT_PUBLIC_API_BASE_URL
    ?? (process.env.NODE_ENV === "production" ? PRODUCTION_API_BASE_URL : LOCAL_API_BASE_URL),
);

export const serverApiBaseUrl = normalizeBaseUrl(
  process.env.API_BASE_URL ?? publicApiBaseUrl,
);
