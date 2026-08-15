import { cn } from "@/lib/cn";

export function Avatar({
  src,
  name,
  themeColor = "#1A1714",
  size = 64,
  className = "",
}: {
  src?: string | null;
  name?: string;
  themeColor?: string;
  size?: number;
  className?: string;
}) {
  const initial = name && name.length > 0 ? name[0] : "?";
  if (src) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={src}
        alt={name ?? "头像"}
        style={{ width: size, height: size }}
        className={cn("rounded-full object-cover", className)}
      />
    );
  }
  return (
    <div
      style={{ width: size, height: size, backgroundColor: themeColor, fontSize: size * 0.42 }}
      className={cn(
        "flex items-center justify-center rounded-full font-serif font-semibold text-paper-soft",
        className,
      )}
    >
      {initial}
    </div>
  );
}
