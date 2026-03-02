import { auth } from "@/auth"
import { AppShell } from "@/components/layout/app-shell"
import { StudioLayoutClient } from "./layout-client"

export default async function PlatformLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const session = await auth()
  const roles = session?.user?.roles || []

  return (
    <AppShell>
      <StudioLayoutClient initialRoles={roles} session={session}>
        {children}
      </StudioLayoutClient>
    </AppShell>
  )
}
