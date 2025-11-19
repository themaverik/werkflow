/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  output: 'standalone', // For Docker deployment
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  },
  webpack: (config, { isServer }) => {
    // Fix for bpmn-js in Next.js
    config.module.rules.push({
      test: /\.bpmn$/,
      use: 'raw-loader',
    })

    // Exclude formiojs and bpmn-js from server-side bundling
    if (isServer) {
      config.externals = [...(config.externals || []), 'formiojs', 'bpmn-js', '@formio/react']
    }

    return config
  },
}

export default nextConfig
