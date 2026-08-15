"use client";

import { useEffect, useState } from "react";
import Link from "next/link";

/**
 * 公共门户页的浮动编辑按钮：
 * 尝试调用编辑接口，成功则说明当前登录用户是该作品集所有者，显示「编辑」按钮。
 * 未登录或非所有者时静默隐藏（不触发登录重定向）。
 */
export function EditButton({ slug }: { slug: string }) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    // 直接 fetch，不走 api.ts 的 request（避免 401 时重定向到 /login）
    fetch(`/api/admin/portfolios/${encodeURIComponent(slug)}`, {
      credentials: "include",
      headers: { Accept: "application/json" },
    })
      .then((res) => {
        if (res.ok) setVisible(true);
      })
      .catch(() => {});
  }, [slug]);

  if (!visible) return null;

  return (
    <Link
      href={`/admin/editor?slug=${encodeURIComponent(slug)}`}
      className="fixed bottom-6 right-6 z-50 flex items-center gap-2 rounded-full bg-ink px-5 py-3 text-[13px] font-medium text-paper-soft shadow-lg transition-all hover:scale-105 hover:bg-ink-soft"
    >
      <svg
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      >
        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
      </svg>
      编辑门户
    </Link>
  );
}
