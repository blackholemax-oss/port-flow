import type { CSSProperties } from "react";

/**
 * 无图片时的优雅占位符：
 * 基于主题色生成渐变背景 + 几何线条装饰，替代纯文本 "no image"。
 * 用于作品封面、头像等位置。
 */
export function CoverPlaceholder({
  themeColor = "#1A1714",
  className = "",
  ratio = "aspect-[4/3]",
  label = "作品",
}: {
  themeColor?: string;
  className?: string;
  ratio?: string;
  label?: string;
}) {
  const style: CSSProperties = {
    background: `linear-gradient(135deg, ${themeColor}14 0%, ${themeColor}06 50%, ${themeColor}0F 100%)`,
  };
  return (
    <div
      style={style}
      className={`${className} ${ratio} relative flex w-full items-center justify-center overflow-hidden`}
    >
      {/* 几何装饰线条 */}
      <svg
        className="absolute inset-0 h-full w-full opacity-[0.12]"
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        <circle cx="80" cy="20" r="22" fill="none" stroke={themeColor} strokeWidth="0.6" />
        <circle cx="20" cy="85" r="14" fill="none" stroke={themeColor} strokeWidth="0.6" />
        <line x1="0" y1="65" x2="100" y2="35" stroke={themeColor} strokeWidth="0.4" />
      </svg>
      {/* 中心图标 */}
      <div className="relative flex flex-col items-center gap-2">
        <svg
          width="32"
          height="32"
          viewBox="0 0 24 24"
          fill="none"
          stroke={themeColor}
          strokeWidth="1.2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="opacity-40"
          aria-hidden="true"
        >
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
        <span
          className="font-mono text-[10px] uppercase tracking-wider2"
          style={{ color: themeColor, opacity: 0.45 }}
        >
          {label}
        </span>
      </div>
    </div>
  );
}
