import { apiClient } from './client'

/**
 * Service Registry API Client
 *
 * Manages service discovery, URL configuration, and endpoint documentation
 * for cross-service workflow integration.
 */

export interface Service {
  id: string
  name: string
  displayName: string
  description: string
  baseUrl: string
  endpoints: ServiceEndpoint[]
  environment: 'development' | 'staging' | 'production'
  status: 'active' | 'inactive' | 'maintenance'
  lastChecked?: Date
  responseTime?: number
  version?: string
  tags?: string[]
}

export interface ServiceEndpoint {
  path: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  description: string
  requestSchema?: Record<string, any>
  responseSchema?: Record<string, any>
  parameters?: ServiceParameter[]
  exampleRequest?: string
  exampleResponse?: string
}

export interface ServiceParameter {
  name: string
  type: string
  required: boolean
  description: string
  defaultValue?: any
}

export interface ServiceConnectivityTestResult {
  online: boolean
  responseTime: number
  error?: string
  timestamp: Date
}

export interface CreateServiceRequest {
  name: string
  displayName: string
  description: string
  baseUrl: string
  environment?: string
  tags?: string[]
}

export interface UpdateServiceRequest {
  displayName?: string
  description?: string
  baseUrl?: string
  environment?: string
  status?: string
  tags?: string[]
}

/**
 * Get all registered services
 */
export async function getServices(): Promise<Service[]> {
  try {
    const response = await apiClient.get('/services')
    return response.data.map((service: any) => ({
      ...service,
      lastChecked: service.lastChecked ? new Date(service.lastChecked) : undefined
    }))
  } catch (error) {
    console.error('Error fetching services:', error)
    // Return mock data for development
    return getMockServices()
  }
}

/**
 * Get a specific service by name or ID
 */
export async function getServiceByName(nameOrId: string): Promise<Service> {
  try {
    const response = await apiClient.get(`/services/${nameOrId}`)
    return {
      ...response.data,
      lastChecked: response.data.lastChecked ? new Date(response.data.lastChecked) : undefined
    }
  } catch (error) {
    console.error(`Error fetching service ${nameOrId}:`, error)
    // Return mock data for development
    const mockServices = getMockServices()
    const service = mockServices.find(s => s.name === nameOrId || s.id === nameOrId)
    if (service) return service
    throw new Error(`Service ${nameOrId} not found`)
  }
}

/**
 * Create a new service registration
 */
export async function createService(data: CreateServiceRequest): Promise<Service> {
  const response = await apiClient.post('/services', data)
  return response.data
}

/**
 * Update service configuration
 */
export async function updateService(serviceId: string, data: UpdateServiceRequest): Promise<Service> {
  const response = await apiClient.put(`/services/${serviceId}`, data)
  return response.data
}

/**
 * Update service base URL
 */
export async function updateServiceUrl(serviceId: string, baseUrl: string, environment?: string): Promise<Service> {
  const response = await apiClient.patch(`/services/${serviceId}/url`, {
    baseUrl,
    environment: environment || 'development'
  })
  return response.data
}

/**
 * Delete a service registration
 */
export async function deleteService(serviceId: string): Promise<void> {
  await apiClient.delete(`/services/${serviceId}`)
}

/**
 * Test service connectivity
 */
export async function testServiceConnectivity(serviceUrl: string): Promise<ServiceConnectivityTestResult> {
  try {
    const startTime = Date.now()
    const response = await apiClient.post('/services/test-connectivity', {
      url: serviceUrl
    })
    const responseTime = Date.now() - startTime

    return {
      online: response.data.online || true,
      responseTime,
      timestamp: new Date()
    }
  } catch (error: any) {
    return {
      online: false,
      responseTime: 0,
      error: error.message || 'Connection failed',
      timestamp: new Date()
    }
  }
}

/**
 * Get service endpoints
 */
export async function getServiceEndpoints(serviceName: string): Promise<ServiceEndpoint[]> {
  try {
    const response = await apiClient.get(`/services/${serviceName}/endpoints`)
    return response.data
  } catch (error) {
    console.error(`Error fetching endpoints for ${serviceName}:`, error)
    // Return mock data for development
    const service = await getServiceByName(serviceName)
    return service.endpoints
  }
}

/**
 * Get service health status
 */
export async function getServiceHealth(serviceName: string): Promise<ServiceConnectivityTestResult> {
  try {
    const response = await apiClient.get(`/services/${serviceName}/health`)
    return {
      ...response.data,
      timestamp: new Date(response.data.timestamp)
    }
  } catch (error) {
    return {
      online: false,
      responseTime: 0,
      error: 'Health check failed',
      timestamp: new Date()
    }
  }
}

/**
 * Mock services for development
 * This will be replaced with real API calls once backend is ready
 */
