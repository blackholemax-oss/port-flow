import type { CSSProperties } from "react";
import type { PortfolioPageData } from "@/lib/types";
import { CoverPlaceholder } from "@/components/CoverPlaceholder";

function splitSkills(skills?: string): string[] {
  if (!skills) return [];
  return skills
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

const accent = (color?: string): CSSProperties =>
  ({ "--accent": color || "#1A1714" }) as CSSProperties;

function Cover({
  src,
  className = "",
  ratio = "aspect-[4/3]",
  themeColor = "#1A1714",
  label = "作品",
}: {
  src?: string | null;
  className?: string;
  ratio?: string;
  themeColor?: string;
  label?: string;
}) {
  if (src) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img src={src} alt="" className={`${className} ${ratio} w-full object-cover`} />
    );
  }
  return (
    <CoverPlaceholder themeColor={themeColor} className={className} ratio={ratio} label={label} />
  );
}

function AvatarOrInitial({
  p,
  size,
  className = "",
}: {
  p: PortfolioPageData;
  size: number;
  className?: string;
}) {
  if (p.avatarPath) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={p.avatarPath}
        alt={p.userName}
        style={{ width: size, height: size }}
        className={`rounded-full object-cover ${className}`}
      />
    );
  }
  return (
    <div
      style={{
        width: size,
        height: size,
        backgroundColor: p.themeColor || "#1A1714",
        fontSize: size * 0.4,
      }}
      className={`flex items-center justify-center rounded-full font-serif font-semibold text-paper-soft ${className}`}
    >
      {p.userName?.[0] ?? "?"}
    </div>
  );
}

/** 模板一：极简卡片 —— 暖纸底色，居中编排 */
export function CardTemplate({ p }: { p: PortfolioPageData }) {
  const skills = splitSkills(p.skills);
  return (
    <div className="min-h-screen bg-paper" style={accent(p.themeColor)}>
      <div className="mx-auto max-w-3xl px-6 py-20 md:py-28">
        <header className="flex animate-rise flex-col items-center text-center">
          <AvatarOrInitial p={p} size={112} className="border-4 border-paper-soft shadow-lg" />
          <h1 className="mt-6 font-serif text-[34px] font-semibold tracking-tight text-ink md:text-[44px]">
            {p.userName}
          </h1>
          {p.slogan && (
            <span className="mt-3 inline-block rounded-full bg-[var(--accent)] px-4 py-1 text-[13px] font-medium text-paper-soft">
              {p.slogan}
            </span>
          )}
          {p.bio && (
            <p className="mt-6 max-w-xl text-[15px] leading-[1.8] text-ink-soft">{p.bio}</p>
          )}
          {skills.length > 0 && (
            <div className="mt-6 flex flex-wrap justify-center gap-2">
              {skills.map((s) => (
                <span
                  key={s}
                  className="rounded-full border-[color:var(--accent)] bg-paper-soft px-3 py-1 text-[12px] text-[color:var(--accent)]"
                >
                  {s}
                </span>
              ))}
            </div>
          )}
        </header>

        <div className="my-14 h-px w-full bg-line" />

        {p.projects.length > 0 && (
          <section>
            <div className="mb-7 flex items-center gap-3">
              <span className="font-mono text-[11px] uppercase tracking-wider3 text-mute">
                WORKS · {String(p.projects.length).padStart(2, "0")}
              </span>
              <span className="h-px flex-1 bg-line" />
            </div>
            <div className="grid grid-cols-1 gap-7 sm:grid-cols-2">
              {p.projects.map((proj, i) => (
                <article
                  key={i}
                  className="group overflow-hidden rounded-xl border border-line bg-paper-soft transition-all duration-300 hover:-translate-y-1 hover:shadow-xl"
                >
                  <Cover
                    src={proj.coverPath}
                    className="rounded-t-xl"
                    ratio="h-40"
                    themeColor={p.themeColor}
                    label={proj.title}
                  />
                  <div className="p-5">
                    <h3 className="font-serif text-[18px] font-semibold text-ink">{proj.title}</h3>
                    {proj.description && (
                      <p className="mt-2 text-[13px] leading-relaxed text-ink-soft">
                        {proj.description}
                      </p>
                    )}
                  </div>
                </article>
              ))}
            </div>
          </section>
        )}

        <footer className="mt-20 text-center font-mono text-[11px] uppercase tracking-wider3 text-mute">
          Powered by {p.userName} · AI 策展师
        </footer>
      </div>
    </div>
  );
}

