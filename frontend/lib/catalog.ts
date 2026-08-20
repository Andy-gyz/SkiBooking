export type ProductCategory = "RESORT_ACCESS" | "LIFT_TICKET" | "LESSON" | "RENTAL";

export type ResortSummary = {
  id: number;
  name: string;
  location: string;
};

export type Product = {
  id: number;
  resort: ResortSummary;
  name: string;
  category: ProductCategory;
  description: string | null;
  price: number;
  currency: string;
  imageUrl: string | null;
};

const apiBaseUrl = process.env.API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function getProducts(category: ProductCategory): Promise<Product[]> {
  const response = await fetch(`${apiBaseUrl}/api/products?category=${encodeURIComponent(category)}`, { cache: "no-store" });
  if (!response.ok) throw new Error(`Catalog request failed with status ${response.status}`);
  return response.json() as Promise<Product[]>;
}