function getMockServices(): Service[] {
  return [
    {
      id: 'finance-service',
      name: 'finance',
      displayName: 'Finance Service',
      description: 'Budget management and financial operations',
      baseUrl: 'http://finance-service:8084/api',
      environment: 'development',
      status: 'active',
      lastChecked: new Date(),
      responseTime: 45,
      version: '1.0.0',
      tags: ['finance', 'budget', 'core'],
      endpoints: [
        {
          path: '/budget/check',
          method: 'POST',
          description: 'Check if budget is available for a department',
          parameters: [
            {
              name: 'departmentId',
              type: 'string',
              required: true,
              description: 'Department identifier'
            },
            {
              name: 'amount',
              type: 'number',
              required: true,
              description: 'Budget amount to check'
            }
          ],
          exampleRequest: JSON.stringify({ departmentId: 'HR', amount: 50000 }, null, 2),
          exampleResponse: JSON.stringify({ approved: true, availableBudget: 100000 }, null, 2)
        },
        {
          path: '/budget/allocate',
          method: 'POST',
          description: 'Allocate budget for a purchase order',
          parameters: [
            {
              name: 'poId',
              type: 'string',
              required: true,
              description: 'Purchase order ID'
            },
            {
              name: 'amount',
              type: 'number',
              required: true,
              description: 'Amount to allocate'
            }
          ]
        }
      ]
    },
    {
      id: 'procurement-service',
      name: 'procurement',
      displayName: 'Procurement Service',
      description: 'Purchase order and vendor management',
      baseUrl: 'http://procurement-service:8085/api',
      environment: 'development',
      status: 'active',
      lastChecked: new Date(),
      responseTime: 62,
      version: '1.0.0',
      tags: ['procurement', 'purchase', 'vendor'],
      endpoints: [
        {
          path: '/purchase-orders',
          method: 'POST',
          description: 'Create a new purchase order',
          parameters: [
            {
              name: 'vendorId',
              type: 'string',
              required: true,
              description: 'Vendor identifier'
            },
            {
              name: 'items',
              type: 'array',
              required: true,
              description: 'List of items to purchase'
            },
            {
              name: 'totalAmount',
              type: 'number',
              required: true,
              description: 'Total purchase amount'
            }
          ],
          exampleRequest: JSON.stringify({
            vendorId: 'V123',
            items: [{ itemId: 'ITEM001', quantity: 10, price: 100 }],
            totalAmount: 1000
          }, null, 2)
        },
        {
          path: '/purchase-orders/{id}',
          method: 'GET',
          description: 'Get purchase order details',
          parameters: [
            {
              name: 'id',
              type: 'string',
              required: true,
              description: 'Purchase order ID'
            }
          ]
        }
      ]
    },
    {
      id: 'inventory-service',
      name: 'inventory',
      displayName: 'Inventory Service',
      description: 'Inventory tracking and stock management',
      baseUrl: 'http://inventory-service:8086/api',
      environment: 'development',
      status: 'active',
      lastChecked: new Date(),
      responseTime: 38,
      version: '1.0.0',
      tags: ['inventory', 'stock', 'warehouse'],
      endpoints: [
        {
          path: '/stock/check',
          method: 'GET',
          description: 'Check stock availability',
          parameters: [
            {
              name: 'itemId',
              type: 'string',
              required: true,
              description: 'Item identifier'
            }
          ]
        },
        {
          path: '/stock/reserve',
          method: 'POST',
          description: 'Reserve stock for a purchase order',
          parameters: [
            {
              name: 'poId',
              type: 'string',
              required: true,
              description: 'Purchase order ID'
            },
            {
              name: 'items',
              type: 'array',
              required: true,
              description: 'Items to reserve'
            }
          ]
        }
      ]
    },
    {
      id: 'hr-service',
      name: 'hr',
      displayName: 'HR Service',
      description: 'Employee management and HR operations',
      baseUrl: 'http://hr-service:8082/api',
      environment: 'development',
      status: 'active',
      lastChecked: new Date(),
      responseTime: 51,
      version: '1.0.0',
      tags: ['hr', 'employee', 'leave'],
      endpoints: [
        {
          path: '/employees/{id}',
          method: 'GET',
          description: 'Get employee details',
          parameters: [
            {
              name: 'id',
              type: 'string',
              required: true,
              description: 'Employee ID'
            }
          ]
        },
        {
          path: '/leave/apply',
          method: 'POST',
          description: 'Apply for leave',
          parameters: [
            {
              name: 'employeeId',
              type: 'string',
              required: true,
              description: 'Employee ID'
            },
            {
              name: 'startDate',
              type: 'date',
              required: true,
              description: 'Leave start date'
            },
            {
              name: 'endDate',
              type: 'date',
              required: true,
              description: 'Leave end date'
            },
            {
              name: 'reason',
              type: 'string',
              required: true,
              description: 'Leave reason'
            }
          ]
        }
      ]
    }
  ]
}
