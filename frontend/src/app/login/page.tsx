"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AuthShell } from "@/components/AuthShell";
import { api, ApiError } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await api.login(email.trim(), password);
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
        <Link href="/register" className="btn-link text-[13px]">
          没有账号？立即注册 →
        </Link>
      }
    >
      <div className="animate-rise">
        <p className="eyebrow mb-3">SIGN · IN</p>
        <h2 className="font-serif text-[30px] font-semibold leading-tight text-ink">欢迎回来</h2>
        <p className="mt-2 text-[14px] text-mute">登录后，继续策展你的作品。</p>

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
            <label className="label" htmlFor="password">
              密码
            </label>
            <input
              id="password"
              type="password"
              required
              autoComplete="current-password"
              placeholder="••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input"
            />
          </div>
          <button type="submit" disabled={loading} className="btn btn-ink mt-2 w-full">
            {loading ? "登录中…" : "登 录"}
          </button>
        </form>

        <p className="mt-6 text-center text-[12px] text-mute">
          演示账号 demo@example.com / demo123
        </p>
      </div>
    </AuthShell>
  );
}