/** 模板二：画廊大图 —— 暖调暗色沉浸 */
export function GalleryTemplate({ p }: { p: PortfolioPageData }) {
  const skills = splitSkills(p.skills);
  return (
    <div
      className="min-h-screen text-[#EDE6DA]"
      style={{ ...accent(p.themeColor || "#C2521F"), background: "#161210" }}
    >
      <div className="mx-auto max-w-6xl px-6 py-16 md:py-24">
        <header className="flex animate-rise flex-col items-center text-center">
          <AvatarOrInitial p={p} size={120} className="border-4 border-white/10" />
          <h1 className="mt-6 font-serif text-[40px] font-bold tracking-tight md:text-[56px]">
            {p.userName}
          </h1>
          {p.slogan && (
            <span className="mt-4 inline-block rounded-full bg-[var(--accent)] px-5 py-1.5 text-[14px] font-medium text-[#161210]">
              {p.slogan}
            </span>
          )}
          {p.bio && (
            <p className="mt-6 max-w-xl text-[15px] leading-[1.9] text-[#9C9486]">{p.bio}</p>
          )}
          {skills.length > 0 && (
            <div className="mt-6 flex flex-wrap justify-center gap-2">
              {skills.map((s) => (
                <span
                  key={s}
                  className="rounded-full border border-white/15 px-3 py-1 text-[12px] text-[#C9C0B0]"
                >
                  {s}
                </span>
              ))}
            </div>
          )}
        </header>

        {p.projects.length > 0 && (
          <div className="mt-14 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {p.projects.map((proj, i) => (
              <figure key={i} className="group relative overflow-hidden rounded-2xl bg-[#211B16]">
                <div className="aspect-[4/3] w-full overflow-hidden">
                  <Cover
                    src={proj.coverPath}
                    className="h-full w-full transition-transform duration-700 group-hover:scale-110"
                    ratio="h-full"
                    themeColor={p.themeColor}
                    label={proj.title}
                  />
                </div>
                <figcaption className="absolute inset-0 flex flex-col justify-end bg-gradient-to-t from-black/85 via-black/10 to-transparent p-5 opacity-0 transition-opacity duration-300 group-hover:opacity-100">
                  <h3 className="font-serif text-[19px] font-semibold">{proj.title}</h3>
                  {proj.description && (
                    <p className="mt-1.5 text-[13px] leading-relaxed text-[#D8D0C2]">
                      {proj.description}
                    </p>
                  )}
                </figcaption>
              </figure>
            ))}
          </div>
        )}

        <footer className="mt-20 text-center font-mono text-[11px] uppercase tracking-wider3 text-[#6B6357]">
          Powered by {p.userName} · AI 策展师
        </footer>
      </div>
    </div>
  );
}

/** 模板三：杂志风 —— 衬线编辑式排版 */
export function MagazineTemplate({ p }: { p: PortfolioPageData }) {
  const skills = splitSkills(p.skills);
  return (
    <div
      className="min-h-screen bg-[#FBF8F1] text-[#1C1917]"
      style={accent(p.themeColor || "#1C1917")}
    >
      <div className="mx-auto max-w-3xl px-6 py-16 md:py-24">
        {/* 报头 */}
        <header className="flex animate-rise flex-col items-center border-b-2 border-[#1C1917] pb-10 text-center">
          <AvatarOrInitial p={p} size={92} className="border-[3px] border-white shadow-md" />
          <h1 className="mt-5 font-serif text-[44px] font-bold tracking-[2px] md:text-[60px]">
            {p.userName}
          </h1>
          {p.slogan && (
            <p className="mt-3 font-serif text-[17px] italic text-[#57534E]">{p.slogan}</p>
          )}
          {p.bio && (
            <p className="mt-5 max-w-lg text-[15px] leading-[1.9] text-[#57534E]">{p.bio}</p>
          )}
        </header>

        {skills.length > 0 && (
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            {skills.map((s) => (
              <span
                key={s}
                className="font-mono text-[11px] uppercase tracking-wider2 text-[color:var(--accent)]"
              >
                {s}
              </span>
            ))}
          </div>
        )}

        {p.projects.length > 0 && (
          <section className="mt-14">
            <div className="mb-2 flex items-center gap-3">
              <span className="font-serif text-[20px] font-bold tracking-[4px] text-[color:var(--accent)]">
                WORKS
              </span>
              <span className="h-px flex-1 bg-[#E7E5E4]" />
            </div>

            {p.projects.map((proj, i) => {
              const reverse = i % 2 === 1;
              return (
                <article
                  key={i}
                  className={`grid grid-cols-1 items-center gap-8 border-b border-[#E7E5E4] py-10 md:grid-cols-2 ${
                    reverse ? "md:[direction:rtl]" : ""
                  }`}
                >
                  <div className="md:[direction:ltr]">
                    <p className="font-mono text-[12px] tracking-[2px] text-[color:var(--accent)]">
                      NO.{String(i + 1).padStart(2, "0")}
                    </p>
                    <h3 className="mt-2 font-serif text-[26px] font-bold">{proj.title}</h3>
                    {proj.description && (
                      <p className="mt-3 text-[15px] leading-[1.9] text-[#57534E]">
                        {proj.description}
                      </p>
                    )}
                  </div>
                  <div className="md:[direction:ltr]">
                    <Cover
                      src={proj.coverPath}
                      className="rounded-sm"
                      ratio="aspect-[5/4]"
                      themeColor={p.themeColor}
                      label={proj.title}
                    />
                  </div>
                </article>
              );
            })}
          </section>
        )}

        <footer className="mt-16 text-center font-serif text-[13px] italic text-[#A8A29E]">
          Powered by {p.userName} · AI 策展师
        </footer>
      </div>
    </div>
  );
}
