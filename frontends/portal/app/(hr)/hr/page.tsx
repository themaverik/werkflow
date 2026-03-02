import { auth } from '@/auth'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default async function HRDashboardPage() {
  const session = await auth()

  const stats = [
    { name: 'Pending Tasks', value: '3', change: '+2 from last week' },
    { name: 'Leave Balance', value: '15 days', change: '+5 days accrued' },
    { name: 'Completed Reviews', value: '2/3', change: '1 pending' },
    { name: 'Team Members', value: '12', change: '+2 new hires' },
  ]

  const recentActivities = [
    { id: 1, message: 'Annual leave request approved', date: '2 hours ago', status: 'approved' },
    { id: 2, message: 'Performance review form submitted', date: '1 day ago', status: 'completed' },
    { id: 3, message: 'New task assigned: Quarterly goals', date: '2 days ago', status: 'pending' },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">
          Welcome back, {session?.user?.name || 'Employee'}!
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Here's what's happening in your workspace today.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.name}>
            <CardContent className="p-5">
              <p className="text-sm font-medium text-muted-foreground">{stat.name}</p>
              <p className="text-2xl font-semibold mt-1">{stat.value}</p>
              <p className="text-sm text-muted-foreground mt-2">{stat.change}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent Activity</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="divide-y">
            {recentActivities.map((activity) => (
              <li key={activity.id} className="py-4 flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium">{activity.message}</p>
                  <p className="text-sm text-muted-foreground">{activity.date}</p>
                </div>
                <span
                  className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    activity.status === 'approved' || activity.status === 'completed'
                      ? 'bg-green-100 text-green-800'
                      : 'bg-yellow-100 text-yellow-800'
                  }`}
                >
                  {activity.status}
                </span>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <button className="px-4 py-2 text-sm font-medium border rounded-md hover:bg-muted">
              Request Leave
            </button>
            <button className="px-4 py-2 text-sm font-medium border rounded-md hover:bg-muted">
              Submit Expense
            </button>
            <button className="px-4 py-2 text-sm font-medium border rounded-md hover:bg-muted">
              Update Profile
            </button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
