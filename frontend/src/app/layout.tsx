import type { Metadata } from "next";
import { Fraunces, Spline_Sans, Spline_Sans_Mono } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";
import { Toaster } from "@/components/ui/sonner";

const fraunces = Fraunces({
  subsets: ["latin"],
  variable: "--font-fraunces",
  display: "swap",
  weight: ["400", "500", "600", "700"],
  style: ["normal", "italic"],
});

const spline = Spline_Sans({
  subsets: ["latin"],
  variable: "--font-spline",
  display: "swap",
  weight: ["400", "500", "600", "700"],
});

const mono = Spline_Sans_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
  display: "swap",
  weight: ["400", "500", "600"],
});

export const metadata: Metadata = {
  title: "PortFlow · AI 策展师",
  description: "为创作者打造的 AI 作品集策展平台 —— 零文案，一键生成你的个人门户。",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN" className={cn(fraunces.variable, spline.variable, mono.variable, "font-sans")}>
      <body className="relative">
        {children}
        <Toaster richColors position="top-center" />
      </body>
    </html>
  );
}
