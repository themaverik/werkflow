'use client';

import { useState } from 'react';
import { BarChart3, TrendingUp, Clock, CheckCircle2, Users, XCircle, RefreshCw, TrendingDown } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import {
  getProcessAnalytics,
  getProcessMetrics,
  getActivityMetrics,
  getProcessTrends,
  type ProcessAnalytics,
  type ProcessMetric,
  type ActivityMetric,
  type ProcessTrend,
} from '@/lib/api/workflows';

export default function AnalyticsPage() {
  const [timeRange, setTimeRange] = useState('7d');

  // Fetch analytics overview with polling
  const { data: analytics, isLoading: analyticsLoading, error: analyticsError } = useQuery<ProcessAnalytics>({
    queryKey: ['process-analytics', timeRange],
    queryFn: () => getProcessAnalytics(timeRange),
    refetchInterval: 60000, // Poll every 60 seconds (analytics don't need real-time updates)
    staleTime: 45000,
  });

  // Fetch process metrics
  const { data: processMetrics, isLoading: metricsLoading, error: metricsError } = useQuery<ProcessMetric[]>({
    queryKey: ['process-metrics', timeRange],
    queryFn: () => getProcessMetrics(timeRange),
    refetchInterval: 60000,
    staleTime: 45000,
  });

  // Fetch activity metrics
  const { data: activityMetrics, isLoading: activityLoading, error: activityError } = useQuery<ActivityMetric[]>({
    queryKey: ['activity-metrics', timeRange],
    queryFn: () => getActivityMetrics(timeRange),
    refetchInterval: 60000,
    staleTime: 45000,
  });

  // Fetch process trends
  const { data: processTrends, isLoading: trendsLoading, error: trendsError } = useQuery<ProcessTrend[]>({
    queryKey: ['process-trends', timeRange],
    queryFn: () => getProcessTrends(timeRange),
    refetchInterval: 60000,
    staleTime: 45000,
  });

  // Loading state
  if (analyticsLoading || metricsLoading || activityLoading || trendsLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <RefreshCw className="h-8 w-8 animate-spin text-blue-600 mx-auto mb-4" />
            <p className="text-gray-600">Loading analytics data...</p>
          </div>
        </div>
      </div>
    );
  }

  // Error state
  if (analyticsError || metricsError || activityError || trendsError) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <XCircle className="h-12 w-12 text-red-600 mx-auto mb-4" />
            <p className="text-gray-900 font-medium mb-2">Failed to load analytics data</p>
            <p className="text-gray-600 text-sm mb-4">
              {(analyticsError || metricsError || activityError || trendsError)?.toString() || 'Unknown error occurred'}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Helper function to format percentage change
  const formatChange = (value: number) => {
    const sign = value >= 0 ? '+' : '';
    return `${sign}${value}%`;
  };

  // Helper function to get change color
  const getChangeColor = (value: number, reverse: boolean = false) => {
    if (reverse) {
      return value <= 0 ? 'text-green-600' : 'text-red-600';
    }
    return value >= 0 ? 'text-green-600' : 'text-red-600';
  };

  // Helper function to get change icon
  const getChangeIcon = (value: number, reverse: boolean = false) => {
    const isPositive = reverse ? value <= 0 : value >= 0;
    const Icon = isPositive ? TrendingUp : TrendingDown;
    const color = getChangeColor(value, reverse);
    return <Icon className={`h-4 w-4 ${color}`} />;
  };

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
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="24h">Last 24 Hours</option>
            <option value="7d">Last 7 Days</option>
            <option value="30d">Last 30 Days</option>
            <option value="90d">Last 90 Days</option>
          </select>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Total Processes</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">
                {analytics?.totalProcesses || 0}
              </p>
              {analytics?.changeFromLastPeriod && (
                <div className={`mt-1 text-sm flex items-center space-x-1 ${getChangeColor(analytics.changeFromLastPeriod.processes)}`}>
                  {getChangeIcon(analytics.changeFromLastPeriod.processes)}
                  <span>{formatChange(analytics.changeFromLastPeriod.processes)} from last period</span>
                </div>
              )}
            </div>
            <BarChart3 className="h-10 w-10 text-blue-600" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Avg Completion Rate</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">
                {analytics?.avgCompletionRate ? `${analytics.avgCompletionRate}%` : 'N/A'}
              </p>
              {analytics?.changeFromLastPeriod && (
                <div className={`mt-1 text-sm flex items-center space-x-1 ${getChangeColor(analytics.changeFromLastPeriod.completionRate)}`}>
                  {getChangeIcon(analytics.changeFromLastPeriod.completionRate)}
                  <span>{formatChange(analytics.changeFromLastPeriod.completionRate)} from last period</span>
                </div>
              )}
            </div>
            <CheckCircle2 className="h-10 w-10 text-green-600" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Avg Duration</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">
                {analytics?.avgDuration || 'N/A'}
              </p>
              {analytics?.changeFromLastPeriod && (
                <div className={`mt-1 text-sm flex items-center space-x-1 ${getChangeColor(analytics.changeFromLastPeriod.duration, true)}`}>
                  {getChangeIcon(analytics.changeFromLastPeriod.duration, true)}
                  <span>{formatChange(analytics.changeFromLastPeriod.duration)} from last period</span>
                </div>
              )}
            </div>
            <Clock className="h-10 w-10 text-yellow-600" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Active Users</p>
              <p className="mt-2 text-3xl font-semibold text-gray-900">
                {analytics?.activeUsers || 0}
              </p>
              {analytics?.changeFromLastPeriod && (
                <div className={`mt-1 text-sm flex items-center space-x-1 ${getChangeColor(analytics.changeFromLastPeriod.users)}`}>
                  {getChangeIcon(analytics.changeFromLastPeriod.users)}
                  <span>{formatChange(analytics.changeFromLastPeriod.users)} from last period</span>
                </div>
              )}
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
        {processMetrics && processMetrics.length > 0 ? (
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
                {processMetrics.map((process) => (
                  <tr key={process.processDefinitionKey} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {process.processName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {process.totalInstances}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-green-600 font-medium">
                      {process.completed}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-red-600 font-medium">
                      {process.failed}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-blue-600 font-medium">
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
                        <span className="text-sm text-gray-900 font-medium">{process.completionRate}%</span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="px-6 py-12 text-center text-gray-500">
            <BarChart3 className="h-12 w-12 mx-auto mb-3 text-gray-400" />
            <p className="text-sm">No process metrics available for selected time range</p>
          </div>
        )}
      </div>

      {/* Activity Bottlenecks */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Activity Analysis</h2>
          <p className="mt-1 text-sm text-gray-500">
            Identify bottlenecks and optimize workflow performance
          </p>
        </div>
        {activityMetrics && activityMetrics.length > 0 ? (
          <ul className="divide-y divide-gray-200">
            {activityMetrics.map((activity) => (
              <li key={activity.activityId} className="px-6 py-4 hover:bg-gray-50">
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
                    <div className="mt-1 flex items-center space-x-4 text-sm text-gray-500">
                      <span>Average Duration: {activity.avgDuration}</span>
                      <span>Instances: {activity.instances}</span>
                    </div>
                  </div>
                  <div className="flex items-center">
                    <Clock className={`h-5 w-5 ${activity.bottleneck ? 'text-red-600' : 'text-gray-400'}`} />
                  </div>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <div className="px-6 py-12 text-center text-gray-500">
            <Clock className="h-12 w-12 mx-auto mb-3 text-gray-400" />
            <p className="text-sm">No activity data available for selected time range</p>
          </div>
        )}
      </div>

      {/* Process Trends Placeholder */}
      <div className="bg-white shadow rounded-lg p-6">
        <h2 className="text-lg font-medium text-gray-900 mb-4">Process Trends</h2>
        {processTrends && processTrends.length > 0 ? (
          <div className="space-y-4">
            <div className="text-sm text-gray-600">
              Showing {processTrends.length} data points for selected time range
            </div>
            <div className="h-64 flex items-center justify-center bg-gray-50 rounded border-2 border-dashed border-gray-300">
              <div className="text-center">
                <BarChart3 className="h-12 w-12 text-gray-400 mx-auto mb-2" />
                <p className="text-sm text-gray-500">
                  Time-series chart visualization
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  Data ready - chart library integration pending
                </p>
              </div>
            </div>
          </div>
        ) : (
          <div className="h-64 flex items-center justify-center bg-gray-50 rounded border-2 border-dashed border-gray-300">
            <div className="text-center">
              <BarChart3 className="h-12 w-12 text-gray-400 mx-auto mb-2" />
              <p className="text-sm text-gray-500">
                No trend data available for selected time range
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
