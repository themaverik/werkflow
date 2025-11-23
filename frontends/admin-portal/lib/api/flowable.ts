import { apiClient } from './client'

export interface BpmnDeploymentRequest {
  name: string
  resourceName: string
  bpmnXml: string
}

export interface DeploymentResponse {
  id: string
  name: string
}

export interface ProcessDefinitionResponse {
  id: string
  key: string
  name: string
  version: number
  deploymentId: string
  resourceName: string
}

export interface FormDeploymentRequest {
  key: string
  name: string
  formJson: string
}

export interface FormDefinitionResponse {
  key: string
  name: string
  formJson: string
}

// Deploy BPMN process
export async function deployBpmn(data: BpmnDeploymentRequest): Promise<DeploymentResponse> {
  const response = await apiClient.post('/deployments', data)
  return response.data
}

// Get all process definitions
export async function getProcessDefinitions(): Promise<ProcessDefinitionResponse[]> {
  const response = await apiClient.get('/process-definitions')
  return response.data
}

// Get BPMN XML for a process definition
export async function getProcessDefinitionXml(processDefinitionId: string): Promise<string> {
  const response = await apiClient.get(`/process-definitions/${processDefinitionId}/xml`)
  return response.data
}

// Delete a deployment (includes all process definitions within it)
export async function deleteDeployment(deploymentId: string): Promise<void> {
  await apiClient.delete(`/deployments/${deploymentId}`)
}

// Deploy form definition
export async function deployForm(data: FormDeploymentRequest): Promise<DeploymentResponse> {
  const response = await apiClient.post('/forms', data)
  return response.data
}

// Get all form definitions
export async function getFormDefinitions(): Promise<FormDefinitionResponse[]> {
  const response = await apiClient.get('/forms')
  return response.data
}

// Get form definition by key
export async function getFormDefinition(formKey: string): Promise<FormDefinitionResponse> {
  const response = await apiClient.get(`/forms/${formKey}`)
  return response.data
}

// Delete a form definition
export async function deleteFormDefinition(formKey: string): Promise<void> {
  await apiClient.delete(`/forms/${formKey}`)
}

// Get form data for a task
export async function getTaskFormData(taskId: string): Promise<any> {
  const response = await apiClient.get(`/tasks/${taskId}/form`)
  return response.data
}
