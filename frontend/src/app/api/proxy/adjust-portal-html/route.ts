import { NextRequest } from "next/server";

/**
 * AI 对话修改门户 HTML 的代理路由：
 * 用户输入自然语言修改指令，AI 在现有 HTML 基础上调整。
 * LLM 调用耗时较长，使用 API route 手动转发，设置 170s 超时。
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
    const res = await fetch(`${BACKEND}/api/admin/ai/adjust-portal-html`, {
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
        ? "AI 调整超时，请稍后重试"
        : "请求失败，请稍后重试";
    return new Response(JSON.stringify({ msg }), {
      status: 504,
      headers: { "Content-Type": "application/json" },
    });
  } finally {
    clearTimeout(timer);
  }
}
