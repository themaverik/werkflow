'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Calendar,
  ListTodo,
  Star,
  User,
  FileText,
  DollarSign,
  Briefcase,
} from 'lucide-react';
import { cn } from '@/lib/utils';

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'My Tasks', href: '/dashboard/tasks', icon: ListTodo },
  { name: 'Leave Requests', href: '/dashboard/leave', icon: Calendar },
  { name: 'Performance', href: '/dashboard/performance', icon: Star },
  { name: 'Expenses', href: '/dashboard/expenses', icon: DollarSign },
  { name: 'Documents', href: '/dashboard/documents', icon: FileText },
  { name: 'Benefits', href: '/dashboard/benefits', icon: Briefcase },
  { name: 'Profile', href: '/dashboard/profile', icon: User },
];

export function DashboardNav() {
  const pathname = usePathname();

  return (
    <nav className="w-64 bg-white shadow-sm border-r border-gray-200 min-h-screen">
      <div className="px-4 py-6 space-y-1">
        {navigation.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href;

          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                'flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors',
                isActive
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900'
              )}
            >
              <Icon
                className={cn(
                  'mr-3 h-5 w-5',
                  isActive ? 'text-blue-600' : 'text-gray-400'
                )}
              />
              {item.name}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
