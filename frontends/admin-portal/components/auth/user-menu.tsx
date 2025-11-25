import { auth, signOut } from "@/auth"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

export async function UserMenu() {
  const session = await auth()

  if (!session?.user) {
    return null
  }

  const userInitials = session.user.name
    ? session.user.name
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
    : "U"

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="relative h-10 w-10 rounded-full p-0">
          <div className="flex h-full w-full items-center justify-center rounded-full bg-primary text-primary-foreground">
            {userInitials}
          </div>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-56" align="end" forceMount>
        <DropdownMenuLabel className="font-normal">
          <div className="flex flex-col space-y-1">
            <p className="text-sm font-medium leading-none">{session.user.name}</p>
            <p className="text-xs leading-none text-muted-foreground">
              {session.user.email}
            </p>
            {session.user.roles && session.user.roles.length > 0 && (
              <p className="text-xs leading-none text-muted-foreground mt-1">
                Roles: {session.user.roles.join(", ")}
              </p>
            )}
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <a href="/portal/tasks">My Tasks</a>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <a href="/portal/processes">My Processes</a>
        </DropdownMenuItem>
        {session.user.roles?.includes("HR_ADMIN") && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <a href="/studio/processes">Process Designer</a>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
              <a href="/studio/forms">Form Builder</a>
            </DropdownMenuItem>
          </>
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <form
            action={async () => {
              "use server"
              await signOut({ redirectTo: "/" })
            }}
          >
            <button type="submit" className="w-full text-left">
              Sign out
            </button>
          </form>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
