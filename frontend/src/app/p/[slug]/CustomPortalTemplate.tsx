/**
 * AI 定制门户模板：
 * 将大模型生成的完整自包含 HTML 通过 sandbox iframe 渲染。
 * sandbox 允许 allow-scripts 让 IntersectionObserver 等动画脚本正常执行。
 */
export function CustomPortalTemplate({
  html,
  userName,
}: {
  html: string | null;
  userName: string;
}) {
  if (!html) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-paper">
        <div className="text-center">
          <p className="font-serif text-[24px] text-ink">{userName}</p>
          <p className="mt-2 text-[13px] text-mute">门户内容尚未生成</p>
        </div>
      </div>
    );
  }

  return (
    <iframe
      srcDoc={html}
      title={`${userName} 的个人门户`}
      className="min-h-screen w-full border-0"
      sandbox="allow-scripts allow-same-origin allow-popups"
    />
  );
}
