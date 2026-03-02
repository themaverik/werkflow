import { UserMenu } from '@/components/auth/user-menu'
import { Sidebar } from '@/components/layout/sidebar'
import Link from 'next/link'

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-14 items-center">
          <Link href="/" className="mr-6 flex items-center space-x-2">
            <span className="font-bold text-xl">Werkflow Portal</span>
          </Link>
          <div className="flex flex-1 items-center justify-end space-x-2">
            <UserMenu />
          </div>
        </div>
      </header>
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  )
}
