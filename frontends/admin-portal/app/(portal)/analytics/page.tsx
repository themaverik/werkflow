'use client';

import { useState } from 'react';
import { BarChart3, TrendingUp, Clock, CheckCircle2, Users, Calendar } from 'lucide-react';

export default function AnalyticsPage() {
  const [timeRange, setTimeRange] = useState('7d');

  const processMetrics = [
    {
      processName: 'Employee Onboarding',
      totalInstances: 45,
      completed: 38,
      failed: 2,
      active: 5,
      avgDuration: '4.2 hours',
      completionRate: 84,
    },
    {
      processName: 'Leave Request',
      totalInstances: 128,
      completed: 120,
      failed: 1,
      active: 7,
      avgDuration: '1.5 hours',
      completionRate: 94,
    },
    {
      processName: 'Performance Review',
      totalInstances: 32,
      completed: 25,
      failed: 0,
      active: 7,
      avgDuration: '8.5 hours',
      completionRate: 78,
    },
    {
      processName: 'Expense Claim',
      totalInstances: 89,
      completed: 82,
      failed: 3,
      active: 4,
      avgDuration: '2.1 hours',
      completionRate: 92,
    },
  ];

  const activityMetrics = [
    { activity: 'Document Upload', avgDuration: '15 min', bottleneck: false },
    { activity: 'Manager Approval', avgDuration: '2.5 hours', bottleneck: true },
    { activity: 'HR Review', avgDuration: '1.2 hours', bottleneck: false },
    { activity: 'Finance Approval', avgDuration: '3.8 hours', bottleneck: true },
    { activity: 'Final Verification', avgDuration: '25 min', bottleneck: false },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Process Analytics</h1>
          <p className="mt-1 text-sm text-gray-600">
            Performance metrics and insights for workflow processes
          </p>
        </div>
        <div className="flex space-x-2">
          <select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value)}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
          >
            <option value="24h">Last 24 Hours</option>
            <option value="7d">Last 7 Days</option>
            <option value="30d">Last 30 Days</option>
            <option value="90d">Last 90 Days</option>
          </select>
          <button className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
            Export Report
          </button>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Total Processes</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">294</p>
              <p className="mt-1 text-sm text-green-600">+12% from last period</p>
            </div>
            <BarChart3 className="h-10 w-10 text-blue-600" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Avg Completion Rate</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">87%</p>
              <p className="mt-1 text-sm text-green-600">+3% from last period</p>
            </div>
            <CheckCircle2 className="h-10 w-10 text-green-600" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Avg Duration</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">3.2h</p>
              <p className="mt-1 text-sm text-red-600">+5% from last period</p>
            </div>
            <Clock className="h-10 w-10 text-yellow-600" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Active Users</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">68</p>
              <p className="mt-1 text-sm text-green-600">+8% from last period</p>
            </div>
            <Users className="h-10 w-10 text-purple-600" />
          </div>
        </div>
      </div>

      {/* Process Performance Table */}
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="px-6 py-5 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Process Performance</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Process Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Total Instances
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Completed
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Failed
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Active
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Avg Duration
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Completion Rate
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {processMetrics.map((process, idx) => (
                <tr key={idx} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {process.processName}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {process.totalInstances}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-green-600">
                    {process.completed}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-red-600">
                    {process.failed}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-blue-600">
                    {process.active}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {process.avgDuration}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="w-16 bg-gray-200 rounded-full h-2 mr-2">
                        <div
                          className="bg-green-600 h-2 rounded-full"
                          style={{ width: `${process.completionRate}%` }}
                        ></div>
                      </div>
                      <span className="text-sm text-gray-900">{process.completionRate}%</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Activity Bottlenecks */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Activity Analysis</h2>
          <p className="mt-1 text-sm text-gray-500">
            Identify bottlenecks and optimize workflow performance
          </p>
        </div>
        <ul className="divide-y divide-gray-200">
          {activityMetrics.map((activity, idx) => (
            <li key={idx} className="px-6 py-4 hover:bg-gray-50">
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-3">
                    <h3 className="text-sm font-medium text-gray-900">{activity.activity}</h3>
                    {activity.bottleneck && (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                        Bottleneck
                      </span>
                    )}
                  </div>
                  <p className="mt-1 text-sm text-gray-500">
                    Average Duration: {activity.avgDuration}
                  </p>
                </div>
                <div className="flex items-center">
                  <Clock className={`h-5 w-5 ${activity.bottleneck ? 'text-red-600' : 'text-gray-400'}`} />
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>

      {/* Time-based Chart Placeholder */}
      <div className="bg-white shadow rounded-lg p-6">
        <h2 className="text-lg font-medium text-gray-900 mb-4">Process Trends</h2>
        <div className="h-64 flex items-center justify-center bg-gray-50 rounded border-2 border-dashed border-gray-300">
          <div className="text-center">
            <BarChart3 className="h-12 w-12 text-gray-400 mx-auto mb-2" />
            <p className="text-sm text-gray-500">
              Time-series chart will be rendered here with Recharts
            </p>
            <p className="text-xs text-gray-400 mt-1">
              Showing process completion trends over time
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
