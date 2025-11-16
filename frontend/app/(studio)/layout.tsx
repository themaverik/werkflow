import { auth } from "@/auth"
import { StudioHeader } from "@/components/layout/studio-header"
import { redirect } from "next/navigation"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

export default async function StudioLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const session = await auth()

  // Check if user has HR_ADMIN role
  const hasAdminRole = session?.user?.roles?.includes("HR_ADMIN")

  if (!hasAdminRole) {
    return (
      <div className="min-h-screen flex flex-col">
        <StudioHeader />
        <main className="flex-1 container py-12">
          <Card className="max-w-2xl mx-auto">
            <CardHeader>
              <CardTitle>Access Denied</CardTitle>
              <CardDescription>
                You don't have permission to access the Studio
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                The Process Studio and Form Builder require the <strong>HR_ADMIN</strong> role.
                Please contact your administrator if you need access.
              </p>
              <div className="mt-4">
                <p className="text-sm font-semibold">Your current roles:</p>
                <div className="flex gap-2 mt-2">
                  {session?.user?.roles?.map((role) => (
                    <span
                      key={role}
                      className="px-2 py-1 bg-secondary text-secondary-foreground rounded text-sm"
                    >
                      {role}
                    </span>
                  )) || <span className="text-muted-foreground">No roles assigned</span>}
                </div>
              </div>
            </CardContent>
          </Card>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex flex-col">
      <StudioHeader />
      <main className="flex-1">{children}</main>
    </div>
  )
}
