/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  },
  webpack: (config) => {
    // Fix for bpmn-js in Next.js
    config.module.rules.push({
      test: /\.bpmn$/,
      use: 'raw-loader',
    })
    return config
  },
}

export default nextConfig
