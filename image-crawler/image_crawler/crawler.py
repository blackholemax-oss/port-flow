"""
百度图片搜索 API：根据关键词搜索图片，返回图片 URL 列表。
免认证，直接请求百度图片 acjson 结构化接口。
"""
import json
import re
import time
from urllib.parse import unquote, quote

import requests

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    "Accept": "application/json,text/plain,*/*",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    "Connection": "keep-alive",
}

# 过滤：广告条目与小尺寸图（logo/头像/商品缩略图）
MIN_WIDTH = 500
MIN_HEIGHT = 300


def search_images(keyword: str, count: int = 4) -> list[str]:
    """
    搜索百度图片，返回图片 URL 列表（acjson 接口，过滤广告和小图）。

    :param keyword: 搜索关键词（支持中文）
    :param count: 需要的图片数量
    :return: 图片 URL 列表
    """
    if not keyword or count <= 0:
        return []

    results: list[str] = []
    page = 0

    while len(results) < count and page < 3:
        pn = page * 30
        url = (
            f"https://image.baidu.com/search/acjson?tn=resultjson_com&ipn=rj"
            f"&ct=201326592&is=&fp=result&queryWord={quote(keyword)}&cl=2&lm=-1"
            f"&ie=utf-8&oe=utf-8&word={quote(keyword)}&pn={pn}&rn=30&face=0&istype=2"
        )

        try:
            resp = requests.get(url, headers=HEADERS, timeout=10)
            if resp.status_code != 200:
                break

            text = resp.text.strip()
            start = text.find("{")
            end = text.rfind("}")
            if start < 0 or end <= start:
                break
            data = json.loads(text[start:end + 1]).get("data", [])
            if not isinstance(data, list):
                break

            for item in data:
                if len(results) >= count:
                    break
                # 过滤广告
                if str(item.get("adType", "0")) != "0":
                    continue
                # 过滤小图
                w = item.get("width", 0) or 0
                h = item.get("height", 0) or 0
                if w > 0 and h > 0 and (w < MIN_WIDTH or h < MIN_HEIGHT):
                    continue

                # 优先 objURL（原图），依次降级 middleURL/hoverURL/thumbURL
                candidates = [item.get("objURL"), item.get("middleURL"),
                              item.get("hoverURL"), item.get("thumbURL")]
                for u in candidates:
                    if not u:
                        continue
                    u = unquote(u)
                    if u.startswith("http") and u not in results:
                        results.append(u)
                        break

        except Exception:
            break

        page += 1
        time.sleep(0.5)

    return results[:count]


def download_images(keyword: str, count: int, save_dir: str) -> list[str]:
    """
    搜索并下载图片到指定目录。

    :param keyword: 搜索关键词
    :param count: 图片数量
    :param save_dir: 保存目录
    :return: 已保存的文件路径列表
    """
    import os

    os.makedirs(save_dir, exist_ok=True)
    urls = search_images(keyword, count)
    saved: list[str] = []

    for i, img_url in enumerate(urls):
        try:
            headers = {**HEADERS, "Referer": "https://image.baidu.com/"}
            resp = requests.get(img_url, headers=headers, timeout=15)
            resp.raise_for_status()

            ext = ".jpg"
            if "." in img_url:
                ext = "." + img_url.split(".")[-1].split("?")[0]
                if len(ext) > 5:
                    ext = ".jpg"

            filename = f"{keyword}_{i + 1}{ext}"
            filepath = os.path.join(save_dir, filename)
            with open(filepath, "wb") as f:
                f.write(resp.content)
            saved.append(filepath)
        except Exception:
            continue

    return saved
