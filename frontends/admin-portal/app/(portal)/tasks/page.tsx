'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getMyTasks,
  getGroupTasks,
  claimTaskForCurrentUser,
  unclaimTask,
  type TaskInfo,
} from '@/lib/api/workflows'
import {
  Activity,
  CheckCircle2,
  Clock,
  AlertCircle,
  RefreshCw,
  User,
  Users,
  Calendar,
  FileText,
  XCircle,
} from 'lucide-react'
import Link from 'next/link'
import FormRenderer from '@/components/forms/FormRenderer'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'

export default function TasksPage() {
  const queryClient = useQueryClient()
  const [activeTab, setActiveTab] = useState<'my-tasks' | 'group-tasks'>('my-tasks')
  const [selectedTask, setSelectedTask] = useState<TaskInfo | null>(null)
  const [showFormDialog, setShowFormDialog] = useState(false)
  const [refreshing, setRefreshing] = useState(false)

  // Fetch My Tasks with polling
  const { data: myTasks, isLoading: myTasksLoading, error: myTasksError } = useQuery<TaskInfo[]>({
    queryKey: ['my-tasks'],
    queryFn: getMyTasks,
    refetchInterval: 30000, // Poll every 30 seconds
    staleTime: 20000,
  })

  // Fetch Group Tasks with polling
  const { data: groupTasks, isLoading: groupTasksLoading, error: groupTasksError } = useQuery<TaskInfo[]>({
    queryKey: ['group-tasks'],
    queryFn: getGroupTasks,
    refetchInterval: 30000,
    staleTime: 20000,
  })

  // Claim task mutation
  const claimMutation = useMutation({
    mutationFn: (taskId: string) => claimTaskForCurrentUser(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-tasks'] })
      queryClient.invalidateQueries({ queryKey: ['group-tasks'] })
    },
  })

  // Unclaim task mutation
  const unclaimMutation = useMutation({
    mutationFn: (taskId: string) => unclaimTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-tasks'] })
      queryClient.invalidateQueries({ queryKey: ['group-tasks'] })
    },
  })

  // Manual refresh handler
  const handleRefresh = async () => {
    setRefreshing(true)
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['my-tasks'] }),
      queryClient.invalidateQueries({ queryKey: ['group-tasks'] }),
    ])
    setTimeout(() => setRefreshing(false), 500)
  }

  // Handle task completion
  const handleTaskComplete = () => {
    setShowFormDialog(false)
    setSelectedTask(null)
    // Invalidate queries to refresh task lists
    queryClient.invalidateQueries({ queryKey: ['my-tasks'] })
    queryClient.invalidateQueries({ queryKey: ['group-tasks'] })
  }

  // Handle claim and work on task
  const handleClaimAndWork = async (task: TaskInfo) => {
    try {
      await claimMutation.mutateAsync(task.taskId)
      setSelectedTask(task)
      setShowFormDialog(true)
    } catch (error) {
      console.error('Failed to claim task:', error)
    }
  }

  // Handle work on task (already claimed)
  const handleWorkOnTask = (task: TaskInfo) => {
    setSelectedTask(task)
    setShowFormDialog(true)
  }

  // Get priority color
  const getPriorityColor = (priority: number) => {
    if (priority >= 75) return 'text-red-600 bg-red-100'
    if (priority >= 50) return 'text-orange-600 bg-orange-100'
    if (priority >= 25) return 'text-yellow-600 bg-yellow-100'
    return 'text-green-600 bg-green-100'
  }

  // Format date
  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return 'Just now'
    if (diffMins < 60) return `${diffMins} min ago`
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`
    return date.toLocaleDateString()
  }

  // Check if task is overdue
  const isOverdue = (dueDate?: string) => {
    if (!dueDate) return false
    return new Date(dueDate) < new Date()
  }

  // Render task card
  const renderTaskCard = (task: TaskInfo, isGroupTask: boolean = false) => {
    const overdue = isOverdue(task.dueDate)

    return (
      <div
        key={task.taskId}
        className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow"
      >
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <h3 className="text-lg font-semibold text-gray-900">{task.taskName}</h3>
              <Badge variant="outline" className={getPriorityColor(task.priority)}>
                Priority: {task.priority}
              </Badge>
              {overdue && (
                <Badge variant="destructive" className="flex items-center gap-1">
                  <AlertCircle className="h-3 w-3" />
                  Overdue
                </Badge>
              )}
            </div>
            <p className="text-sm text-gray-600 mb-3">{task.processDefinitionName}</p>

            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="flex items-center gap-2 text-gray-600">
                <FileText className="h-4 w-4" />
                <span className="truncate">{task.businessKey || 'No business key'}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-600">
                <Clock className="h-4 w-4" />
                <span>Created {formatDate(task.createTime)}</span>
              </div>
              {task.dueDate && (
                <div className={`flex items-center gap-2 ${overdue ? 'text-red-600' : 'text-gray-600'}`}>
                  <Calendar className="h-4 w-4" />
                  <span>Due {new Date(task.dueDate).toLocaleDateString()}</span>
                </div>
              )}
              {isGroupTask && task.candidateGroups && task.candidateGroups.length > 0 && (
                <div className="flex items-center gap-2 text-gray-600">
                  <Users className="h-4 w-4" />
                  <span className="truncate">{task.candidateGroups.join(', ')}</span>
                </div>
              )}
              {!isGroupTask && task.assignee && (
                <div className="flex items-center gap-2 text-gray-600">
                  <User className="h-4 w-4" />
                  <span>Assigned to you</span>
                </div>
              )}
            </div>

            {task.description && (
              <p className="mt-3 text-sm text-gray-600 line-clamp-2">{task.description}</p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2 pt-4 border-t border-gray-200">
          {isGroupTask ? (
            <>
              <Button
                onClick={() => handleClaimAndWork(task)}
                disabled={claimMutation.isPending}
                className="flex-1"
              >
                {claimMutation.isPending ? (
                  <>
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                    Claiming...
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="h-4 w-4 mr-2" />
                    Claim & Work
                  </>
                )}
              </Button>
              <Link href={`/portal/processes/${task.processInstanceId}`}>
                <Button variant="outline">View Process</Button>
              </Link>
            </>
          ) : (
            <>
              <Button
                onClick={() => handleWorkOnTask(task)}
                className="flex-1"
              >
                <Activity className="h-4 w-4 mr-2" />
                Work on Task
              </Button>
              <Button
                variant="outline"
                onClick={() => unclaimMutation.mutate(task.taskId)}
                disabled={unclaimMutation.isPending}
              >
                {unclaimMutation.isPending ? 'Releasing...' : 'Release'}
              </Button>
              <Link href={`/portal/processes/${task.processInstanceId}`}>
                <Button variant="outline">View Process</Button>
              </Link>
            </>
          )}
        </div>
      </div>
    )
  }

  const currentTasks = activeTab === 'my-tasks' ? myTasks : groupTasks
  const isLoading = activeTab === 'my-tasks' ? myTasksLoading : groupTasksLoading
  const error = activeTab === 'my-tasks' ? myTasksError : groupTasksError

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Task Management</h1>
          <p className="mt-1 text-sm text-gray-600">
            View and complete your workflow tasks
          </p>
        </div>
        <Button
          onClick={handleRefresh}
          disabled={refreshing}
          variant="outline"
          className="flex items-center gap-2"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
          {refreshing ? 'Refreshing...' : 'Refresh'}
        </Button>
      </div>

      {/* Tabs */}
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="border-b border-gray-200">
          <nav className="flex -mb-px">
            <button
              onClick={() => setActiveTab('my-tasks')}
              className={`group inline-flex items-center px-6 py-4 border-b-2 font-medium text-sm ${
                activeTab === 'my-tasks'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <User className={`-ml-0.5 mr-2 h-5 w-5 ${activeTab === 'my-tasks' ? 'text-blue-600' : 'text-gray-400'}`} />
              <span>My Tasks</span>
              <Badge
                variant={activeTab === 'my-tasks' ? 'default' : 'secondary'}
                className="ml-2"
              >
                {myTasks?.length || 0}
              </Badge>
            </button>

            <button
              onClick={() => setActiveTab('group-tasks')}
              className={`group inline-flex items-center px-6 py-4 border-b-2 font-medium text-sm ${
                activeTab === 'group-tasks'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <Users className={`-ml-0.5 mr-2 h-5 w-5 ${activeTab === 'group-tasks' ? 'text-blue-600' : 'text-gray-400'}`} />
              <span>Group Tasks</span>
              <Badge
                variant={activeTab === 'group-tasks' ? 'default' : 'secondary'}
                className="ml-2"
              >
                {groupTasks?.length || 0}
              </Badge>
            </button>
          </nav>
        </div>

        {/* Task List */}
        <div className="p-6">
          {isLoading ? (
            <div className="text-center py-12">
              <RefreshCw className="h-8 w-8 animate-spin text-blue-600 mx-auto mb-4" />
              <p className="text-gray-600">Loading tasks...</p>
            </div>
          ) : error ? (
            <div className="text-center py-12">
              <XCircle className="h-12 w-12 text-red-600 mx-auto mb-4" />
              <p className="text-gray-900 font-medium mb-2">Failed to load tasks</p>
              <p className="text-gray-600 text-sm mb-4">{error?.toString() || 'Unknown error occurred'}</p>
              <Button onClick={handleRefresh}>Retry</Button>
            </div>
          ) : currentTasks && currentTasks.length > 0 ? (
            <div className="grid gap-4">
              {currentTasks.map((task) => renderTaskCard(task, activeTab === 'group-tasks'))}
            </div>
          ) : (
            <div className="text-center py-12 text-gray-500">
              <CheckCircle2 className="h-12 w-12 mx-auto mb-3 text-gray-400" />
              <p className="text-sm font-medium">No {activeTab === 'my-tasks' ? 'assigned' : 'available'} tasks</p>
              <p className="text-xs text-gray-400 mt-1">
                {activeTab === 'my-tasks'
                  ? 'You have no tasks assigned to you at the moment'
                  : 'No tasks available for your groups'}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Task Form Dialog */}
      <Dialog open={showFormDialog} onOpenChange={setShowFormDialog}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{selectedTask?.taskName}</DialogTitle>
            <DialogDescription>
              Complete the form below to finish this task
            </DialogDescription>
          </DialogHeader>
          {selectedTask?.formKey ? (
            <FormRenderer
              formKey={selectedTask.formKey}
              taskId={selectedTask.taskId}
              onComplete={handleTaskComplete}
              onCancel={() => setShowFormDialog(false)}
              initialData={selectedTask.processVariables}
            />
          ) : (
            <div className="py-12 text-center">
              <AlertCircle className="h-12 w-12 text-yellow-600 mx-auto mb-4" />
              <p className="text-gray-900 font-medium mb-2">No form defined for this task</p>
              <p className="text-gray-600 text-sm mb-4">
                This task doesn't have an associated form. Contact your administrator.
              </p>
              <Button variant="outline" onClick={() => setShowFormDialog(false)}>
                Close
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
