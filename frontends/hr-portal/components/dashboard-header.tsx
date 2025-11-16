'use client';

import { Bell, LogOut, Settings, User } from 'lucide-react';
import { signOut } from 'next-auth/react';

export function DashboardHeader({ user }: { user: any }) {
  return (
    <header className="bg-white shadow-sm border-b border-gray-200">
      <div className="flex items-center justify-between px-6 py-4">
        <div className="flex items-center">
          <h1 className="text-2xl font-bold text-blue-600">Werkflow HR</h1>
        </div>

        <div className="flex items-center space-x-4">
          {/* Notifications */}
          <button className="relative p-2 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100">
            <Bell className="h-5 w-5" />
            <span className="absolute top-0 right-0 block h-2 w-2 rounded-full bg-red-500"></span>
          </button>

          {/* User Menu */}
          <div className="relative flex items-center space-x-3">
            <div className="flex items-center space-x-2 px-3 py-2 rounded-lg hover:bg-gray-100">
              <div className="h-8 w-8 rounded-full bg-blue-600 flex items-center justify-center text-white font-medium">
                {user?.name?.charAt(0) || 'U'}
              </div>
              <div className="hidden md:block text-sm">
                <div className="font-medium text-gray-900">{user?.name || 'User'}</div>
                <div className="text-gray-500">{user?.email || ''}</div>
              </div>
            </div>

            <button
              onClick={() => signOut({ redirectTo: '/login' })}
              className="p-2 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100"
              title="Sign out"
            >
              <LogOut className="h-5 w-5" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
