import { Activity, CheckCircle2, Clock, XCircle, TrendingUp, Users } from 'lucide-react';
import Link from 'next/link';

export default async function ProcessMonitoringPage() {
  // In production, fetch from Engine Service API
  const stats = {
    activeProcesses: 24,
    completedToday: 18,
    failedToday: 2,
    avgCompletionTime: '2.5 hours',
    totalDeployed: 12,
    activeUsers: 45,
  };

  const runningProcesses = [
    {
      id: 'proc-001',
      processDefinitionKey: 'employee-onboarding',
      processDefinitionName: 'Employee Onboarding',
      businessKey: 'EMP-2024-001',
      startTime: '2024-11-16T10:30:00Z',
      startedBy: 'hr.manager@werkflow.com',
      currentActivity: 'Manager Approval',
      status: 'active',
    },
    {
      id: 'proc-002',
      processDefinitionKey: 'leave-request',
      processDefinitionName: 'Leave Request',
      businessKey: 'LEAVE-2024-123',
      startTime: '2024-11-16T09:15:00Z',
      startedBy: 'john.doe@werkflow.com',
      currentActivity: 'HR Review',
      status: 'active',
    },
    {
      id: 'proc-003',
      processDefinitionKey: 'performance-review',
      processDefinitionName: 'Performance Review Q4',
      businessKey: 'PERF-Q4-2024-045',
      startTime: '2024-11-16T08:00:00Z',
      startedBy: 'manager@werkflow.com',
      currentActivity: 'Employee Self Assessment',
      status: 'active',
    },
    {
      id: 'proc-004',
      processDefinitionKey: 'expense-claim',
      processDefinitionName: 'Expense Claim',
      businessKey: 'EXP-2024-789',
      startTime: '2024-11-16T11:45:00Z',
      startedBy: 'employee@werkflow.com',
      currentActivity: 'Finance Approval',
      status: 'suspended',
    },
  ];

  const recentActivities = [
    {
      id: 1,
      type: 'completed',
      message: 'Leave Request LEAVE-2024-120 completed successfully',
      timestamp: '5 minutes ago',
      user: 'system',
    },
    {
      id: 2,
      type: 'started',
      message: 'New Expense Claim process started by employee@werkflow.com',
      timestamp: '15 minutes ago',
      user: 'employee@werkflow.com',
    },
    {
      id: 3,
      type: 'failed',
      message: 'Onboarding process ONB-2024-032 failed at Document Verification',
      timestamp: '1 hour ago',
      user: 'system',
    },
    {
      id: 4,
      type: 'deployed',
      message: 'New process definition deployed: Asset Request v2.0',
      timestamp: '2 hours ago',
      user: 'admin@werkflow.com',
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Process Monitoring</h1>
          <p className="mt-1 text-sm text-gray-600">
            Real-time workflow execution monitoring and analytics
          </p>
        </div>
        <div className="flex space-x-2">
          <button className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
            Export Report
          </button>
          <button className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700">
            Refresh
          </button>
        </div>
      </div>

      {/* Statistics Grid */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <Activity className="h-6 w-6 text-blue-600" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">
                    Active Processes
                  </dt>
                  <dd className="flex items-baseline">
                    <div className="text-2xl font-semibold text-gray-900">
                      {stats.activeProcesses}
                    </div>
                    <div className="ml-2 flex items-baseline text-sm font-semibold text-green-600">
                      <TrendingUp className="h-4 w-4 mr-1" />
                      12%
                    </div>
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <CheckCircle2 className="h-6 w-6 text-green-600" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">
                    Completed Today
                  </dt>
                  <dd className="flex items-baseline">
                    <div className="text-2xl font-semibold text-gray-900">
                      {stats.completedToday}
                    </div>
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <XCircle className="h-6 w-6 text-red-600" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">
                    Failed Today
                  </dt>
                  <dd className="flex items-baseline">
                    <div className="text-2xl font-semibold text-gray-900">
                      {stats.failedToday}
                    </div>
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <Clock className="h-6 w-6 text-yellow-600" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">
                    Avg Completion Time
                  </dt>
                  <dd className="flex items-baseline">
                    <div className="text-2xl font-semibold text-gray-900">
                      {stats.avgCompletionTime}
                    </div>
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <Activity className="h-6 w-6 text-purple-600" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">
                    Deployed Definitions
                  </dt>
                  <dd className="flex items-baseline">
                    <div className="text-2xl font-semibold text-gray-900">
                      {stats.totalDeployed}
                    </div>
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <Users className="h-6 w-6 text-indigo-600" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">
                    Active Users
                  </dt>
                  <dd className="flex items-baseline">
                    <div className="text-2xl font-semibold text-gray-900">
                      {stats.activeUsers}
                    </div>
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Running Processes */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200 flex items-center justify-between">
          <h2 className="text-lg font-medium text-gray-900">Running Process Instances</h2>
          <Link
            href="/portal/processes"
            className="text-sm text-blue-600 hover:text-blue-700 font-medium"
          >
            View All
          </Link>
        </div>
        <ul className="divide-y divide-gray-200">
          {runningProcesses.map((process) => (
            <li key={process.id} className="p-6 hover:bg-gray-50">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3">
                    <h3 className="text-lg font-medium text-gray-900">
                      {process.processDefinitionName}
                    </h3>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        process.status === 'active'
                          ? 'bg-green-100 text-green-800'
                          : 'bg-yellow-100 text-yellow-800'
                      }`}
                    >
                      {process.status}
                    </span>
                  </div>
                  <div className="mt-2 grid grid-cols-2 gap-4 text-sm text-gray-600">
                    <div>
                      <span className="font-medium">Business Key:</span> {process.businessKey}
                    </div>
                    <div>
                      <span className="font-medium">Current Activity:</span> {process.currentActivity}
                    </div>
                    <div>
                      <span className="font-medium">Started:</span>{' '}
                      {new Date(process.startTime).toLocaleString()}
                    </div>
                    <div>
                      <span className="font-medium">Started By:</span> {process.startedBy}
                    </div>
                  </div>
                </div>
                <div className="ml-4 flex space-x-2">
                  <button className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
                    View Details
                  </button>
                  {process.status === 'suspended' && (
                    <button className="px-3 py-1.5 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700">
                      Resume
                    </button>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
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
                  <div className="flex items-center space-x-2">
                    {activity.type === 'completed' && (
                      <CheckCircle2 className="h-5 w-5 text-green-600" />
                    )}
                    {activity.type === 'started' && (
                      <Activity className="h-5 w-5 text-blue-600" />
                    )}
                    {activity.type === 'failed' && (
                      <XCircle className="h-5 w-5 text-red-600" />
                    )}
                    {activity.type === 'deployed' && (
                      <TrendingUp className="h-5 w-5 text-purple-600" />
                    )}
                    <p className="text-sm font-medium text-gray-900">{activity.message}</p>
                  </div>
                  <p className="mt-1 text-sm text-gray-500">{activity.timestamp}</p>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
