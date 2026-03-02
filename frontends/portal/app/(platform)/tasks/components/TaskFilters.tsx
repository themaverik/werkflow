'use client'

import { useState } from 'react'
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { X, Filter } from "lucide-react"
import type { TaskFilter } from "@/lib/types/task"

interface TaskFiltersProps {
  filters: TaskFilter
  onFilterChange: (filters: TaskFilter) => void
  userDepartment?: string
}

export function TaskFilters({ filters, onFilterChange, userDepartment }: TaskFiltersProps) {
  const [localFilters, setLocalFilters] = useState<TaskFilter>(filters)

  const handleFilterChange = (key: keyof TaskFilter, value: any) => {
    const newFilters = { ...localFilters, [key]: value }
    setLocalFilters(newFilters)
    onFilterChange(newFilters)
  }

  const clearFilters = () => {
    const defaultFilters: TaskFilter = {}
    setLocalFilters(defaultFilters)
    onFilterChange(defaultFilters)
  }

  const hasActiveFilters = Object.keys(localFilters).some(key => {
    const value = localFilters[key as keyof TaskFilter]
    return value !== undefined && value !== null && value !== ''
  })

  const activeFilterCount = Object.keys(localFilters).filter(key => {
    const value = localFilters[key as keyof TaskFilter]
    return value !== undefined && value !== null && value !== ''
  }).length

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base flex items-center gap-2">
            <Filter className="h-4 w-4" />
            Filters
            {activeFilterCount > 0 && (
              <Badge variant="secondary" className="ml-2">
                {activeFilterCount}
              </Badge>
            )}
          </CardTitle>
          {hasActiveFilters && (
            <Button
              variant="ghost"
              size="sm"
              onClick={clearFilters}
              className="h-8 px-2"
            >
              <X className="h-4 w-4 mr-1" />
              Clear
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-3">
          <div>
            <Label className="text-sm font-medium mb-2 block">Task Assignment</Label>
            <div className="space-y-2">
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="radio"
                  name="assignment"
                  checked={localFilters.myTasks === true}
                  onChange={() => handleFilterChange('myTasks', true)}
                  className="w-4 h-4"
                />
                <span className="text-sm">My Tasks</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="radio"
                  name="assignment"
                  checked={localFilters.teamTasks === true}
                  onChange={() => handleFilterChange('teamTasks', true)}
                  className="w-4 h-4"
                />
                <span className="text-sm">Team Tasks</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="radio"
                  name="assignment"
                  checked={localFilters.unassigned === true}
                  onChange={() => handleFilterChange('unassigned', true)}
                  className="w-4 h-4"
                />
                <span className="text-sm">Unassigned</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="radio"
                  name="assignment"
                  checked={!localFilters.myTasks && !localFilters.teamTasks && !localFilters.unassigned}
                  onChange={() => {
                    handleFilterChange('myTasks', undefined)
                    handleFilterChange('teamTasks', undefined)
                    handleFilterChange('unassigned', undefined)
                  }}
                  className="w-4 h-4"
                />
                <span className="text-sm">All Tasks</span>
              </label>
            </div>
          </div>

          <div className="border-t pt-3">
            <Label className="text-sm font-medium mb-2 block">Priority</Label>
            <div className="space-y-2">
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={localFilters.priority === 100}
                  onChange={(e) => handleFilterChange('priority', e.target.checked ? 100 : undefined)}
                  className="w-4 h-4"
                />
                <span className="text-sm">Urgent (100)</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={localFilters.priority === 75}
                  onChange={(e) => handleFilterChange('priority', e.target.checked ? 75 : undefined)}
                  className="w-4 h-4"
                />
                <span className="text-sm">High (75)</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={localFilters.priority === 50}
                  onChange={(e) => handleFilterChange('priority', e.target.checked ? 50 : undefined)}
                  className="w-4 h-4"
                />
                <span className="text-sm">Medium (50)</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={localFilters.priority === 25}
                  onChange={(e) => handleFilterChange('priority', e.target.checked ? 25 : undefined)}
                  className="w-4 h-4"
                />
                <span className="text-sm">Low (25)</span>
              </label>
            </div>
          </div>

          {userDepartment && (
            <div className="border-t pt-3">
              <Label className="text-sm font-medium mb-2 block">Department</Label>
              <div className="space-y-2">
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="radio"
                    name="department"
                    checked={localFilters.department === userDepartment}
                    onChange={() => handleFilterChange('department', userDepartment)}
                    className="w-4 h-4"
                  />
                  <span className="text-sm">My Department ({userDepartment})</span>
                </label>
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="radio"
                    name="department"
                    checked={!localFilters.department}
                    onChange={() => handleFilterChange('department', undefined)}
                    className="w-4 h-4"
                  />
                  <span className="text-sm">All Departments</span>
                </label>
              </div>
            </div>
          )}

          <div className="border-t pt-3">
            <Label className="text-sm font-medium mb-2 block">Due Date</Label>
            <div className="space-y-2">
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={!!localFilters.dueAfter}
                  onChange={(e) => {
                    const today = new Date().toISOString()
                    handleFilterChange('dueAfter', e.target.checked ? today : undefined)
                  }}
                  className="w-4 h-4"
                />
                <span className="text-sm">Overdue</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={!!localFilters.dueBefore}
                  onChange={(e) => {
                    const tomorrow = new Date()
                    tomorrow.setDate(tomorrow.getDate() + 1)
                    handleFilterChange('dueBefore', e.target.checked ? tomorrow.toISOString() : undefined)
                  }}
                  className="w-4 h-4"
                />
                <span className="text-sm">Due Today</span>
              </label>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
