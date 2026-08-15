import { Brand } from "./Brand";

/**
 * 登录 / 注册共用的分屏布局：左侧策展宣言，右侧表单。
 */
export function AuthShell({
  children,
  footer,
}: {
  children: React.ReactNode;
  footer?: React.ReactNode;
}) {
  return (
    <div className="relative z-10 grid min-h-screen grid-cols-1 lg:grid-cols-[1.05fr_0.95fr]">
      {/* 左：策展宣言 */}
      <div className="relative hidden flex-col justify-between overflow-hidden border-r border-line bg-paper-soft p-12 lg:flex">
        <Brand />
        <div className="max-w-md">
          <p className="eyebrow mb-6 animate-fade">A I · C U R A T O R</p>
          <h1 className="animate-rise font-serif text-[44px] font-semibold leading-[1.08] tracking-tight text-ink">
            把作品，
            <br />
            交给一位
            <br />
            <span className="italic text-ember">懂你</span>的策展师。
          </h1>
          <p className="delay-2 mt-7 max-w-sm animate-rise text-[15px] leading-relaxed text-ink-soft">
            零文案，一键生成属于你的个人门户。从一句灵感便签开始，让 AI 为你落笔成页。
          </p>
        </div>
        <div className="flex items-center gap-3 text-mute">
          <span className="font-mono text-[11px] uppercase tracking-wider3">
            PortFlow — 作品集策展平台
          </span>
        </div>
        {/* 装饰：右下角的大号序号 */}
        <span className="pointer-events-none absolute -bottom-10 -right-2 select-none font-serif text-[200px] leading-none text-line">
          01
        </span>
      </div>

      {/* 右：表单 */}
      <div className="flex flex-col">
        <div className="flex items-center justify-between p-6 lg:p-8">
          <div className="lg:hidden">
            <Brand />
          </div>
          <div className="hidden lg:block" />
          {footer}
        </div>
        <div className="flex flex-1 items-center justify-center px-6 pb-16 lg:px-8">
          <div className="w-full max-w-[380px]">{children}</div>
        </div>
      </div>
    </div>
  );
}
