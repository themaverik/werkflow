'use client'

import { useState, useEffect } from 'react'
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Search, Bell, RefreshCw } from "lucide-react"
import { TaskList } from './components/TaskList'
import { TaskFilters } from './components/TaskFilters'
import { useTasks, useClaimTask, useTaskSummary } from '@/lib/hooks/useTasks'
import { getTokenFromStorage, extractUserClaims, mapGroupsToCandidateGroups } from '@/lib/utils/jwt'
import { useToast } from "@/hooks/use-toast"
import type { TaskFilter, UserClaims } from '@/lib/types/task'

export default function TasksPage() {
  const { toast } = useToast()
  const [page, setPage] = useState(0)
  const [pageSize] = useState(20)
  const [searchText, setSearchText] = useState('')
  const [filters, setFilters] = useState<TaskFilter>({})
  const [userClaims, setUserClaims] = useState<UserClaims | null>(null)
  const [claimingTaskId, setClaimingTaskId] = useState<string | undefined>(undefined)

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
        toast({
          title: 'Authentication Error',
          description: 'Failed to load user information. Please log in again.',
          variant: 'destructive',
        })
      }
    }

    loadUserClaims()
  }, [toast])

  const buildQueryParams = () => {
    const params: any = {
      start: page * pageSize,
      size: pageSize,
      sort: 'createTime',
      order: 'desc' as const,
      includeProcessVariables: false,
    }

    if (userClaims) {
      if (filters.myTasks) {
        params.assignee = userClaims.sub
      } else if (filters.teamTasks) {
        const candidateGroups = mapGroupsToCandidateGroups(userClaims.groups || [])
        if (candidateGroups.length > 0) {
          params.candidateGroups = candidateGroups.join(',')
        }
      } else if (filters.unassigned) {
        params.unassigned = true
      } else {
        params.candidateUser = userClaims.sub
        const candidateGroups = mapGroupsToCandidateGroups(userClaims.groups || [])
        if (candidateGroups.length > 0) {
          params.candidateGroups = candidateGroups.join(',')
        }
      }
    }

    if (searchText && searchText.length > 2) {
      params.nameLike = `%${searchText}%`
    }

    if (filters.priority !== undefined) {
      params.priority = filters.priority
    }

    if (filters.processDefinitionKey) {
      params.processDefinitionKey = filters.processDefinitionKey
    }

    if (filters.dueBefore) {
      params.dueBefore = filters.dueBefore
    }

    if (filters.dueAfter) {
      params.dueAfter = filters.dueAfter
    }

    return params
  }

  const { data: tasksData, isLoading: isLoadingTasks, refetch } = useTasks(buildQueryParams())
  const { data: summary, isLoading: isLoadingSummary } = useTaskSummary()
  const claimTaskMutation = useClaimTask()

  const handleClaim = async (taskId: string) => {
    if (!userClaims) {
      toast({
        title: 'Authentication Required',
        description: 'Please log in to claim tasks.',
        variant: 'destructive',
      })
      return
    }

    setClaimingTaskId(taskId)

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
    } finally {
      setClaimingTaskId(undefined)
    }
  }

  const handleFilterChange = (newFilters: TaskFilter) => {
    setFilters(newFilters)
    setPage(0)
  }

  const handleSearch = () => {
    setPage(0)
    refetch()
  }

  const handleRefresh = () => {
    refetch()
  }

  return (
    <div className="container py-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Task Management</h1>
          <p className="text-muted-foreground">
            View and manage your workflow tasks
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="icon" onClick={handleRefresh} disabled={isLoadingTasks}>
            <RefreshCw className={`h-4 w-4 ${isLoadingTasks ? 'animate-spin' : ''}`} />
          </Button>
          <Button variant="outline" size="icon">
            <Bell className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {summary && !isLoadingSummary && (
        <div className="grid gap-4 md:grid-cols-4 mb-6">
          <Card>
            <CardContent className="pt-6">
              <div className="text-2xl font-bold">{summary.myTasks}</div>
              <p className="text-xs text-muted-foreground">My Tasks</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="text-2xl font-bold">{summary.teamTasks}</div>
              <p className="text-xs text-muted-foreground">Team Tasks</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="text-2xl font-bold">{summary.overdue}</div>
              <p className="text-xs text-muted-foreground">Overdue</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="text-2xl font-bold">{summary.highPriority}</div>
              <p className="text-xs text-muted-foreground">High Priority</p>
            </CardContent>
          </Card>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-1">
          <TaskFilters
            filters={filters}
            onFilterChange={handleFilterChange}
            userDepartment={userClaims?.department}
          />
        </div>

        <div className="lg:col-span-3 space-y-4">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search tasks by name..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleSearch()
                  }
                }}
                className="pl-9"
              />
            </div>
            <Button onClick={handleSearch} disabled={isLoadingTasks}>
              Search
            </Button>
          </div>

          <TaskList
            tasks={tasksData?.data || []}
            total={tasksData?.total || 0}
            page={page}
            pageSize={pageSize}
            onPageChange={setPage}
            onClaim={handleClaim}
            isLoading={isLoadingTasks}
            claimingTaskId={claimingTaskId}
          />
        </div>
      </div>
    </div>
  )
}
