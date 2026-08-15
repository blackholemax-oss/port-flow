"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AuthShell } from "@/components/AuthShell";
import { api, ApiError } from "@/lib/api";

export default function RegisterPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await api.register(email.trim(), password, displayName.trim() || undefined);
      router.push("/admin");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "网络异常，请稍后重试");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell
      footer={
        <Link href="/login" className="btn-link text-[13px]">
          已有账号？去登录 →
        </Link>
      }
    >
      <div className="animate-rise">
        <p className="eyebrow mb-3">CREATE · ACCOUNT</p>
        <h2 className="font-serif text-[30px] font-semibold leading-tight text-ink">
          开启你的策展
        </h2>
        <p className="mt-2 text-[14px] text-mute">注册后，立刻拥有第一个作品集。</p>

        {error && (
          <div className="mt-6 rounded-lg border border-ember/30 bg-ember/5 px-3.5 py-2.5 text-[13px] text-ember-deep">
            {error}
          </div>
        )}

        <form onSubmit={onSubmit} className="mt-7 space-y-4">
          <div>
            <label className="label" htmlFor="email">
              邮箱
            </label>
            <input
              id="email"
              type="email"
              required
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input"
            />
          </div>
          <div>
            <label className="label" htmlFor="displayName">
              昵称 <span className="hint">不填则取邮箱前缀</span>
            </label>
            <input
              id="displayName"
              type="text"
              placeholder="如何称呼你"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="input"
            />
          </div>
          <div>
            <label className="label" htmlFor="password">
              密码 <span className="hint">至少 6 位</span>
            </label>
            <input
              id="password"
              type="password"
              required
              minLength={6}
              autoComplete="new-password"
              placeholder="••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input"
            />
          </div>
          <button type="submit" disabled={loading} className="btn btn-ember mt-2 w-full">
            {loading ? "创建中…" : "注册并开始"}
          </button>
        </form>
      </div>
    </AuthShell>
  );
}
