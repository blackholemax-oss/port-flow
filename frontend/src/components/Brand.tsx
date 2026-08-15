import Link from "next/link";

export function Brand({ className = "", href = "/" }: { className?: string; href?: string }) {
  return (
    <Link href={href} className={`group inline-flex items-center gap-2.5 ${className}`}>
      <span className="relative flex h-7 w-7 items-center justify-center">
        <span className="absolute inset-0 rotate-45 rounded-[3px] border border-ink transition-transform duration-500 group-hover:rotate-[135deg]" />
        <span className="h-2 w-2 rounded-full bg-ember transition-transform duration-300 group-hover:scale-125" />
      </span>
      <span className="font-serif text-[19px] font-semibold tracking-tight text-ink">
        Port<span className="text-ember">·</span>Flow
      </span>
    </Link>
  );
}
