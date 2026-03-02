import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function InventoryPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Inventory</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Asset management, stock tracking, and inventory transfers.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Coming Soon</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">
            The Inventory module is under development. It will include asset tracking,
            stock management, and inventory transfer workflows.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
