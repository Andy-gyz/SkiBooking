import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { CategoryIcon } from "@/components/category-icon";
import { ArrowIcon } from "@/components/icons";
import { categories, findCategory } from "@/lib/categories";
import { getProducts, type Product } from "@/lib/catalog";

export const dynamic = "force-dynamic";

type CategoryPageProps = { params: Promise<{ category: string }> };

export function generateStaticParams() {
  return categories.map(({ slug }) => ({ category: slug }));
}

export async function generateMetadata({ params }: CategoryPageProps): Promise<Metadata> {
  const { category: slug } = await params;
  const category = findCategory(slug);
  if (!category) return {};
  return { title: category.navTitle, description: category.description };
}

function ProductCard({ product, priceSuffix }: { product: Product; priceSuffix: string }) {
  const price = new Intl.NumberFormat("en-AU", { style: "currency", currency: product.currency, minimumFractionDigits: 0 }).format(product.price);

  return (
    <article className="product-card">
      <div className="product-card__visual">
        <span>{product.resort.name}</span>
        <CategoryIcon category={product.category} />
      </div>
      <div className="product-card__body">
        <div className="product-card__location">{product.resort.location}</div>
        <h2>{product.name}</h2>
        <p>{product.description}</p>
        <div className="product-card__footer">
          <div className="price"><span>From</span><strong>{price}</strong><small>{priceSuffix}</small></div>
          <span className="button button--ink button--disabled" aria-label="Product configuration will be added in the next milestone">Select options <ArrowIcon /></span>
        </div>
      </div>
    </article>
  );
}

export default async function CategoryPage({ params }: CategoryPageProps) {
  const { category: slug } = await params;
  const category = findCategory(slug);
  if (!category) notFound();

  let products: Product[] = [];
  let unavailable = false;
  try {
    products = await getProducts(category.category);
  } catch {
    unavailable = true;
  }

  return (
    <main>
      <section className={`catalog-hero catalog-hero--${category.accent}`}>
        <div className="shell catalog-hero__inner">
          <div>
            <nav className="breadcrumbs" aria-label="Breadcrumb"><Link href="/">Home</Link><span>/</span><span>{category.navTitle}</span></nav>
            <p className="eyebrow eyebrow--light">{category.eyebrow}</p>
            <h1>{category.title}</h1>
            <p>{category.description}</p>
          </div>
          <div className="catalog-hero__icon"><CategoryIcon category={category.category} /></div>
        </div>
      </section>

      <section className="catalog-section">
        <div className="shell">
          <div className="catalog-heading">
            <div><p className="eyebrow">Book online</p><h2>{category.navTitle}</h2></div>
            {!unavailable && <span>{products.length} {products.length === 1 ? "option" : "options"} available</span>}
          </div>
          {unavailable ? (
            <div className="catalog-state" role="alert"><span>Connection paused</span><h2>We couldn&apos;t reach the booking service.</h2><p>Check that the backend is running on port 8080, then refresh this page.</p></div>
          ) : products.length === 0 ? (
            <div className="catalog-state"><span>More snow days are coming</span><h2>No products are available just yet.</h2><p>Please check back soon for the latest season release.</p></div>
          ) : (
            <div className="product-grid">{products.map((product) => <ProductCard product={product} priceSuffix={category.priceSuffix} key={product.id} />)}</div>
          )}
        </div>
      </section>
    </main>
  );
}
