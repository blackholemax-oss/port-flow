"use client";

import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Brand } from "@/components/Brand";
import { api, ApiError } from "@/lib/api";
import type { StylePrompt, Template } from "@/lib/types";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { toast } from "sonner";

const TEMPLATES: { value: Template; label: string; desc: string }[] = [
  { value: "card", label: "极简卡片", desc: "居中头像 + 卡片网格" },
  { value: "gallery", label: "画廊大图", desc: "暗色全屏画廊" },
  { value: "magazine", label: "杂志风", desc: "图文交错编排" },
  { value: "custom", label: "AI 定制门户", desc: "大模型生成完整 HTML" },
];

interface ProjectRow {
  key: string;
  title: string;
  description: string;
  coverFile: File | null;
  existingCover: string | null;
}

let rowSeq = 0;
const newRow = (): ProjectRow => ({
  key: `r${++rowSeq}`,
  title: "",
  description: "",
  coverFile: null,
  existingCover: null,
});

export default function EditorPage() {
  return (
    <Suspense fallback={null}>
      <EditorInner />
    </Suspense>
  );
}

function EditorInner() {
  const router = useRouter();
  const slugParam = useSearchParams().get("slug");
  const isEdit = !!slugParam;

  const [slug, setSlug] = useState("");
  const [userName, setUserName] = useState("");
  const [slogan, setSlogan] = useState("");
  const [bio, setBio] = useState("");
  const [skills, setSkills] = useState("");
  const [themeColor, setThemeColor] = useState("#4F46E5");
  const [template, setTemplate] = useState<Template>("card");
  const [seoTitle, setSeoTitle] = useState("");
  const [seoDescription, setSeoDescription] = useState("");
  const [isPublished, setIsPublished] = useState(true);

  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [existingAvatar, setExistingAvatar] = useState<string | null>(null);

  const [projects, setProjects] = useState<ProjectRow[]>([newRow()]);

  const [notes, setNotes] = useState("");
  const [occupation, setOccupation] = useState("程序员");
  const [aiHint, setAiHint] = useState("");
  const [aiBusy, setAiBusy] = useState<string | null>(null);
  const [polishingKey, setPolishingKey] = useState<string | null>(null);
  const [colorReason, setColorReason] = useState("");

  // AI 门户生成
  const [styles, setStyles] = useState<StylePrompt[]>([]);
  const [selectedStyle, setSelectedStyle] = useState<string>("");
  const [customPrompt, setCustomPrompt] = useState("");
  const [generatedHtml, setGeneratedHtml] = useState("");
  const [portalBusy, setPortalBusy] = useState(false);
  // AI 对话修改：用户通过自然语言指令让 AI 在现有 HTML 基础上调整
  const [adjustPrompt, setAdjustPrompt] = useState("");
  const [adjustBusy, setAdjustBusy] = useState(false);

  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [loadError, setLoadError] = useState("");

  const load = useCallback(async () => {
    if (!slugParam) {
      setLoading(false);
      return;
    }
    try {
      const d = await api.getForEdit(slugParam);
      setSlug(d.slug);
      setUserName(d.userName);
      setSlogan(d.slogan ?? "");
      setBio(d.bio ?? "");
      setSkills(d.skills ?? "");
      setThemeColor(d.themeColor || "#4F46E5");
      setTemplate(d.template || "card");
      setSeoTitle(d.seoTitle ?? "");
      setSeoDescription(d.seoDescription ?? "");
      setIsPublished(d.isPublished);
      setExistingAvatar(d.avatarPath ?? null);
      setGeneratedHtml(d.generatedHtml ?? "");
      setCustomPrompt(d.customPrompt ?? "");
      setProjects(
        d.projects.length
          ? d.projects.map((p) => ({
              key: `r${++rowSeq}`,
              title: p.title,
              description: p.description ?? "",
              coverFile: null,
              existingCover: p.coverPath ?? null,
            }))
          : [newRow()],
      );
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : "加载失败");
    } finally {
      setLoading(false);
    }
  }, [slugParam]);

  useEffect(() => {
    void load();
  }, [load]);

  // 加载设计风格列表
  useEffect(() => {
    api
      .aiListStyles()
      .then(setStyles)
      .catch(() => {});
  }, []);

  function showAi(msg: string) {
    setAiHint(msg);
    if (msg.startsWith("✅")) {
      toast.success(msg);
    } else if (msg.includes("失败") || msg.includes("错误") || msg.includes("异常")) {
      toast.error(msg);
    } else {
      toast.info(msg);
    }
  }

  async function runAi<T>(key: string, fn: () => Promise<T>): Promise<T | null> {
    setAiBusy(key);
    showAi("AI 生成中…");
    try {
      const r = await fn();
      return r;
    } catch (err) {
      showAi(err instanceof ApiError ? err.message : "AI 服务异常，请稍后重试");
      return null;
    } finally {
      setAiBusy(null);
    }
  }

  async function generateBio() {
    const kw = notes.trim();
    if (!kw) {
      showAi("请先在灵感便签里写几个关键词");
      return;
    }
    const r = await runAi("bio", () => api.aiBio(kw));
    if (!r) return;
    setSlogan(r.slogan ?? "");
    setBio(r.story ?? "");
    setSkills(Array.isArray(r.skills) ? r.skills.join(", ") : "");
    showAi("✅ 个人简介已生成，可手动微调后再保存");
  }

  async function recommendColor() {
    const r = await runAi("color", () => api.aiColor(occupation.trim() || "程序员"));
    if (!r) return;
    if (r.color) setThemeColor(r.color);
    setColorReason(r.reason ?? "");
    showAi(`✅ 推荐配色 ${r.color ?? ""}`);
  }

  async function generateSeo() {
    const r = await runAi("seo", () => api.aiSeo(userName, bio));
    if (!r) return;
    if (r.title) setSeoTitle(r.title);
    if (r.description) setSeoDescription(r.description);
    showAi("✅ SEO 已生成");
  }

  async function polishRow(row: ProjectRow) {
    const text = row.description.trim();
    if (!text) return;
    setPolishingKey(row.key);
    try {
      const r = await api.aiPolish(text, row.title.trim());
      setProjects((rows) =>
        rows.map((p) => (p.key === row.key ? { ...p, description: r.text ?? p.description } : p)),
      );
      showAi("✅ 描述已润色");
    } catch (err) {
      showAi(err instanceof ApiError ? err.message : "润色失败");
    } finally {
      setPolishingKey(null);
    }
  }

  async function generatePortalHtml() {
    setPortalBusy(true);
    showAi("AI 正在生成门户网页…");
    try {
      // 如果选中了风格，将其描述追加到自定义提示词
      let prompt = customPrompt.trim();
      if (selectedStyle) {
        const style = styles.find((s) => s.code === selectedStyle);
        if (style) {
          prompt =
            (prompt ? prompt + "\n\n" : "") +
            `参考设计风格：${style.name}。${style.promptTemplate}`;
        }
      }
      const r = await api.aiGeneratePortalHtml({
        slug: slug.trim() || "draft",
        userName,
        bio,
        skills,
        slogan,
        themeColor,
        userPrompt: prompt || undefined,
        notes: notes.trim() || undefined,
        projects: projects
          .filter((p) => p.title.trim())
          .map((p) => ({ title: p.title, description: p.description })),
      });
      setGeneratedHtml(r.html);
      // 自动切换到 custom 模板
      if (template !== "custom") setTemplate("custom");
      // 拼接图片搜索关键词提示（便于用户判断搜索是否精准）
      const keywordHint = r.imageKeywords ? `\n📷 ${r.imageKeywords}` : "";
      showAi(`✅ ${r.message}，可在下方预览和编辑${keywordHint}`);
    } catch (err) {
      showAi(err instanceof ApiError ? err.message : "门户生成失败，请稍后重试");
    } finally {
      setPortalBusy(false);
    }
  }

  // AI 对话修改：在当前已生成的 HTML 基础上，按用户指令让 AI 调整
  async function adjustPortalHtml() {
    if (!generatedHtml) {
      showAi("请先生成网页，再让 AI 调整");
      return;
    }
    if (!adjustPrompt.trim()) {
      showAi("请输入想要调整的内容");
      return;
    }
    setAdjustBusy(true);
    showAi("AI 正在按你的要求调整网页…");
    try {
      const r = await api.aiAdjustPortalHtml({
        slug: slug.trim() || "draft",
        currentHtml: generatedHtml,
        instruction: adjustPrompt.trim(),
      });
      setGeneratedHtml(r.html);
      setAdjustPrompt("");
      // 自动保存 HTML 到数据库，确保调整不丢失
      if (isEdit && slug.trim()) {
        await api.updatePortalHtml(slug.trim(), { html: r.html });
        showAi(`✅ ${r.message}，已自动保存`);
      } else {
        showAi(`✅ ${r.message}，请点击下方保存发布`);
      }
    } catch (err) {
      showAi(err instanceof ApiError ? err.message : "调整失败，请稍后重试");
    } finally {
      setAdjustBusy(false);
    }
  }

  // 同步表单到网页：自动把当前表单信息转成 AI 调整指令，让 AI 在现有 HTML 上更新内容
  async function syncFormToPortal() {
    if (!generatedHtml) {
      showAi("请先生成网页，再同步表单");
      return;
    }

    // 构造调整指令：把当前表单信息整理成结构化指令
    const parts: string[] = [];
    parts.push("请在现有网页的基础上执行以下更新（保持设计风格和布局不变，只更新内容和结构）：\n");

    if (userName.trim()) parts.push(`1. 将网页中显示的姓名更新为：${userName.trim()}`);
    if (slogan.trim()) parts.push(`2. 将网页中显示的口号更新为：${slogan.trim()}`);
    if (bio.trim()) parts.push(`3. 将网页中显示的个人简介更新为：${bio.trim()}`);
    if (skills.trim()) {
      const skillList = skills.split(",").map(s => s.trim()).filter(Boolean);
      parts.push(`4. 将网页中的技能标签更新为以下 ${skillList.length} 个：${skillList.join("、")}`);
    }

    const validProjects = projects.filter((p) => p.title.trim() || p.description.trim());
    if (validProjects.length > 0) {
      parts.push(`\n5. 【重要】将网页中的作品项目区块替换为以下 ${validProjects.length} 个项目（删除旧的项目，用以下内容替换）：`);
      validProjects.forEach((p, i) => {
        parts.push(`   项目${i + 1}：`);
        parts.push(`     - 标题：${p.title.trim() || "无标题"}`);
        parts.push(`     - 描述：${p.description.trim() || "无描述"}`);
      });
      parts.push("   每个项目使用与网页中现有项目相同的 HTML 结构和样式。如果网页中原本没有项目，请按照网页的整体风格创建项目卡片。");
    } else {
      parts.push("\n5. 如果没有作品项目，作品区块显示占位提示即可，不要编造项目。");
    }

    parts.push("\n请严格按照以上要求更新网页，确保所有修改都正确反映在输出的 HTML 中。");

    const instruction = parts.join("\n");

    setAdjustBusy(true);
    showAi("AI 正在将表单内容同步到网页…");
    try {
      const r = await api.aiAdjustPortalHtml({
        slug: slug.trim() || "draft",
        currentHtml: generatedHtml,
        instruction,
      });
      setGeneratedHtml(r.html);
      // 自动保存 HTML + 表单数据（含作品项目）到数据库，确保刷新后不丢失
      if (isEdit && slug.trim()) {
        await api.updatePortalHtml(slug.trim(), {
          html: r.html,
          userName: userName.trim(),
          slogan: slogan.trim(),
          bio: bio.trim(),
          skills: skills.trim(),
          projects: validProjects.map(p => ({ title: p.title.trim(), description: p.description.trim() })),
        });
        showAi(`✅ ${r.message}，已自动保存`);
      } else {
        showAi(`✅ ${r.message}，请点击下方保存发布`);
      }
    } catch (err) {
      showAi(err instanceof ApiError ? err.message : "同步失败，请稍后重试");
    } finally {
      setAdjustBusy(false);
    }
  }

  function updateRow(key: string, patch: Partial<ProjectRow>) {
    setProjects((rows) => rows.map((p) => (p.key === key ? { ...p, ...patch } : p)));
  }
  function removeRow(key: string) {
    setProjects((rows) => (rows.length > 1 ? rows.filter((p) => p.key !== key) : rows));
  }
  function addRow() {
    setProjects((rows) => [...rows, newRow()]);
  }

  const [deleting, setDeleting] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  async function onDelete() {
    if (!slugParam) return;
    setDeleteDialogOpen(true);
  }

  async function confirmDelete() {
    if (!slugParam) return;
    setDeleting(true);
    try {
      await api.deletePortfolio(slugParam);
      router.push("/admin");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "删除失败，请稍后重试");
    } finally {
      setDeleting(false);
      setDeleteDialogOpen(false);
    }
  }

  async function onSave(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    if (!slug.trim() || !userName.trim()) {
      setError("页面地址与用户名为必填项");
      return;
    }
    setSaving(true);
    try {
      const form = new FormData();
      form.append("slug", slug.trim());
      form.append("userName", userName);
      if (slogan) form.append("slogan", slogan);
      if (bio) form.append("bio", bio);
      if (skills) form.append("skills", skills);
      if (themeColor) form.append("themeColor", themeColor);
      if (template) form.append("template", template);
      if (isPublished) form.append("isPublished", "true");
      if (seoTitle) form.append("seoTitle", seoTitle);
      if (seoDescription) form.append("seoDescription", seoDescription);
      // AI 定制门户：custom 模板时保存生成的 HTML 和自定义提示词
      if (template === "custom" && generatedHtml) {
        form.append("generatedHtml", generatedHtml);
      }
      if (customPrompt) form.append("customPrompt", customPrompt);
      if (avatarFile) form.append("avatar", avatarFile);

      projects.forEach((p) => {
        form.append("title", p.title);
        form.append("description", p.description);
        if (p.coverFile) form.append("cover", p.coverFile, p.coverFile.name);
        else form.append("cover", new Blob([]), "");
      });

      const { slug: savedSlug } = await api.savePortfolio(form);
      router.push(`/p/${encodeURIComponent(savedSlug)}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "保存失败，请稍后重试");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="relative z-10 flex min-h-screen items-center justify-center">
        <p className="font-mono text-[12px] text-mute">LOADING…</p>
      </div>
    );
  }
  if (loadError) {
    return (
      <div className="relative z-10 flex min-h-screen flex-col items-center justify-center gap-4">
        <p className="font-serif text-[24px] text-ink">{loadError}</p>
        <Link href="/admin" className="btn btn-ghost">
          ← 返回列表
        </Link>
      </div>
    );
  }

  return (
    <div className="relative z-10 min-h-screen pb-24">
      <form onSubmit={onSave}>
        {/* 顶栏 */}
        <header className="sticky top-0 z-20 border-b border-line bg-paper/85 backdrop-blur">
          <div className="container-page flex h-16 items-center justify-between">
            <div className="flex items-center gap-5">
              <Brand href="/admin" />
              <span className="hidden h-4 w-px bg-line sm:block" />
              <Link href="/admin" className="hidden text-[13px] text-mute hover:text-ink sm:block">
                ← 返回列表
              </Link>
            </div>
            <div className="flex items-center gap-3">
              <span className="hidden font-mono text-[11px] uppercase tracking-wider3 text-mute md:block">
                {isEdit ? "EDIT · 编辑" : "NEW · 新建"}
              </span>
              <button type="submit" disabled={saving} className="btn btn-ink">
                {saving ? "保存中…" : "保存并发布"}
              </button>
            </div>
          </div>
        </header>

        <main className="container-page py-10 md:py-14">
          {/* 标题 */}
          <div className="animate-rise mb-9">
            <p className="eyebrow mb-3">EDITOR · 作品集编辑器</p>
            <h1 className="font-serif text-[34px] font-semibold leading-tight tracking-tight text-ink md:text-[44px]">
              {isEdit ? "编辑作品集" : "新建作品集"}
            </h1>
          </div>

          <div className="grid grid-cols-1 gap-8 lg:grid-cols-[1fr_340px]">
            {/* 主表单 */}
            <div className="space-y-8">
              <Section n="01" title="基础信息">
                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                  <div>
                    <label className="label" htmlFor="slug">
                      页面地址 <span className="hint">→ /p/你的地址</span>
                    </label>
                    <input
                      id="slug"
                      className="input font-mono"
                      maxLength={64}
                      value={slug}
                      onChange={(e) => setSlug(e.target.value)}
                      placeholder="john-doe"
                      required
                    />
                  </div>
                  <div>
                    <label className="label" htmlFor="userName">
                      用户名
                    </label>
                    <input
                      id="userName"
                      className="input"
                      maxLength={64}
                      value={userName}
                      onChange={(e) => setUserName(e.target.value)}
                      placeholder="张三"
                      required
                    />
                  </div>
                </div>

                <div className="mt-5">
                  <label className="label" htmlFor="slogan">
                    口号 Slogan
                  </label>
                  <input
                    id="slogan"
                    className="input"
                    maxLength={128}
                    value={slogan}
                    onChange={(e) => setSlogan(e.target.value)}
                    placeholder="让技术，成为我的名片"
                  />
                </div>

                <div className="mt-5">
                  <label className="label" htmlFor="bio">
                    个人故事 Bio
                  </label>
                  <textarea
                    id="bio"
                    className="textarea"
                    rows={4}
                    maxLength={500}
                    value={bio}
                    onChange={(e) => setBio(e.target.value)}
                    placeholder="介绍一下你自己…"
                  />
                </div>

                <div className="mt-5">
                  <label className="label" htmlFor="skills">
                    技能标签 <span className="hint">逗号分隔</span>
                  </label>
                  <input
                    id="skills"
                    className="input"
                    maxLength={200}
                    value={skills}
                    onChange={(e) => setSkills(e.target.value)}
                    placeholder="Java, Spring Boot, AI"
                  />
                </div>
              </Section>

              <Section n="02" title="视觉与模板">
                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                  <div>
                    <label className="label" htmlFor="themeColor">
                      主题色
                    </label>
                    <div className="flex items-center gap-3">
                      <Popover>
                        <PopoverTrigger
                          render={
                            <button
                              type="button"
                              className="h-10 w-10 rounded-lg border border-line"
                              style={{ backgroundColor: themeColor }}
                            />
                          }
                        />
                        <PopoverContent className="w-64 p-3">
                          <div className="grid grid-cols-6 gap-2">
                            {["#4F46E5", "#16A34A", "#C2521F", "#9333EA", "#2563EB", "#DB2777",
                              "#0891B2", "#CA8A04", "#DC2626", "#7C3AED", "#059669", "#1A1714"].map((c) => (
                              <button
                                key={c}
                                type="button"
                                className={`h-8 w-8 rounded-lg border-2 transition ${
                                  themeColor === c ? "border-ink" : "border-transparent"
                                }`}
                                style={{ backgroundColor: c }}
                                onClick={() => setThemeColor(c)}
                              />
                            ))}
                          </div>
                          <div className="mt-3 flex items-center gap-2">
                            <input
                              type="color"
                              value={themeColor}
                              onChange={(e) => {
                                setThemeColor(e.target.value);
                                setColorReason("");
                              }}
                              className="h-8 w-8 cursor-pointer rounded border border-line"
                            />
                            <input
                              type="text"
                              value={themeColor}
                              onChange={(e) => setThemeColor(e.target.value)}
                              className="input flex-1"
                              placeholder="#RRGGBB"
                            />
                          </div>
                        </PopoverContent>
                      </Popover>
                      <span className="text-[13px] text-mute">{themeColor}</span>
                    </div>
                    {colorReason && (
                      <p className="mt-2 text-[12px] leading-relaxed text-mute">
                        <span className="text-ember">AI ·</span> {colorReason}
                      </p>
                    )}
                  </div>
                  <div>
                    <label className="label" htmlFor="template">
                      展示模板
                    </label>
                    <select
                      id="template"
                      className="select"
                      value={template}
                      onChange={(e) => setTemplate(e.target.value as Template)}
                    >
                      {TEMPLATES.map((t) => (
                        <option key={t.value} value={t.value}>
                          {t.label} — {t.desc}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="mt-5">
                  <label className="label" htmlFor="avatar">
                    头像 <span className="hint">不传则保留原图</span>
                  </label>
                  <div className="flex items-center gap-4">
                    <AvatarPreview
                      file={avatarFile}
                      existing={existingAvatar}
                      name={userName}
                      color={themeColor}
                    />
                    <input
                      id="avatar"
                      type="file"
                      accept="image/*"
                      onChange={(e) => setAvatarFile(e.target.files?.[0] ?? null)}
                      className="block w-full text-[13px] text-mute file:mr-3 file:rounded-full file:border-0 file:bg-ink file:px-3.5 file:py-1.5 file:text-[12px] file:font-medium file:text-paper-soft hover:file:bg-ink-soft"
                    />
                  </div>
                </div>
              </Section>

              <Section
                n="03"
                title="作品项目"
                action={
                  <button type="button" onClick={addRow} className="btn btn-ghost text-[13px]">
                    + 添加作品
                  </button>
                }
              >
                <div className="space-y-4">
                  {projects.map((p, i) => (
                    <ProjectCard
                      key={p.key}
                      index={i + 1}
                      row={p}
                      onChange={(patch) => updateRow(p.key, patch)}
                      onRemove={() => removeRow(p.key)}
                      onPolish={() => polishRow(p)}
                      polishing={polishingKey === p.key}
                    />
                  ))}
                </div>
              </Section>

              <Section n="04" title="SEO 与发布">
                <div>
                  <label className="label" htmlFor="seoTitle">
                    SEO 标题
                  </label>
                  <input
                    id="seoTitle"
                    className="input"
                    value={seoTitle}
                    onChange={(e) => setSeoTitle(e.target.value)}
                    placeholder="张三 - 个人作品集 | AI 策展师"
                  />
                </div>
                <div className="mt-5">
                  <label className="label" htmlFor="seoDescription">
                    SEO 描述
                  </label>
                  <textarea
                    id="seoDescription"
                    className="textarea"
                    rows={2}
                    value={seoDescription}
                    onChange={(e) => setSeoDescription(e.target.value)}
                    placeholder="搜索引擎摘要，150 字以内"
                  />
                </div>
                <label className="mt-5 flex w-fit cursor-pointer items-center gap-3 rounded-xl border border-line bg-paper-soft px-4 py-3">
                  <input
                    type="checkbox"
                    checked={isPublished}
                    onChange={(e) => setIsPublished(e.target.checked)}
                    className="h-4 w-4 accent-ember"
                  />
                  <span className="text-[13px] text-ink-soft">
                    立即发布
                    <span className="ml-1 text-mute">（不勾选则访问页面返回 404）</span>
                  </span>
                </label>
              </Section>

              {/* AI 门户生成 */}
              <Section
                n="05"
                title="AI 一键生成我的门户网页"
                action={
                  !isEdit || !generatedHtml ? (
                    <button
                      type="button"
                      onClick={generatePortalHtml}
                      disabled={portalBusy || !userName.trim()}
                      className="btn btn-ember text-[13px]"
                    >
                      {portalBusy ? "正在生成中…" : "✨ 生成我的网页"}
                    </button>
                  ) : undefined
                }
              >
                {isEdit && generatedHtml ? (
                  <p className="rounded-lg bg-ember/[0.06] px-4 py-3 text-[12px] text-ember-deep">
                    网页已生成，请在下方预览区通过「同步表单到网页」或「让 AI 调整」来修改内容。如需重新生成，请删除当前网页后操作。
                  </p>
                ) : (
                  <>
                    <p className="mb-4 text-[12px] leading-relaxed text-mute">
                      选择「AI 定制门户」模板后，AI 会根据你的个人信息和喜欢的风格，
                      自动生成一个完整的个人网页。生成后可以随时修改再发布。
                    </p>

                    {/* 风格选择 */}
                    <div className="mb-5">
                      <label className="label">选一个喜欢的风格</label>
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => setSelectedStyle("")}
                          className={`rounded-full px-3 py-1.5 text-[12px] transition-colors ${
                            !selectedStyle
                              ? "bg-ink text-paper-soft"
                              : "border border-line bg-paper text-mute hover:text-ink"
                          }`}
                        >
                          让 AI 自动选
                        </button>
                        {styles.map((s) => (
                          <button
                            key={s.code}
                            type="button"
                            onClick={() => setSelectedStyle(s.code)}
                            title={s.useCase}
                            className={`rounded-full px-3 py-1.5 text-[12px] transition-colors ${
                              selectedStyle === s.code
                                ? "bg-ink text-paper-soft"
                                : "border border-line bg-paper text-mute hover:text-ink"
                            }`}
                          >
                            {s.name}
                          </button>
                        ))}
                      </div>
                      {selectedStyle && (
                        <p className="mt-2 text-[12px] leading-relaxed text-mute">
                          {styles.find((s) => s.code === selectedStyle)?.description}
                        </p>
                      )}
                    </div>

                    {/* 自定义提示词 */}
                    <div className="mb-5">
                      <label className="label" htmlFor="customPrompt">
                        告诉 AI 你想要什么样的网页{" "}
                        <span className="hint">可选，留空则按风格自动生成</span>
                      </label>
                      <textarea
                        id="customPrompt"
                        className="textarea"
                        rows={3}
                        maxLength={500}
                        value={customPrompt}
                        onChange={(e) => setCustomPrompt(e.target.value)}
                        placeholder="例如：我想要暗黑风格，紫色调，顶部有固定导航栏，卡片有圆角和阴影…"
                      />
                    </div>

                    {/* 生成按钮 */}
                    <div className="mb-5 flex flex-wrap gap-3">
                      <button
                        type="button"
                        onClick={generatePortalHtml}
                        disabled={portalBusy || !userName.trim()}
                        className="btn btn-ember"
                      >
                        {portalBusy ? "正在生成中…" : "✨ 生成我的网页"}
                      </button>
                    </div>
                  </>
                )}

                {/* 生成中加载动画 */}
                {portalBusy && (
                  <div className="mb-5 flex flex-col items-center justify-center rounded-xl border border-ember/20 bg-ember/[0.04] py-10">
                    <div className="relative h-12 w-12">
                      <div className="absolute inset-0 rounded-full border-4 border-ember/20" />
                      <div className="absolute inset-0 animate-spin rounded-full border-4 border-transparent border-t-ember" />
                    </div>
                    <p className="mt-4 text-[13px] font-medium text-ink-soft">
                      AI 正在为你设计网页…
                    </p>
                    <p className="mt-1 text-[11px] text-mute">通常需要 2-3 分钟，请耐心等待</p>
                  </div>
                )}

                {/* HTML 预览（用户不可直接编辑源码，只能通过 AI 对话调整） */}
                {generatedHtml && (
                  <div>
                    <label className="label">预览效果</label>
                    <div className="overflow-hidden rounded-xl border border-line bg-white">
                      <iframe
                        srcDoc={generatedHtml}
                        title="门户预览"
                        className="h-[400px] w-full"
                        sandbox="allow-scripts allow-same-origin"
                      />
                    </div>

                    {/* AI 对话修改区：用户输入自然语言指令，AI 在现有 HTML 基础上调整 */}
                    <div className="mt-4 rounded-xl border border-line bg-paper-soft p-4">
                      {/* 一键同步：把左侧表单的变更同步到已生成的网页中 */}
                      <div className="mb-3 flex items-center justify-between rounded-lg bg-ember/[0.06] px-3 py-2">
                        <p className="text-[12px] text-ink-soft">
                          修改了左侧表单？一键同步到网页。
                        </p>
                        <button
                          type="button"
                          onClick={syncFormToPortal}
                          disabled={adjustBusy}
                          className="btn btn-ember text-[12px]"
                        >
                          {adjustBusy ? "同步中…" : "📋 同步表单到网页"}
                        </button>
                      </div>

                      <label className="label">想让 AI 调整哪里？</label>
                      <textarea
                        className="textarea"
                        rows={3}
                        maxLength={500}
                        value={adjustPrompt}
                        onChange={(e) => setAdjustPrompt(e.target.value)}
                        placeholder="例如：把标题改大一些、换成暖色调、增加一个联系方式区块…"
                      />
                      <div className="mt-2 flex items-center justify-between">
                        <p className="text-[11px] text-mute">
                          修改网页只能通过与 AI 对话完成，不能直接编辑代码。
                        </p>
                        <button
                          type="button"
                          onClick={adjustPortalHtml}
                          disabled={adjustBusy || !adjustPrompt.trim()}
                          className="btn btn-ghost"
                        >
                          {adjustBusy ? "AI 调整中…" : "🔄 让 AI 调整"}
                        </button>
                      </div>
                      {adjustBusy && (
                        <div className="mt-3 flex items-center gap-2 text-[12px] text-ink-soft">
                          <div className="h-3 w-3 animate-spin rounded-full border-2 border-ember/30 border-t-ember" />
                          AI 正在按你的要求调整网页…
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {template === "custom" && !generatedHtml && (
                  <p className="rounded-lg bg-ember/[0.06] px-4 py-3 text-[12px] text-ember-deep">
                    已选择「AI 定制门户」模板，请先点击上方按钮生成网页。
                  </p>
                )}
              </Section>
            </div>

            {/* AI 侧栏 */}
            <aside className="lg:sticky lg:top-24 lg:h-fit">
              <div className="overflow-hidden rounded-2xl border border-line bg-paper-soft">
                <div className="border-b border-line bg-ember/[0.04] px-5 py-4">
                  <p className="eyebrow text-ember-deep">✦ AI CURATOR</p>
                  <h3 className="mt-1.5 font-serif text-[19px] font-semibold text-ink">
                    AI 魔法生成
                  </h3>
                  <p className="mt-1 text-[12px] text-mute">零文案，一键生成你的策展内容。</p>
                </div>
                <div className="space-y-5 p-5">
                  <div>
                    <label className="label" htmlFor="notes">
                      灵感便签
                    </label>
                    <textarea
                      id="notes"
                      className="textarea"
                      rows={3}
                      value={notes}
                      onChange={(e) => setNotes(e.target.value)}
                      placeholder="随便写几个词：技术、摄影、咖啡、独立开发…"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={generateBio}
                    disabled={!!aiBusy}
                    className="btn btn-ember w-full"
                  >
                    {aiBusy === "bio" ? "生成中…" : "✨ 生成个人简介"}
                  </button>

                  <div className="hairline" />

                  <div>
                    <label className="label" htmlFor="occupation">
                      职业属性
                    </label>
                    <input
                      id="occupation"
                      className="input"
                      value={occupation}
                      onChange={(e) => setOccupation(e.target.value)}
                      placeholder="程序员 / 设计师 / 摄影师 / 产品经理"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={recommendColor}
                    disabled={!!aiBusy}
                    className="btn btn-ghost w-full"
                  >
                    {aiBusy === "color" ? "推荐中…" : "🎨 推荐配色"}
                  </button>

                  <div className="hairline" />

                  <button
                    type="button"
                    onClick={generateSeo}
                    disabled={!!aiBusy || !userName}
                    className="btn btn-ghost w-full"
                  >
                    {aiBusy === "seo" ? "生成中…" : "🔍 生成 SEO"}
                  </button>

                  {aiHint && (
                    <p className="min-h-[16px] rounded-lg bg-ember/[0.06] px-3 py-2 text-[12px] leading-relaxed text-ember-deep">
                      {aiHint}
                    </p>
                  )}
                </div>
              </div>
              <p className="mt-3 px-1 text-[11px] leading-relaxed text-mute">
                AI 生成内容会自动填入左侧表单，可随时手动微调。
              </p>
            </aside>
          </div>

          {error && (
            <div className="mt-8 rounded-xl border border-ember/30 bg-ember/5 px-4 py-3 text-[13px] text-ember-deep">
              {error}
            </div>
          )}

          <div className="mt-10 flex items-center justify-between">
            {isEdit ? (
              <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogTrigger
                  render={
                    <button
                      type="button"
                      disabled={deleting || saving}
                      className="btn border border-red-300 text-red-600 hover:bg-red-50"
                    >
                      {deleting ? "删除中…" : "🗑 删除作品集"}
                    </button>
                  }
                />
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>确认删除作品集</AlertDialogTitle>
                    <AlertDialogDescription>
                      确定要删除「{userName}」的作品集吗？此操作不可恢复，所有数据和网页将被永久删除。
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>取消</AlertDialogCancel>
                    <AlertDialogAction
                      onClick={confirmDelete}
                      className="bg-red-600 text-white hover:bg-red-700"
                    >
                      确认删除
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            ) : (
              <span />
            )}
            <button type="submit" disabled={saving} className="btn btn-ink px-8">
              {saving ? "保存中…" : "保存并发布"}
            </button>
          </div>
        </main>
      </form>
    </div>
  );
}

function Section({
  n,
  title,
  action,
  children,
}: {
  n: string;
  title: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-line bg-paper-soft p-6 md:p-7">
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="font-mono text-[12px] text-ember">{n}</span>
          <h2 className="font-serif text-[19px] font-semibold text-ink">{title}</h2>
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

function ProjectCard({
  index,
  row,
  onChange,
  onRemove,
  onPolish,
  polishing,
}: {
  index: number;
  row: ProjectRow;
  onChange: (patch: Partial<ProjectRow>) => void;
  onRemove: () => void;
  onPolish: () => void;
  polishing: boolean;
}) {
  const coverPreview = useMemo(
    () => (row.coverFile ? URL.createObjectURL(row.coverFile) : row.existingCover),
    [row.coverFile, row.existingCover],
  );
  return (
    <div className="rounded-xl border border-dashed border-line bg-paper/50 p-4">
      <div className="mb-3 flex items-center justify-between">
        <span className="font-mono text-[11px] uppercase tracking-wider2 text-mute">
          作品 {String(index).padStart(2, "0")}
        </span>
        <button
          type="button"
          onClick={onRemove}
          className="text-[12px] text-ember-deep/70 hover:text-ember-deep"
        >
          删除
        </button>
      </div>

      <input
        className="input"
        value={row.title}
        onChange={(e) => onChange({ title: e.target.value })}
        placeholder="作品标题"
      />
      <textarea
        className="textarea mt-3"
        rows={2}
        value={row.description}
        onChange={(e) => onChange({ description: e.target.value })}
        placeholder="原始描述…例如：做了个后台"
      />
      <button
        type="button"
        onClick={onPolish}
        disabled={polishing || !row.description.trim()}
        className="btn btn-ghost mt-2 text-[12px]"
      >
        {polishing ? "润色中…" : "✨ 润色描述"}
      </button>

      <div className="mt-3 flex items-center gap-3">
        {coverPreview ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={coverPreview} alt="封面预览" className="h-12 w-16 rounded-md object-cover" />
        ) : (
          <div className="flex h-12 w-16 items-center justify-center rounded-md border border-dashed border-line text-[10px] text-mute">
            封面
          </div>
        )}
        <input
          type="file"
          accept="image/*"
          onChange={(e) => onChange({ coverFile: e.target.files?.[0] ?? null })}
          className="block w-full text-[12px] text-mute file:mr-3 file:rounded-full file:border-0 file:bg-ink file:px-3 file:py-1.5 file:text-[11px] file:font-medium file:text-paper-soft hover:file:bg-ink-soft"
        />
      </div>
    </div>
  );
}

function AvatarPreview({
  file,
  existing,
  name,
  color,
}: {
  file: File | null;
  existing: string | null;
  name: string;
  color: string;
}) {
  const src = useMemo(() => (file ? URL.createObjectURL(file) : existing), [file, existing]);
  if (src) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img src={src} alt="头像预览" className="h-14 w-14 rounded-full object-cover" />
    );
  }
  return (
    <div
      style={{ backgroundColor: color }}
      className="flex h-14 w-14 items-center justify-center rounded-full font-serif text-[20px] font-semibold text-paper-soft"
    >
      {name?.[0] ?? "?"}
    </div>
  );
}
