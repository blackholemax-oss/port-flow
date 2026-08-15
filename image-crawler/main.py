#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""百度图片搜索 API 命令行入口。"""
import argparse
import sys

from image_crawler.crawler import search_images, download_images


def main():
    parser = argparse.ArgumentParser(description="百度图片搜索 API")
    sub = parser.add_subparsers(dest="command")

    # 搜索（返回 URL 列表）
    search_p = sub.add_parser("search", help="搜索图片 URL")
    search_p.add_argument("keyword", help="搜索关键词")
    search_p.add_argument("-n", "--count", type=int, default=4, help="数量（默认 4）")

    # 下载（搜索并保存到本地）
    dl_p = sub.add_parser("download", help="搜索并下载图片")
    dl_p.add_argument("keyword", help="搜索关键词")
    dl_p.add_argument("-n", "--count", type=int, default=4, help="数量")
    dl_p.add_argument("-o", "--output", default="Pictures", help="保存目录")

    args = parser.parse_args()

    if args.command == "search":
        urls = search_images(args.keyword, args.count)
        for u in urls:
            print(u)
        if not urls:
            print("未找到图片", file=sys.stderr)
            sys.exit(1)

    elif args.command == "download":
        files = download_images(args.keyword, args.count, args.output)
        for f in files:
            print(f)
        if not files:
            print("下载失败", file=sys.stderr)
            sys.exit(1)

    else:
        parser.print_help()


if __name__ == "__main__":
    main()
