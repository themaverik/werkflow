'use client'

import { useParams, useRouter } from 'next/navigation'
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { ArrowLeft } from "lucide-react"
import Link from "next/link"
import { useToast } from "@/hooks/use-toast"
import { getProcessStartForm } from "@/lib/api/flowable"
import { startProcess } from "@/lib/api/workflows"
import FormJsViewer from "@/components/forms/FormJsViewer"

export default function StartProcessPage() {
  const params = useParams()
  const router = useRouter()
  const { toast } = useToast()
  const processDefinitionId = params.id as string
  const [formData, setFormData] = useState<Record<string, any>>({})

  const { data: startForm, isLoading, error } = useQuery({
    queryKey: ['processStartForm', processDefinitionId],
    queryFn: () => getProcessStartForm(processDefinitionId),
    enabled: !!processDefinitionId,
  })

  const startProcessMutation = useMutation({
    mutationFn: async (variables: Record<string, any>) => {
      // Extract process definition key from the ID (format: key:version:hash)
      const processDefinitionKey = processDefinitionId.split(':')[0]
      return startProcess({
        processDefinitionKey,
        variables,
      })
    },
    onSuccess: (data) => {
      toast({
        title: 'Process Started',
        description: `Process instance ${data.processInstanceId} created successfully.`,
      })
      router.push('/requests')
    },
    onError: (error: Error) => {
      toast({
        title: 'Failed to Start Process',
        description: error.message || 'An error occurred while starting the process.',
        variant: 'destructive',
      })
    },
  })

  const handleFormSubmit = async (data: Record<string, any>) => {
    startProcessMutation.mutate(data)
  }

  if (isLoading) {
    return (
      <div className="container py-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-muted rounded w-1/3" />
          <div className="h-64 bg-muted rounded" />
        </div>
      </div>
    )
  }

  if (error || !startForm) {
    return (
      <div className="container py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <h3 className="text-lg font-semibold mb-2">Start Form Not Available</h3>
            <p className="text-muted-foreground mb-4">
              {error instanceof Error ? error.message : 'This process does not have a start form configured.'}
            </p>
            <Button asChild>
              <Link href="/processes">
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
    <div className="container py-6 max-w-3xl">
      <Button variant="ghost" asChild className="mb-4">
        <Link href="/processes">
          <ArrowLeft className="h-4 w-4 mr-2" />
          Back to Processes
        </Link>
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>Start New Process</CardTitle>
          {startForm.description && (
            <CardDescription>{startForm.description}</CardDescription>
          )}
        </CardHeader>
        <CardContent>
          <FormJsViewer
            schema={startForm.schema}
            data={{}}
            onSubmit={handleFormSubmit}
            onChange={setFormData}
          />
          {startProcessMutation.isPending && (
            <p className="text-sm text-muted-foreground mt-4">Starting process...</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
