import { apiClient } from './client'

export interface StartProcessRequest {
  processDefinitionKey: string
  businessKey?: string
  variables?: Record<string, any>
}

export interface ProcessInstanceResponse {
  processInstanceId: string
  processDefinitionId: string
  processDefinitionKey: string
  businessKey?: string
  suspended: boolean
  ended: boolean
  startTime?: string
  endTime?: string
  variables?: Record<string, any>
}

export interface TaskResponse {
  taskId: string
  taskName: string
  taskDefinitionKey: string
  processInstanceId: string
  processDefinitionId: string
  assignee?: string
  owner?: string
  createTime: string
  dueDate?: string
  priority: number
  suspended: boolean
  description?: string
  variables?: Record<string, any>
}

export interface CompleteTaskRequest {
  taskId: string
  variables?: Record<string, any>
  comment?: string
}

// Start a new workflow process
export async function startProcess(data: StartProcessRequest): Promise<ProcessInstanceResponse> {
  const response = await apiClient.post('/workflows/processes/start', data)
  return response.data
}

// Get process instance by ID
export async function getProcessInstance(processInstanceId: string): Promise<ProcessInstanceResponse> {
  const response = await apiClient.get(`/workflows/processes/${processInstanceId}`)
  return response.data
}

// Get all process instances for a process definition
export async function getProcessInstances(processDefinitionKey: string): Promise<ProcessInstanceResponse[]> {
  const response = await apiClient.get(`/workflows/processes`, {
    params: { processDefinitionKey }
  })
  return response.data
}

// Get tasks by assignee
export async function getTasksByAssignee(assignee: string): Promise<TaskResponse[]> {
  const response = await apiClient.get(`/workflows/tasks/assignee/${assignee}`)
  return response.data
}

// Get tasks by group
export async function getTasksByGroup(group: string): Promise<TaskResponse[]> {
  const response = await apiClient.get(`/workflows/tasks/group/${group}`)
  return response.data
}

// Get tasks for a process instance
export async function getTasksByProcessInstance(processInstanceId: string): Promise<TaskResponse[]> {
  const response = await apiClient.get(`/workflows/tasks/process/${processInstanceId}`)
  return response.data
}

// Get task by ID
export async function getTask(taskId: string): Promise<TaskResponse> {
  const response = await apiClient.get(`/workflows/tasks/${taskId}`)
  return response.data
}

// Complete a task
export async function completeTask(data: CompleteTaskRequest): Promise<void> {
  await apiClient.post('/workflows/tasks/complete', data)
}

// Claim a task
export async function claimTask(taskId: string, userId: string): Promise<void> {
  await apiClient.post(`/workflows/tasks/${taskId}/claim`, null, {
    params: { userId }
  })
}

// Delete a process instance
export async function deleteProcessInstance(processInstanceId: string, deleteReason?: string): Promise<void> {
  await apiClient.delete(`/workflows/processes/${processInstanceId}`, {
    params: { deleteReason }
  })
}

// Get process variables
export async function getProcessVariables(processInstanceId: string): Promise<Record<string, any>> {
  const response = await apiClient.get(`/workflows/processes/${processInstanceId}/variables`)
  return response.data
}

// Set process variables
export async function setProcessVariables(processInstanceId: string, variables: Record<string, any>): Promise<void> {
  await apiClient.put(`/workflows/processes/${processInstanceId}/variables`, variables)
}
