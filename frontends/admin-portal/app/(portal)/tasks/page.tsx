import { auth } from "@/auth"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

export default async function TasksPage() {
  const session = await auth()

  return (
    <div className="container py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">My Tasks</h1>
        <p className="text-muted-foreground">
          View and complete your assigned workflow tasks
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Welcome, {session?.user?.name}!</CardTitle>
          <CardDescription>
            Task management interface will be implemented in Phase 4
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              This is a protected route. You're seeing this because you're authenticated.
            </p>
            <div className="rounded-lg border p-4">
              <h3 className="font-semibold mb-2">Your Roles:</h3>
              <div className="flex gap-2">
                {session?.user?.roles?.map((role) => (
                  <span
                    key={role}
                    className="px-2 py-1 bg-primary/10 text-primary rounded text-sm"
                  >
                    {role}
                  </span>
                )) || <span className="text-muted-foreground">No roles assigned</span>}
              </div>
            </div>
            <div className="text-sm text-muted-foreground">
              <p className="font-semibold mb-2">Coming in Phase 4 (Weeks 7-8):</p>
              <ul className="list-disc list-inside space-y-1">
                <li>Task list with filters (assigned tasks, group tasks)</li>
                <li>Task claiming functionality</li>
                <li>Dynamic form rendering</li>
                <li>Task completion workflow</li>
                <li>Real-time updates</li>
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
