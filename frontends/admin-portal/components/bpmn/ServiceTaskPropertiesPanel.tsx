'use client'

import { useEffect, useState } from 'react'
import ExtensionElementsEditor from './ExtensionElementsEditor'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Button } from '@/components/ui/button'
import { useServices } from '@/lib/hooks/useServiceRegistry'
import { ExternalLink } from 'lucide-react'

interface ServiceTaskPropertiesPanelProps {
  element: any
  modeler: any
}

/**
 * ServiceTaskPropertiesPanel Component
 *
 * Integrated properties panel for ServiceTask configuration in BPMN designer.
 * Displays:
 * - Delegate expression selector
 * - Service URL builder (with service registry integration)
 * - Extension elements editor
 */
export default function ServiceTaskPropertiesPanel({
  element,
  modeler
}: ServiceTaskPropertiesPanelProps) {
  const { data: services } = useServices()
  const [delegateExpression, setDelegateExpression] = useState('')
  const [selectedService, setSelectedService] = useState('')
  const [selectedEndpoint, setSelectedEndpoint] = useState('')

  useEffect(() => {
    if (element && modeler) {
      const businessObject = element.businessObject
      const delegate = businessObject.get('flowable:delegateExpression') ||
                      businessObject.delegateExpression || ''
      setDelegateExpression(delegate)
    }
  }, [element, modeler])

  const handleDelegateChange = (value: string) => {
    setDelegateExpression(value)
    const modeling = modeler.get('modeling')
    modeling.updateProperties(element, {
      'flowable:delegateExpression': value || undefined,
      delegateExpression: value || undefined
    })
  }

  const handleServiceSelect = (serviceName: string) => {
    setSelectedService(serviceName)
    setSelectedEndpoint('')
  }

  const handleEndpointSelect = (endpointPath: string) => {
    setSelectedEndpoint(endpointPath)

    // Auto-fill URL field in extension elements
    const service = services?.find(s => s.serviceName === selectedService)
    if (service) {
      const fullUrl = `${service.baseUrl || ''}${endpointPath}`
      // This will be handled by ExtensionElementsEditor
      // We can emit a custom event or use a callback
    }
  }

  const selectedServiceData = services?.find(s => s.serviceName === selectedService)
  const availableEndpoints = selectedServiceData?.endpoints || []

  return (
    <div className="space-y-4 p-4">
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Delegate Configuration</CardTitle>
          <CardDescription>
            Configure the service delegate for this task
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <div>
            <Label className="text-xs">Delegate Expression</Label>
            <Select value={delegateExpression} onValueChange={handleDelegateChange}>
              <SelectTrigger className="h-8 text-xs">
                <SelectValue placeholder="Select delegate" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="${restServiceDelegate}">
                  REST Service Delegate
                </SelectItem>
                <SelectItem value="${emailDelegate}">
                  Email Delegate
                </SelectItem>
                <SelectItem value="${notificationDelegate}">
                  Notification Delegate
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {delegateExpression === '${restServiceDelegate}' && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm">Service Selection</CardTitle>
            <CardDescription>
              Select a service and endpoint from the registry
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div>
              <Label className="text-xs">Service</Label>
              <Select value={selectedService} onValueChange={handleServiceSelect}>
                <SelectTrigger className="h-8 text-xs">
                  <SelectValue placeholder="Select service" />
                </SelectTrigger>
                <SelectContent>
                  {services?.map((service) => (
                    <SelectItem key={service.id} value={service.serviceName}>
                      {service.displayName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {selectedService && availableEndpoints.length > 0 && (
              <div>
                <Label className="text-xs">Endpoint</Label>
                <Select value={selectedEndpoint} onValueChange={handleEndpointSelect}>
                  <SelectTrigger className="h-8 text-xs">
                    <SelectValue placeholder="Select endpoint" />
                  </SelectTrigger>
                  <SelectContent>
                    {availableEndpoints.map((endpoint, index) => (
                      <SelectItem key={index} value={endpoint.endpointPath}>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs">{endpoint.httpMethod}</span>
                          <span>{endpoint.endpointPath}</span>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {selectedServiceData && (
              <div className="pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full text-xs"
                  onClick={() => window.open('/studio/services', '_blank')}
                >
                  <ExternalLink className="h-3 w-3 mr-2" />
                  View Service Registry
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <ExtensionElementsEditor
        element={element}
        modeler={modeler}
      />
    </div>
  )
}
