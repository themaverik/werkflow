import { signIn } from '@/auth';
import { Building2, Users } from 'lucide-react';

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="max-w-md w-full space-y-8 p-8 bg-white rounded-xl shadow-2xl">
        <div className="text-center">
          <div className="flex justify-center mb-4">
            <div className="bg-blue-600 p-3 rounded-lg">
              <Users className="h-10 w-10 text-white" />
            </div>
          </div>
          <h2 className="text-3xl font-bold text-gray-900">Werkflow HR Portal</h2>
          <p className="mt-2 text-sm text-gray-600">
            Employee self-service and HR workflows
          </p>
        </div>

        <form
          action={async () => {
            'use server';
            await signIn('keycloak', { redirectTo: '/dashboard' });
          }}
        >
          <button
            type="submit"
            className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
          >
            <span className="absolute left-0 inset-y-0 flex items-center pl-3">
              <Building2 className="h-5 w-5 text-blue-500 group-hover:text-blue-400" />
            </span>
            Sign in with Single Sign-On
          </button>
        </form>

        <div className="text-xs text-center text-gray-500">
          Powered by Keycloak Authentication
        </div>
      </div>
    </div>
  );
}
