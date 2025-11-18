'use client'

import { Activity, CheckCircle2, Clock, XCircle, TrendingUp, Users, RefreshCw } from 'lucide-react';
import Link from 'next/link';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import {
  getProcessStatistics,
  getRunningProcesses,
  getActivityLogs,
  activateProcessInstance,
  type ProcessStatistics,
  type RunningProcessInstance,
  type ActivityLogEntry
} from '@/lib/api/workflows';
import { useState } from 'react';

export default function ProcessMonitoringPage() {
  const queryClient = useQueryClient();
  const [refreshing, setRefreshing] = useState(false);

  // Fetch process statistics with 30-second polling
  const { data: stats, isLoading: statsLoading, error: statsError } = useQuery<ProcessStatistics>({
    queryKey: ['process-statistics'],
    queryFn: getProcessStatistics,
    refetchInterval: 30000, // Poll every 30 seconds
    staleTime: 20000, // Consider data stale after 20 seconds
  });

  // Fetch running processes with 30-second polling
  const { data: runningProcesses, isLoading: processesLoading, error: processesError } = useQuery<RunningProcessInstance[]>({
    queryKey: ['running-processes'],
    queryFn: getRunningProcesses,
    refetchInterval: 30000,
    staleTime: 20000,
  });

  // Fetch recent activities with 30-second polling
  const { data: recentActivities, isLoading: activitiesLoading, error: activitiesError } = useQuery<ActivityLogEntry[]>({
    queryKey: ['activity-logs'],
    queryFn: () => getActivityLogs(10),
    refetchInterval: 30000,
    staleTime: 20000,
  });

  // Mutation for activating/resuming suspended processes
  const activateMutation = useMutation({
    mutationFn: (processInstanceId: string) => activateProcessInstance(processInstanceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['running-processes'] });
      queryClient.invalidateQueries({ queryKey: ['process-statistics'] });
    },
  });

  // Manual refresh handler
  const handleRefresh = async () => {
    setRefreshing(true);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['process-statistics'] }),
      queryClient.invalidateQueries({ queryKey: ['running-processes'] }),
      queryClient.invalidateQueries({ queryKey: ['activity-logs'] }),
    ]);
    setTimeout(() => setRefreshing(false), 500);
  };

  // Loading state
  if (statsLoading || processesLoading || activitiesLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <RefreshCw className="h-8 w-8 animate-spin text-blue-600 mx-auto mb-4" />
            <p className="text-gray-600">Loading monitoring data...</p>
          </div>
        </div>
      </div>
    );
  }

  // Error state
  if (statsError || processesError || activitiesError) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <XCircle className="h-12 w-12 text-red-600 mx-auto mb-4" />
            <p className="text-gray-900 font-medium mb-2">Failed to load monitoring data</p>
            <p className="text-gray-600 text-sm mb-4">
              {(statsError || processesError || activitiesError)?.toString() || 'Unknown error occurred'}
            </p>
            <button
              onClick={handleRefresh}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

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
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
            <span>{refreshing ? 'Refreshing...' : 'Refresh'}</span>
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
                      {stats?.activeProcesses || 0}
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
                      {stats?.completedToday || 0}
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
                      {stats?.failedToday || 0}
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
                      {stats?.avgCompletionTime || 'N/A'}
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
                      {stats?.totalDeployed || 0}
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
                      {stats?.activeUsers || 0}
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
        {runningProcesses && runningProcesses.length > 0 ? (
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
                        <span className="font-medium">Business Key:</span> {process.businessKey || 'N/A'}
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
                    <Link href={`/portal/processes/${process.id}`}>
                      <button className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
                        View Details
                      </button>
                    </Link>
                    {process.status === 'suspended' && (
                      <button
                        onClick={() => activateMutation.mutate(process.id)}
                        disabled={activateMutation.isPending}
                        className="px-3 py-1.5 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50"
                      >
                        {activateMutation.isPending ? 'Resuming...' : 'Resume'}
                      </button>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <div className="px-6 py-12 text-center text-gray-500">
            <Activity className="h-12 w-12 mx-auto mb-3 text-gray-400" />
            <p className="text-sm">No running process instances found</p>
          </div>
        )}
      </div>

      {/* Recent Activity */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-5 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Recent Activity</h2>
        </div>
        {recentActivities && recentActivities.length > 0 ? (
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
        ) : (
          <div className="px-6 py-12 text-center text-gray-500">
            <Clock className="h-12 w-12 mx-auto mb-3 text-gray-400" />
            <p className="text-sm">No recent activity found</p>
          </div>
        )}
      </div>
    </div>
  );
}
