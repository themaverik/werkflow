'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useAuth } from '@/lib/auth/auth-context'
import { useAuthorization } from '@/lib/auth/use-authorization'
import type { NavigationItem } from '@/components/role-based-nav'

interface SidebarSection {
  title: string
  items: NavigationItem[]
}

const sidebarSections: SidebarSection[] = [
  {
    title: 'General',
    items: [
      { label: 'Dashboard', href: '/dashboard' },
      { label: 'My Tasks', href: '/tasks' },
      { label: 'My Requests', href: '/requests' },
    ],
  },
  {
    title: 'Design Studio',
    items: [
      { label: 'Processes', href: '/processes', requiredRoles: ['ADMIN', 'WORKFLOW_ADMIN', 'SUPER_ADMIN'] },
      { label: 'Forms', href: '/forms', requiredRoles: ['ADMIN', 'WORKFLOW_ADMIN', 'SUPER_ADMIN'] },
      { label: 'Services', href: '/services', requiredRoles: ['ADMIN', 'WORKFLOW_ADMIN', 'SUPER_ADMIN'] },
      { label: 'Workflows', href: '/workflows', requiredRoles: ['ADMIN', 'WORKFLOW_ADMIN', 'SUPER_ADMIN'] },
    ],
  },
  {
    title: 'HR',
    items: [
      { label: 'HR Dashboard', href: '/hr', requiredRoles: ['HR_STAFF', 'HR_ADMIN', 'ADMIN', 'SUPER_ADMIN'] },
      { label: 'Leave', href: '/hr/leave', requiredRoles: ['HR_STAFF', 'HR_ADMIN', 'ADMIN', 'SUPER_ADMIN'] },
      { label: 'Performance', href: '/hr/performance', requiredRoles: ['HR_STAFF', 'HR_ADMIN', 'ADMIN', 'SUPER_ADMIN'] },
      { label: 'Profile', href: '/hr/profile', requiredRoles: ['HR_STAFF', 'HR_ADMIN', 'ADMIN', 'SUPER_ADMIN'] },
    ],
  },
  {
    title: 'Finance',
    items: [
      { label: 'Finance', href: '/finance', requiredRoles: ['FINANCE_STAFF', 'FINANCE_ADMIN', 'ADMIN', 'SUPER_ADMIN'] },
    ],
  },
  {
    title: 'Procurement',
    items: [
      { label: 'Procurement', href: '/procurement', requiredRoles: ['PROCUREMENT_STAFF', 'PROCUREMENT_ADMIN', 'ADMIN', 'SUPER_ADMIN'] },
    ],
  },
  {
    title: 'Inventory',
    items: [
      { label: 'Inventory', href: '/inventory', requiredRoles: ['INVENTORY_ADMIN', 'ADMIN', 'SUPER_ADMIN'] },
    ],
  },
  {
    title: 'System',
    items: [
      { label: 'Monitoring', href: '/monitoring', requiredRoles: ['ADMIN', 'SUPER_ADMIN'] },
      { label: 'Analytics', href: '/analytics', requiredRoles: ['ADMIN', 'SUPER_ADMIN'] },
    ],
  },
]

export function Sidebar() {
  const pathname = usePathname()
  const { isAuthenticated } = useAuth()
  const { hasAnyRole } = useAuthorization()

  if (!isAuthenticated) {
    return null
  }

  const visibleSections = sidebarSections
    .map(section => ({
      ...section,
      items: section.items.filter(item => {
        if (!item.requiredRoles || item.requiredRoles.length === 0) return true
        return hasAnyRole(item.requiredRoles)
      }),
    }))
    .filter(section => section.items.length > 0)

  return (
    <aside className="w-64 border-r bg-background min-h-[calc(100vh-3.5rem)] p-4 space-y-6 shrink-0">
      {visibleSections.map(section => (
        <div key={section.title}>
          <h3 className="px-3 mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            {section.title}
          </h3>
          <nav className="space-y-1">
            {section.items.map(item => {
              const isActive = pathname === item.href || pathname?.startsWith(item.href + '/')
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center px-3 py-2 text-sm rounded-md transition-colors ${
                    isActive
                      ? 'bg-primary/10 text-primary font-medium'
                      : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                  }`}
                >
                  {item.label}
                </Link>
              )
            })}
          </nav>
        </div>
      ))}
    </aside>
  )
}
