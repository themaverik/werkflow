import Link from "next/link"
import { UserMenu } from "@/components/auth/user-menu"
import { Button } from "@/components/ui/button"

export function Header() {
  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-14 items-center">
        <div className="mr-4 flex">
          <Link href="/" className="mr-6 flex items-center space-x-2">
            <span className="font-bold text-xl">Werkflow</span>
          </Link>
          <nav className="flex items-center space-x-6 text-sm font-medium">
            <Link
              href="/portal/tasks"
              className="transition-colors hover:text-foreground/80 text-foreground/60"
            >
              Tasks
            </Link>
            <Link
              href="/portal/processes"
              className="transition-colors hover:text-foreground/80 text-foreground/60"
            >
              Processes
            </Link>
          </nav>
        </div>
        <div className="flex flex-1 items-center justify-between space-x-2 md:justify-end">
          <nav className="flex items-center space-x-2">
            <UserMenu />
          </nav>
        </div>
      </div>
    </header>
  )
}
