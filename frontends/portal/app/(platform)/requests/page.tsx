'use client'

import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import Link from 'next/link'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Skeleton } from '@/components/ui/skeleton'
import { Card, CardContent } from '@/components/ui/card'
import { getAllWorkflowInstances, type WorkflowInstance } from '@/lib/api/workflows'
import { formatDate } from '@/lib/utils/format'

type StatusFilter = 'all' | 'active' | 'completed' | 'suspended'

const STATUS_TABS: { value: StatusFilter; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'active', label: 'Active' },
  { value: 'completed', label: 'Completed' },
  { value: 'suspended', label: 'Suspended' },
]

function statusBadge(status: WorkflowInstance['status']) {
  switch (status) {
    case 'active':
      return <Badge className="bg-blue-100 text-blue-800 border-blue-200 hover:bg-blue-100">Active</Badge>
    case 'completed':
      return <Badge className="bg-green-100 text-green-800 border-green-200 hover:bg-green-100">Completed</Badge>
    case 'failed':
      return <Badge className="bg-red-100 text-red-800 border-red-200 hover:bg-red-100">Failed</Badge>
    case 'suspended':
      return <Badge className="bg-yellow-100 text-yellow-800 border-yellow-200 hover:bg-yellow-100">Suspended</Badge>
    default:
      return <Badge variant="outline">{status}</Badge>
  }
}

function TableSkeleton() {
  return (
    <div className="space-y-2">
      {Array.from({ length: 5 }).map((_, i) => (
        <Skeleton key={i} className="h-12 w-full rounded" />
      ))}
    </div>
  )
}

export default function RequestsPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all')
  const [search, setSearch] = useState('')

  const queryStatus = statusFilter === 'all' ? undefined : statusFilter

  // TODO: Backend lacks a startedBy/initiator filter on /workflows/instances.
  // Currently fetches all workflow instances. Scope to current user once
  // the Engine service adds an initiator query parameter.
  const { data: instances, isLoading } = useQuery({
    queryKey: ['workflow-instances', statusFilter],
    queryFn: () => getAllWorkflowInstances(queryStatus, 100),
  })

  const filtered = (instances ?? []).filter((instance) => {
    if (!search) return true
    const term = search.toLowerCase()
    const key = (instance.businessKey ?? '').toLowerCase()
    const name = (instance.processDefinitionName ?? instance.processDefinitionKey ?? '').toLowerCase()
    return key.includes(term) || name.includes(term) || instance.id.toLowerCase().includes(term)
  })

  return (
    <div className="container py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">My Requests</h1>
        <p className="text-muted-foreground">Track the status of workflow requests across the organization</p>
      </div>

      <div className="flex flex-col sm:flex-row sm:items-center gap-4 mb-6">
        <Tabs
          value={statusFilter}
          onValueChange={(v) => setStatusFilter(v as StatusFilter)}
        >
          <TabsList>
            {STATUS_TABS.map((tab) => (
              <TabsTrigger key={tab.value} value={tab.value}>
                {tab.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>

        <Input
          placeholder="Search by business key or process name..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="sm:max-w-xs"
        />
      </div>

      {isLoading ? (
        <TableSkeleton />
      ) : filtered.length === 0 ? (
        <Card>
          <CardContent className="py-16 text-center">
            <p className="text-muted-foreground">No requests found.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Process Name</TableHead>
                <TableHead>Business Key</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Started</TableHead>
                <TableHead>Current Activity</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((instance) => (
                <TableRow key={instance.id}>
                  <TableCell className="font-medium">
                    {instance.processDefinitionName || instance.processDefinitionKey}
                  </TableCell>
                  <TableCell>
                    {instance.businessKey ? (
                      <Link
                        href={`/requests/${instance.id}`}
                        className="text-primary underline-offset-4 hover:underline font-mono text-sm"
                      >
                        {instance.businessKey}
                      </Link>
                    ) : (
                      <Link
                        href={`/requests/${instance.id}`}
                        className="text-muted-foreground underline-offset-4 hover:underline font-mono text-sm"
                      >
                        {instance.id.substring(0, 8)}...
                      </Link>
                    )}
                  </TableCell>
                  <TableCell>{statusBadge(instance.status)}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(instance.startTime)}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {instance.currentActivity ?? '-'}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
