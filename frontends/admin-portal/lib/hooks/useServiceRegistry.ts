import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getServices,
  getServiceByName,
  createService,
  updateService,
  updateServiceUrl,
  deleteService,
  testServiceConnectivity,
  getServiceEndpoints,
  getServiceHealth,
  type Service,
  type CreateServiceRequest,
  type UpdateServiceRequest,
  type ServiceConnectivityTestResult
} from '@/lib/api/services'

/**
 * Hook to fetch all services
 */
export function useServices() {
  return useQuery({
    queryKey: ['services'],
    queryFn: getServices,
    refetchInterval: 60000, // Refetch every minute
    staleTime: 30000 // Consider data stale after 30 seconds
  })
}

/**
 * Hook to fetch a specific service
 */
export function useService(serviceName: string) {
  return useQuery({
    queryKey: ['services', serviceName],
    queryFn: () => getServiceByName(serviceName),
    enabled: !!serviceName,
    staleTime: 30000
  })
}

/**
 * Hook to fetch service endpoints
 */
export function useServiceEndpoints(serviceName: string) {
  return useQuery({
    queryKey: ['services', serviceName, 'endpoints'],
    queryFn: () => getServiceEndpoints(serviceName),
    enabled: !!serviceName,
    staleTime: 60000
  })
}

/**
 * Hook to check service health
 */
export function useServiceHealth(serviceName: string) {
  return useQuery({
    queryKey: ['services', serviceName, 'health'],
    queryFn: () => getServiceHealth(serviceName),
    enabled: !!serviceName,
    refetchInterval: 30000, // Check health every 30 seconds
    staleTime: 15000
  })
}

/**
 * Hook to create a new service
 */
export function useCreateService() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateServiceRequest) => createService(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['services'] })
    }
  })
}

/**
 * Hook to update a service
 */
export function useUpdateService() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ serviceId, data }: { serviceId: string; data: UpdateServiceRequest }) =>
      updateService(serviceId, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['services'] })
      queryClient.invalidateQueries({ queryKey: ['services', variables.serviceId] })
    }
  })
}

/**
 * Hook to update service URL
 */
export function useUpdateServiceUrl() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      serviceId,
      baseUrl,
      environment
    }: {
      serviceId: string
      baseUrl: string
      environment?: string
    }) => updateServiceUrl(serviceId, baseUrl, environment),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['services'] })
      queryClient.invalidateQueries({ queryKey: ['services', variables.serviceId] })
    }
  })
}

/**
 * Hook to delete a service
 */
export function useDeleteService() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (serviceId: string) => deleteService(serviceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['services'] })
    }
  })
}

/**
 * Hook to test service connectivity
 */
export function useTestServiceConnectivity() {
  return useMutation({
    mutationFn: (serviceUrl: string) => testServiceConnectivity(serviceUrl)
  })
}
