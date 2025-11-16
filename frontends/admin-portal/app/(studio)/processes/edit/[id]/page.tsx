'use client'

import { useParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { getProcessDefinitionXml } from '@/lib/api/flowable'
import BpmnDesigner from '@/components/bpmn/BpmnDesigner'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ArrowLeft } from 'lucide-react'
import Link from 'next/link'

export default function EditProcessPage() {
  const params = useParams()
  const router = useRouter()
  const processDefinitionId = params.id as string

  // Fetch process definition XML
  const { data: bpmnXml, isLoading, error } = useQuery({
    queryKey: ['processDefinition', processDefinitionId],
    queryFn: () => getProcessDefinitionXml(processDefinitionId),
    enabled: !!processDefinitionId
  })

  if (isLoading) {
    return (
      <div className="container py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">Loading process definition...</p>
          </CardContent>
        </Card>
      </div>
    )
  }

  if (error) {
    return (
      <div className="container py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-destructive mb-4">
              Failed to load process: {error instanceof Error ? error.message : 'Unknown error'}
            </p>
            <Button asChild>
              <Link href="/studio/processes">
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Processes
              </Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="h-screen flex flex-col">
      <BpmnDesigner
        initialXml={bpmnXml}
        processId={processDefinitionId}
      />
    </div>
  )
}
