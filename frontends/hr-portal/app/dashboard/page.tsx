import { auth } from '@/auth';
import { Calendar, CheckCircle2, Clock, TrendingUp, Users } from 'lucide-react';

export default async function DashboardPage() {
  const session = await auth();

  const stats = [
    {
      name: 'Pending Tasks',
      value: '3',
      icon: Clock,
      change: '+2 from last week',
      changeType: 'neutral' as const,
    },
    {
      name: 'Leave Balance',
      value: '15 days',
      icon: Calendar,
      change: '+5 days accrued',
      changeType: 'positive' as const,
    },
    {
      name: 'Completed Reviews',
      value: '2/3',
      icon: CheckCircle2,
      change: '1 pending',
      changeType: 'neutral' as const,
    },
    {
      name: 'Team Members',
      value: '12',
      icon: Users,
      change: '+2 new hires',
      changeType: 'positive' as const,
    },
  ];

  const recentActivities = [
    {
      id: 1,
      type: 'leave',
      message: 'Annual leave request approved',
      date: '2 hours ago',
      status: 'approved',
    },
    {
      id: 2,
      type: 'task',
      message: 'Performance review form submitted',
      date: '1 day ago',
      status: 'completed',
    },
    {
      id: 3,
      type: 'task',
      message: 'New task assigned: Quarterly goals',
      date: '2 days ago',
      status: 'pending',
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">
          Welcome back, {session?.user?.name || 'Employee'}!
        </h1>
        <p className="mt-1 text-sm text-gray-600">
          Here's what's happening in your workspace today.
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div
              key={stat.name}
              className="bg-white overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow"
            >
              <div className="p-5">
                <div className="flex items-center">
                  <div className="flex-shrink-0">
                    <Icon className="h-6 w-6 text-blue-600" />
                  </div>
                  <div className="ml-5 w-0 flex-1">
                    <dl>
                      <dt className="text-sm font-medium text-gray-500 truncate">
                        {stat.name}
                      </dt>
                      <dd className="flex items-baseline">
                        <div className="text-2xl font-semibold text-gray-900">
                          {stat.value}
                        </div>
                      </dd>
                    </dl>
                  </div>
                </div>
                <div className="mt-4">
                  <div
                    className={`text-sm ${
                      stat.changeType === 'positive'
                        ? 'text-green-600'
                        : 'text-gray-600'
                    }`}
                  >
                    {stat.change}
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Recent Activity */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Recent Activity</h2>
        </div>
        <ul className="divide-y divide-gray-200">
          {recentActivities.map((activity) => (
            <li key={activity.id} className="px-6 py-4 hover:bg-gray-50">
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900">
                    {activity.message}
                  </p>
                  <p className="text-sm text-gray-500">{activity.date}</p>
                </div>
                <div>
                  <span
                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      activity.status === 'approved' || activity.status === 'completed'
                        ? 'bg-green-100 text-green-800'
                        : 'bg-yellow-100 text-yellow-800'
                    }`}
                  >
                    {activity.status}
                  </span>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>

      {/* Quick Actions */}
      <div className="bg-white shadow rounded-lg p-6">
        <h2 className="text-lg font-medium text-gray-900 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <button className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
            Request Leave
          </button>
          <button className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
            Submit Expense
          </button>
          <button className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
            Update Profile
          </button>
        </div>
      </div>
    </div>
  );
}
