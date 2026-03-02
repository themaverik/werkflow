import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function ProcurementPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Procurement</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Vendor management, purchase requisitions, and purchase order tracking.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Coming Soon</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">
            The Procurement module is under development. It will include vendor management,
            purchase requisition workflows, and purchase order tracking.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
