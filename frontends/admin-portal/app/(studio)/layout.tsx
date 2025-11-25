import { auth } from "@/auth"
import { StudioHeader } from "@/components/layout/studio-header"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { StudioLayoutClient } from "./layout-client"

export default async function StudioLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const session = await auth()
  const roles = session?.user?.roles || []

  return (
    <div className="min-h-screen flex flex-col">
      <StudioHeader />
      <main className="flex-1">
        <StudioLayoutClient initialRoles={roles} session={session}>
          {children}
        </StudioLayoutClient>
      </main>
    </div>
  )
}
