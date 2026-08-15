/** @type {import('next').NextConfig} */
const BACKEND = process.env.BACKEND_URL || "http://127.0.0.1:8080";

const nextConfig = {
  reactStrictMode: true,
  async rewrites() {
    return [
      // 同源代理：让前端携带 sa-token Cookie 访问后端 API 与上传文件
      { source: "/api/:path*", destination: `${BACKEND}/api/:path*` },
      { source: "/uploads/:path*", destination: `${BACKEND}/uploads/:path*` },
      // demo 静态资源（头像/封面）：用 /demo/ 前缀避免段内参数 rewrite 的路径篡改
      { source: "/demo/:path*", destination: `${BACKEND}/demo/:path*` },
    ];
  },
};

export default nextConfig;
