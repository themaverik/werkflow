import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

export default function LeavePage() {
  const leaveBalance = {
    annual: { total: 20, used: 5, remaining: 15 },
    sick: { total: 10, used: 2, remaining: 8 },
    personal: { total: 5, used: 1, remaining: 4 },
  }

  const leaveRequests = [
    { id: 1, type: 'Annual Leave', startDate: '2024-12-20', endDate: '2024-12-27', days: 6, status: 'approved', reason: 'Year-end vacation', approver: 'John Smith' },
    { id: 2, type: 'Sick Leave', startDate: '2024-11-05', endDate: '2024-11-06', days: 2, status: 'approved', reason: 'Medical appointment', approver: 'John Smith' },
    { id: 3, type: 'Personal Leave', startDate: '2024-11-18', endDate: '2024-11-18', days: 1, status: 'pending', reason: 'Family matter', approver: 'Pending approval' },
  ]

  const balanceCards = [
    { label: 'Annual Leave', data: leaveBalance.annual, color: 'bg-blue-600' },
    { label: 'Sick Leave', data: leaveBalance.sick, color: 'bg-red-600' },
    { label: 'Personal Leave', data: leaveBalance.personal, color: 'bg-green-600' },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Leave Management</h1>
          <p className="mt-1 text-sm text-muted-foreground">View your leave balance and submit requests</p>
        </div>
        <Button>Request Leave</Button>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {balanceCards.map(({ label, data, color }) => (
          <Card key={label}>
            <CardContent className="p-6">
              <p className="text-sm font-medium text-muted-foreground">{label}</p>
              <p className="mt-2 text-3xl font-semibold">{data.remaining}</p>
              <p className="text-sm text-muted-foreground">of {data.total} days remaining</p>
              <div className="mt-4 w-full bg-muted rounded-full h-2">
                <div
                  className={`${color} h-2 rounded-full`}
                  style={{ width: `${(data.remaining / data.total) * 100}%` }}
                />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Leave Requests</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="divide-y">
            {leaveRequests.map((request) => (
              <li key={request.id} className="py-4">
                <div className="flex items-center space-x-3 mb-2">
                  <h3 className="text-lg font-medium">{request.type}</h3>
                  <span
                    className={`inline-flex items-center px-3 py-0.5 rounded-full text-xs font-medium ${
                      request.status === 'approved' ? 'bg-green-100 text-green-800'
                        : request.status === 'pending' ? 'bg-yellow-100 text-yellow-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {request.status}
                  </span>
                </div>
                <div className="grid grid-cols-2 gap-4 text-sm text-muted-foreground">
                  <div><span className="font-medium">Dates:</span> {request.startDate} - {request.endDate}</div>
                  <div><span className="font-medium">Duration:</span> {request.days} days</div>
                  <div><span className="font-medium">Reason:</span> {request.reason}</div>
                  <div><span className="font-medium">Approver:</span> {request.approver}</div>
                </div>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  )
}
