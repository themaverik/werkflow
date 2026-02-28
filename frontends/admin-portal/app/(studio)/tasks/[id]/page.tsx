'use client'

import { useParams, useRouter } from 'next/navigation'
import { useState, useEffect } from 'react'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ArrowLeft, CheckCircle2, XCircle, UserPlus, Clock, User, Calendar } from "lucide-react"
import Link from "next/link"
import { useTask, useTaskFormData, useTaskHistory, useCompleteTask, useClaimTask, useUnclaimTask, useDelegateTask } from '@/lib/hooks/useTasks'
import { getTokenFromStorage, extractUserClaims } from '@/lib/utils/jwt'
import { useToast } from "@/hooks/use-toast"
import { ApprovalPanel } from '../components/ApprovalPanel'
import { DelegationModal } from '../components/DelegationModal'
import { FormSection } from '../components/FormSection'
import { ProcessTimeline } from '../components/ProcessTimeline'
import type { UserClaims } from '@/lib/types/task'

export default function TaskDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { toast } = useToast()
  const taskId = params.id as string

  const [userClaims, setUserClaims] = useState<UserClaims | null>(null)
  const [showDelegationModal, setShowDelegationModal] = useState(false)

  const { data: task, isLoading: isLoadingTask } = useTask(taskId)
  const { data: formData, isLoading: isLoadingForm } = useTaskFormData(taskId)
  const { data: history, isLoading: isLoadingHistory } = useTaskHistory(taskId)

  const completeTaskMutation = useCompleteTask()
  const claimTaskMutation = useClaimTask()
  const unclaimTaskMutation = useUnclaimTask()
  const delegateTaskMutation = useDelegateTask()

  useEffect(() => {
    async function loadUserClaims() {
      try {
        const token = await getTokenFromStorage()
        if (token) {
          const claims = extractUserClaims(token)
          setUserClaims(claims)
        }
      } catch (error) {
        console.error('Failed to load user claims:', error)
      }
    }

    loadUserClaims()
  }, [])

  const handleClaim = async () => {
    if (!userClaims) {
      toast({
        title: 'Authentication Required',
        description: 'Please log in to claim tasks.',
        variant: 'destructive',
      })
      return
    }

    try {
      await claimTaskMutation.mutateAsync({
        taskId,
        assignee: userClaims.sub,
      })

      toast({
        title: 'Task Claimed',
        description: 'You have successfully claimed this task.',
      })
    } catch (error: any) {
      toast({
        title: 'Failed to Claim Task',
        description: error.message || 'An error occurred while claiming the task.',
        variant: 'destructive',
      })
    }
  }

  const handleComplete = async () => {
    try {
      await completeTaskMutation.mutateAsync({
        taskId,
        data: {},
      })

      toast({
        title: 'Task Completed',
        description: 'The task has been completed successfully.',
      })

      router.push('/tasks')
    } catch (error: any) {
      toast({
        title: 'Failed to Complete Task',
        description: error.message || 'An error occurred while completing the task.',
        variant: 'destructive',
      })
    }
  }

  const handleApprove = async (comment: string) => {
    try {
      await completeTaskMutation.mutateAsync({
        taskId,
        data: {
          variables: {
            approved: true,
            approvalComment: comment,
            approvedBy: userClaims?.sub,
            approvalTimestamp: new Date().toISOString(),
          },
        },
      })

      toast({
        title: 'Request Approved',
        description: 'The request has been approved successfully.',
      })

      router.push('/tasks')
    } catch (error: any) {
      toast({
        title: 'Approval Failed',
        description: error.message || 'An error occurred while approving the request.',
        variant: 'destructive',
      })
    }
  }

  const handleReject = async (comment: string) => {
    try {
      await completeTaskMutation.mutateAsync({
        taskId,
        data: {
          variables: {
            approved: false,
            rejectionReason: comment,
            rejectedBy: userClaims?.sub,
            rejectionTimestamp: new Date().toISOString(),
          },
        },
      })

      toast({
        title: 'Request Rejected',
        description: 'The request has been rejected.',
      })

      router.push('/tasks')
    } catch (error: any) {
      toast({
        title: 'Rejection Failed',
        description: error.message || 'An error occurred while rejecting the request.',
        variant: 'destructive',
      })
    }
  }

  const handleEscalate = async (reason: string) => {
    try {
      await completeTaskMutation.mutateAsync({
        taskId,
        data: {
          variables: {
            escalated: true,
            escalationReason: reason,
            escalatedBy: userClaims?.sub,
            escalationTimestamp: new Date().toISOString(),
          },
        },
      })

      toast({
        title: 'Request Escalated',
        description: 'The request has been escalated to higher authority.',
      })

      router.push('/tasks')
    } catch (error: any) {
      toast({
        title: 'Escalation Failed',
        description: error.message || 'An error occurred while escalating the request.',
        variant: 'destructive',
      })
    }
  }

  const handleDelegateTask = async (assignee: string, reason: string) => {
    try {
      await delegateTaskMutation.mutateAsync({
        taskId,
        data: {
          assignee,
          reason,
        },
      })

      toast({
        title: 'Task Delegated',
        description: 'The task has been delegated successfully.',
      })

      setShowDelegationModal(false)
      router.push('/tasks')
    } catch (error: any) {
      toast({
        title: 'Delegation Failed',
        description: error.message || 'An error occurred while delegating the task.',
        variant: 'destructive',
      })
    }
  }

  const handleUnclaim = async () => {
    try {
      await unclaimTaskMutation.mutateAsync(taskId)
      toast({
        title: 'Task Unclaimed',
        description: 'Task has been returned to the queue.',
      })
    } catch (error: any) {
      toast({
        title: 'Failed to Unclaim',
        description: error.message || 'An error occurred.',
        variant: 'destructive',
      })
    }
  }

  const handleFormSubmit = async (data: Record<string, any>) => {
    try {
      await completeTaskMutation.mutateAsync({
        taskId,
        data: { variables: data },
      })
      toast({ title: 'Task Completed', description: 'Form submitted successfully.' })
      router.push('/tasks')
    } catch (error: any) {
      toast({
        title: 'Submission Failed',
        description: error.message || 'Failed to submit form.',
        variant: 'destructive',
      })
    }
  }

  if (isLoadingTask) {
    return (
      <div className="container py-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-muted rounded w-1/3" />
          <div className="h-64 bg-muted rounded" />
        </div>
      </div>
    )
  }

  if (!task) {
    return (
      <div className="container py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <h3 className="text-lg font-semibold mb-2">Task Not Found</h3>
            <p className="text-muted-foreground mb-4">
              The task you are looking for does not exist or has been deleted.
            </p>
            <Button asChild>
              <Link href="/tasks">
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Tasks
              </Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  const isAssignedToUser = task.assignee === userClaims?.sub
  const canClaim = !task.assignee && userClaims
  const canComplete = isAssignedToUser

  const priorityColor = task.priority
    ? task.priority >= 75
      ? 'destructive'
      : task.priority >= 50
      ? 'default'
      : 'secondary'
    : 'secondary'

  const priorityLabel = task.priority
    ? task.priority >= 75
      ? 'Urgent'
      : task.priority >= 50
      ? 'High'
      : task.priority >= 25
      ? 'Medium'
      : 'Low'
    : 'Normal'

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString()
  }

  const isApprovalTask = () => {
    if (!task) return false

    const taskNameLower = (task.name || '').toLowerCase()
    const taskDefKeyLower = (task.taskDefinitionKey || '').toLowerCase()

    return (
      taskNameLower.includes('approval') ||
      taskNameLower.includes('approve') ||
      taskDefKeyLower.includes('approval') ||
      taskDefKeyLower.includes('approve') ||
      task.processVariables?.approvalLevel !== undefined ||
      task.processVariables?.requestAmount !== undefined ||
      task.processVariables?.amount !== undefined
    )
  }

  return (
    <div className="container py-6">
      <div className="mb-6">
        <Button variant="ghost" asChild className="mb-4">
          <Link href="/tasks">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Tasks
          </Link>
        </Button>

        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold mb-2">{task.name}</h1>
            <p className="text-muted-foreground">
              {task.processDefinitionName || task.processDefinitionKey}
            </p>
          </div>
          <Badge variant={priorityColor}>{priorityLabel}</Badge>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          {isApprovalTask() && userClaims ? (
            <ApprovalPanel
              task={task}
              userClaims={userClaims}
              onApprove={handleApprove}
              onReject={handleReject}
              onEscalate={handleEscalate}
              isSubmitting={completeTaskMutation.isPending}
            />
          ) : (
            <Tabs defaultValue="details" className="w-full">
            <TabsList>
              <TabsTrigger value="details">Details</TabsTrigger>
              <TabsTrigger value="form">Form</TabsTrigger>
              <TabsTrigger value="history">History</TabsTrigger>
            </TabsList>

            <TabsContent value="details" className="mt-4">
              <Card>
                <CardHeader>
                  <CardTitle>Task Information</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  {task.description && (
                    <div>
                      <h4 className="font-semibold mb-2">Description</h4>
                      <p className="text-sm text-muted-foreground">{task.description}</p>
                    </div>
                  )}

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <h4 className="font-semibold mb-2 flex items-center gap-2">
                        <User className="h-4 w-4" />
                        Assignee
                      </h4>
                      <p className="text-sm text-muted-foreground">
                        {task.assignee || 'Unassigned'}
                      </p>
                    </div>

                    <div>
                      <h4 className="font-semibold mb-2 flex items-center gap-2">
                        <Clock className="h-4 w-4" />
                        Created
                      </h4>
                      <p className="text-sm text-muted-foreground">
                        {formatDate(task.createTime)}
                      </p>
                    </div>

                    {task.dueDate && (
                      <div>
                        <h4 className="font-semibold mb-2 flex items-center gap-2">
                          <Calendar className="h-4 w-4" />
                          Due Date
                        </h4>
                        <p className="text-sm text-muted-foreground">
                          {formatDate(task.dueDate)}
                        </p>
                      </div>
                    )}
                  </div>

                  {task.processVariables && Object.keys(task.processVariables).length > 0 && (
                    <div>
                      <h4 className="font-semibold mb-2">Process Variables</h4>
                      <div className="bg-muted p-4 rounded-lg">
                        <pre className="text-xs overflow-auto">
                          {JSON.stringify(task.processVariables, null, 2)}
                        </pre>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="form" className="mt-4">
              <FormSection
                formData={formData}
                isLoading={isLoadingForm}
                onSubmit={handleFormSubmit}
                isSubmitting={completeTaskMutation.isPending}
                readonly={!canComplete}
              />
            </TabsContent>

            <TabsContent value="history" className="mt-4">
              <ProcessTimeline
                processInstanceId={task.processInstanceId}
                taskHistory={history}
                isLoading={isLoadingHistory}
              />
            </TabsContent>
          </Tabs>
          )}
        </div>

        <div className="lg:col-span-1">
          {!isApprovalTask() && (
            <Card>
              <CardHeader>
                <CardTitle>Actions</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {canClaim && (
                  <Button
                    onClick={handleClaim}
                    className="w-full"
                    disabled={claimTaskMutation.isPending}
                  >
                    <CheckCircle2 className="h-4 w-4 mr-2" />
                    {claimTaskMutation.isPending ? 'Claiming...' : 'Claim Task'}
                  </Button>
                )}

                {canComplete && (
                  <>
                    <Button
                      onClick={handleComplete}
                      className="w-full"
                      disabled={completeTaskMutation.isPending}
                    >
                      <CheckCircle2 className="h-4 w-4 mr-2" />
                      {completeTaskMutation.isPending ? 'Completing...' : 'Complete Task'}
                    </Button>

                    <Button
                      variant="outline"
                      className="w-full"
                      onClick={handleUnclaim}
                      disabled={unclaimTaskMutation.isPending}
                    >
                      <XCircle className="h-4 w-4 mr-2" />
                      {unclaimTaskMutation.isPending ? 'Unclaiming...' : 'Unclaim Task'}
                    </Button>

                    <Button
                      variant="outline"
                      className="w-full"
                      onClick={() => setShowDelegationModal(true)}
                    >
                      <UserPlus className="h-4 w-4 mr-2" />
                      Delegate Task
                    </Button>
                  </>
                )}

                {!canClaim && !canComplete && (
                  <div className="text-center py-4 text-sm text-muted-foreground">
                    {task.assignee && task.assignee !== userClaims?.sub
                      ? 'This task is assigned to another user'
                      : 'You cannot perform actions on this task'}
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {isApprovalTask() && canClaim && (
            <Card>
              <CardHeader>
                <CardTitle>Quick Actions</CardTitle>
              </CardHeader>
              <CardContent>
                <Button
                  onClick={handleClaim}
                  className="w-full"
                  disabled={claimTaskMutation.isPending}
                >
                  <CheckCircle2 className="h-4 w-4 mr-2" />
                  {claimTaskMutation.isPending ? 'Claiming...' : 'Claim Task'}
                </Button>
              </CardContent>
            </Card>
          )}

          <Card className="mt-4">
            <CardHeader>
              <CardTitle className="text-base">Task Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Task ID</span>
                <span className="font-mono text-xs">{task.id.substring(0, 8)}...</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Process Instance</span>
                <span className="font-mono text-xs">
                  {task.processInstanceId?.substring(0, 8)}...
                </span>
              </div>
              {task.taskDefinitionKey && (
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Definition Key</span>
                  <span className="font-mono text-xs">{task.taskDefinitionKey}</span>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      <DelegationModal
        isOpen={showDelegationModal}
        onClose={() => setShowDelegationModal(false)}
        onDelegate={handleDelegateTask}
        isSubmitting={delegateTaskMutation.isPending}
      />
    </div>
  )
}
