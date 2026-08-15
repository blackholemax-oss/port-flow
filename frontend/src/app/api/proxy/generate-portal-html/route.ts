import { NextRequest } from "next/server";

/**
 * AI 门户 HTML 生成的代理路由：
 * Next.js rewrites 代理默认超时 30s，但 LLM 生成 HTML 需要 60-90s。
 * 使用 API route 手动转发请求，设置 120s 超时，避免 ECONNRESET。
 */
const BACKEND = process.env.BACKEND_URL || "http://127.0.0.1:8080";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";
export const maxDuration = 300;

export async function POST(request: NextRequest) {
  const body = await request.text();
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 170000);

  try {
    const res = await fetch(`${BACKEND}/api/admin/ai/generate-portal-html`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        cookie: request.headers.get("cookie") ?? "",
        Accept: "application/json",
      },
      body,
      signal: controller.signal,
    });

    const text = await res.text();
    return new Response(text, {
      status: res.status,
      headers: { "Content-Type": "application/json" },
    });
  } catch (err) {
    const msg =
      err instanceof Error && err.name === "AbortError"
        ? "AI 生成超时，请稍后重试"
        : "代理请求失败";
    return new Response(JSON.stringify({ msg }), {
      status: 504,
      headers: { "Content-Type": "application/json" },
    });
  } finally {
    clearTimeout(timer);
  }
}
