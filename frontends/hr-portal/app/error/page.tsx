import { AlertCircle } from 'lucide-react';
import Link from 'next/link';

export default function ErrorPage({
  searchParams,
}: {
  searchParams: { error?: string };
}) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-red-50 to-pink-100">
      <div className="max-w-md w-full space-y-8 p-8 bg-white rounded-xl shadow-2xl">
        <div className="text-center">
          <div className="flex justify-center mb-4">
            <div className="bg-red-600 p-3 rounded-lg">
              <AlertCircle className="h-10 w-10 text-white" />
            </div>
          </div>
          <h2 className="text-3xl font-bold text-gray-900">Authentication Error</h2>
          <p className="mt-2 text-sm text-gray-600">
            {searchParams.error === 'AccessDenied'
              ? 'You do not have permission to access this portal.'
              : 'An error occurred during authentication.'}
          </p>
        </div>

        <Link
          href="/login"
          className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
        >
          Return to Login
        </Link>
      </div>
    </div>
  );
}
