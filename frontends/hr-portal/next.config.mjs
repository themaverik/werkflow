/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  output: 'standalone', // For Docker deployment

  // Environment variables exposed to the browser
  env: {
    NEXT_PUBLIC_APP_NAME: 'Werkflow HR Portal',
    NEXT_PUBLIC_APP_VERSION: '1.0.0',
  },

  // API rewrites for backend services
  async rewrites() {
    return [
      {
        source: '/api/hr/:path*',
        destination: (process.env.NEXT_PUBLIC_HR_API_URL || 'http://localhost:8082/api') + '/:path*',
      },
      {
        source: '/api/engine/:path*',
        destination: (process.env.NEXT_PUBLIC_ENGINE_API_URL || 'http://localhost:8081/api') + '/:path*',
      },
      {
        source: '/api/admin/:path*',
        destination: (process.env.NEXT_PUBLIC_ADMIN_API_URL || 'http://localhost:8083/api') + '/:path*',
      },
    ];
  },
};

export default nextConfig;
