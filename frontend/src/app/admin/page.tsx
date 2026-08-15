"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Brand } from "@/components/Brand";
import { Avatar } from "@/components/Avatar";
import { api, ApiError } from "@/lib/api";
import type { PortfolioSummary, Template } from "@/lib/types";

const TEMPLATE_LABEL: Record<Template, string> = {
  card: "极简卡片",
  gallery: "画廊大图",
  magazine: "杂志风",
};

export default function DashboardPage() {
  const router = useRouter();
  const [items, setItems] = useState<PortfolioSummary[] | null>(null);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      setItems(await api.listPortfolios());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "加载失败");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function logout() {
    try {
      await api.logout();
    } catch {
      /* ignore */
    }
    router.push("/login");
  }

  const list = items ?? [];
  const totalPv = list.reduce((s, p) => s + p.pv, 0);
  const totalUv = list.reduce((s, p) => s + p.uv, 0);

  return (
    <div className="relative z-10 min-h-screen">
      {/* 顶栏 */}
      <header className="border-b border-line">
        <div className="container-page flex h-16 items-center justify-between">
          <Brand href="/admin" />
          <button onClick={logout} className="btn btn-ghost text-[13px]">
            退出登录
          </button>
        </div>
      </header>

      <main className="container-page py-12 md:py-16">
        {/* 标题区 */}
        <div className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
          <div className="animate-rise">
            <p className="eyebrow mb-3">DASHBOARD · 仪表盘</p>
            <h1 className="font-serif text-[40px] font-semibold leading-none tracking-tight text-ink md:text-[52px]">
              我的作品集
            </h1>
            <p className="mt-4 max-w-md text-[14px] leading-relaxed text-ink-soft">
              管理你的策展作品，追踪每一次被看见的瞬间。
            </p>
          </div>
          <Link href="/admin/editor" className="btn btn-ember shrink-0">
            + 新建作品集
          </Link>
        </div>

        {/* 统计带 */}
        <div className="mt-12 grid grid-cols-3 border-y border-line">
          <Stat label="作品集" value={list.length} />
          <Stat label="累计访问 · PV" value={totalPv} bordered />
          <Stat label="独立访客 · UV" value={totalUv} bordered />
        </div>

        {/* 列表 */}
        <section className="mt-10">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-serif text-[20px] font-semibold text-ink">全部作品</h2>
            {list.length > 0 && (
              <span className="font-mono text-[12px] text-mute">共 {list.length} 项</span>
            )}
          </div>

          {error ? (
            <div className="rounded-xl border border-ember/30 bg-ember/5 px-4 py-3 text-[13px] text-ember-deep">
              {error}
            </div>
          ) : items === null ? (
            <SkeletonRows />
          ) : list.length === 0 ? (
            <EmptyState />
          ) : (
            <ul className="divide-y divide-line border-y border-line">
              {list.map((p, i) => (
                <li
                  key={p.id}
                  className="group flex flex-wrap items-center gap-5 py-5 transition-colors hover:bg-paper-soft/60 md:flex-nowrap"
                >
                  <span className="w-8 shrink-0 font-mono text-[12px] text-mute">
                    {String(i + 1).padStart(2, "0")}
                  </span>
                  <Avatar
                    src={p.avatarPath}
                    name={p.userName}
                    themeColor={p.themeColor}
                    size={48}
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2.5">
                      <h3 className="truncate font-serif text-[18px] font-semibold text-ink">
                        {p.userName}
                      </h3>
                      <span className="rounded-full bg-line/60 px-2 py-0.5 font-mono text-[10px] uppercase tracking-wider2 text-ink-soft">
                        {TEMPLATE_LABEL[p.template] ?? p.template}
                      </span>
                      {!p.isPublished && (
                        <span className="rounded-full bg-ember/10 px-2 py-0.5 font-mono text-[10px] uppercase tracking-wider2 text-ember-deep">
                          草稿
                        </span>
                      )}
                    </div>
                    <p className="mt-0.5 truncate text-[13px] text-mute">
                      {p.slogan || "—"} · <span className="font-mono">/p/{p.slug}</span>
                    </p>
                  </div>

                  <div className="flex items-center gap-6 pr-2">
                    <Metric label="PV" value={p.pv} />
                    <Metric label="UV" value={p.uv} />
                  </div>

                  <div className="flex items-center gap-2">
                    <Link
                      href={`/admin/editor?slug=${encodeURIComponent(p.slug)}`}
                      className="btn btn-ghost text-[13px]"
                    >
                      编辑
                    </Link>
                    <Link
                      href={`/p/${encodeURIComponent(p.slug)}`}
                      target="_blank"
                      className="btn btn-ink text-[13px]"
                    >
                      查看
                    </Link>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}

function Stat({ label, value, bordered }: { label: string; value: number; bordered?: boolean }) {
  return (
    <div className={`px-2 py-7 md:px-6 ${bordered ? "border-l border-line" : ""}`}>
      <p className="font-mono text-[11px] uppercase tracking-wider3 text-mute">{label}</p>
      <p className="mt-3 font-serif text-[40px] font-semibold leading-none text-ink md:text-[52px]">
        {value.toLocaleString()}
      </p>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="text-right">
      <p className="font-mono text-[10px] uppercase tracking-wider2 text-mute">{label}</p>
      <p className="font-serif text-[20px] font-semibold text-ink">{value.toLocaleString()}</p>
    </div>
  );
}

function SkeletonRows() {
  return (
    <ul className="divide-y divide-line border-y border-line">
      {[0, 1, 2].map((i) => (
        <li key={i} className="flex items-center gap-5 py-5">
          <div className="h-3 w-8 animate-pulse rounded bg-line" />
          <div className="h-12 w-12 animate-pulse rounded-full bg-line" />
          <div className="flex-1 space-y-2">
            <div className="h-4 w-1/3 animate-pulse rounded bg-line" />
            <div className="h-3 w-1/2 animate-pulse rounded bg-line" />
          </div>
        </li>
      ))}
    </ul>
  );
}

function EmptyState() {
  return (
    <div className="relative overflow-hidden rounded-2xl border border-dashed border-line bg-paper-soft py-20 text-center">
      <span className="pointer-events-none absolute -top-6 right-6 select-none font-serif text-[140px] leading-none text-line/70">
        ✦
      </span>
      <h3 className="font-serif text-[24px] font-semibold text-ink">空白的画廊</h3>
      <p className="mx-auto mt-3 max-w-sm text-[14px] leading-relaxed text-mute">
        还没有任何作品集。点击下方按钮，让 AI 为你策展第一个个人门户。
      </p>
      <Link href="/admin/editor" className="btn btn-ember mt-7">
        + 新建作品集
      </Link>
    </div>
  );
}
