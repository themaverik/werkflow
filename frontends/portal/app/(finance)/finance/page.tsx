import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function FinancePage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Finance</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Budget management, expense tracking, and capital expenditure approvals.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Coming Soon</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">
            The Finance module is under development. It will include budget tracking,
            expense management, and CapEx approval workflows.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
