#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""image-crawler HTTP API 服务。

以独立 HTTP 服务方式对外提供图片搜索能力，供 Java 后端（或任意语言）调用。
Java 端只需通过 REST 请求即可获取图片 URL 列表，无需再以子进程方式调用命令行。

启动：
    python server.py [--host 0.0.0.0] [--port 8120]

接口：
    GET /health                健康检查，返回 {"status": "ok", "service": "image-crawler"}
    GET /api/search?keyword=关键词&count=4
                                按关键词搜索图片 URL，返回 {"keyword": ..., "count": n, "urls": [...]}
"""
import argparse

from flask import Flask, jsonify, request

from image_crawler.crawler import search_images

app = Flask(__name__)


@app.route("/health")
def health():
    return jsonify({"status": "ok", "service": "image-crawler"})


@app.route("/api/search")
def api_search():
    keyword = (request.args.get("keyword") or "").strip()
    try:
        count = int(request.args.get("count", 4))
    except (TypeError, ValueError):
        count = 4
    if count < 1 or count > 30:
        count = 4

    if not keyword:
        return jsonify({"error": "keyword is required"}), 400

    urls = search_images(keyword, count)
    return jsonify({"keyword": keyword, "count": len(urls), "urls": urls})


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="image-crawler HTTP API 服务")
    parser.add_argument("--host", default="0.0.0.0", help="监听地址（默认 0.0.0.0）")
    parser.add_argument("--port", type=int, default=8120, help="监听端口（默认 8120）")
    args = parser.parse_args()
    app.run(host=args.host, port=args.port, debug=False)
