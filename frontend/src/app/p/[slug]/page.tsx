import { cache } from "react";
import { headers } from "next/headers";
import { notFound } from "next/navigation";
import type { Metadata } from "next";
import type { PortfolioPageData, Template } from "@/lib/types";
import { CardTemplate, GalleryTemplate, MagazineTemplate } from "./templates";
import { CustomPortalTemplate } from "./CustomPortalTemplate";
import { EditButton } from "./EditButton";

const BACKEND = process.env.BACKEND_URL || "http://127.0.0.1:8080";

const fetchPortfolio = cache(async (slug: string): Promise<PortfolioPageData | null> => {
  const cookie = headers().get("cookie") ?? "";
  const res = await fetch(`${BACKEND}/api/p/${encodeURIComponent(slug)}`, {
    headers: cookie ? { cookie } : undefined,
    cache: "no-store",
  });
  if (!res.ok) return null;
  return res.json();
});

export async function generateMetadata({
  params,
}: {
  params: { slug: string };
}): Promise<Metadata> {
  const data = await fetchPortfolio(params.slug);
  if (!data) return { title: "作品集不存在 · PortFlow" };
  return {
    title: data.seoTitle || `${data.userName} · 个人作品集`,
    description: data.seoDescription || `${data.userName} 的个人作品集`,
  };
}

export default async function PortfolioPage({ params }: { params: { slug: string } }) {
  const data = await fetchPortfolio(params.slug);
  if (!data) notFound();

  const template = (data.template || "card") as Template;
  let content;
  switch (template) {
    case "gallery":
      content = <GalleryTemplate p={data} />;
      break;
    case "magazine":
      content = <MagazineTemplate p={data} />;
      break;
    case "custom":
      content = <CustomPortalTemplate html={data.generatedHtml} userName={data.userName} />;
      break;
    case "card":
    default:
      content = <CardTemplate p={data} />;
      break;
  }

  return (
    <>
      {content}
      <EditButton slug={params.slug} />
    </>
  );
}
