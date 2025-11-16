'use client'

import { Form } from '@formio/react'
import { useState, useEffect } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { getFormDefinition } from '@/lib/api/flowable'
import { completeTask } from '@/lib/api/workflows'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Loader2 } from 'lucide-react'

// Import Form.io CSS
import 'formiojs/dist/formio.full.min.css'

interface FormRendererProps {
  formKey: string
  taskId?: string
  onComplete?: (data: any) => void
  onCancel?: () => void
  initialData?: any
  readOnly?: boolean
}

export default function FormRenderer({
  formKey,
  taskId,
  onComplete,
  onCancel,
  initialData = {},
  readOnly = false
}: FormRendererProps) {
  const [submissionData, setSubmissionData] = useState<any>(null)

  // Fetch form definition
  const { data: formDef, isLoading, error } = useQuery({
    queryKey: ['form', formKey],
    queryFn: () => getFormDefinition(formKey),
    enabled: !!formKey
  })

  // Complete task mutation
  const completeMutation = useMutation({
    mutationFn: async (formData: any) => {
      if (taskId) {
        return completeTask({
          taskId,
          variables: formData,
          comment: ''
        })
      }
      // If no taskId, just call the onComplete callback
      return Promise.resolve(formData)
    },
    onSuccess: (_, formData) => {
      if (onComplete) {
        onComplete(formData)
      }
    },
    onError: (error: Error) => {
      alert(`Failed to submit form: ${error.message}`)
    }
  })

  // Handle form submission
  const handleSubmit = (submission: any) => {
    setSubmissionData(submission.data)
    completeMutation.mutate(submission.data)
  }

  if (isLoading) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <Loader2 className="h-8 w-8 mx-auto mb-4 animate-spin text-primary" />
          <p className="text-muted-foreground">Loading form...</p>
        </CardContent>
      </Card>
    )
  }

  if (error) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <p className="text-destructive mb-4">
            Failed to load form: {error instanceof Error ? error.message : 'Unknown error'}
          </p>
          {onCancel && (
            <Button variant="outline" onClick={onCancel}>
              Go Back
            </Button>
          )}
        </CardContent>
      </Card>
    )
  }

  if (!formDef) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <p className="text-muted-foreground mb-4">Form definition not found</p>
          {onCancel && (
            <Button variant="outline" onClick={onCancel}>
              Go Back
            </Button>
          )}
        </CardContent>
      </Card>
    )
  }

  let formSchema
  try {
    formSchema = JSON.parse(formDef.formJson)
  } catch (err) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <p className="text-destructive mb-4">Invalid form definition</p>
          {onCancel && (
            <Button variant="outline" onClick={onCancel}>
              Go Back
            </Button>
          )}
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{formSchema.title || formDef.name}</CardTitle>
        {formSchema.description && (
          <CardDescription>{formSchema.description}</CardDescription>
        )}
      </CardHeader>
      <CardContent>
        <Form
          form={formSchema}
          submission={{ data: initialData }}
          onSubmit={handleSubmit}
          options={{
            readOnly,
            noAlerts: true,
            buttonSettings: {
              showCancel: !!onCancel,
              showSubmit: !readOnly
            }
          }}
        />

        {/* Custom action buttons */}
        {!readOnly && (
          <div className="flex gap-2 mt-6 pt-6 border-t">
            {onCancel && (
              <Button
                variant="outline"
                onClick={onCancel}
                disabled={completeMutation.isPending}
              >
                Cancel
              </Button>
            )}
            {completeMutation.isPending && (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" />
                Submitting form...
              </div>
            )}
          </div>
        )}

        {completeMutation.isSuccess && (
          <div className="mt-4 p-4 bg-green-50 text-green-800 rounded-lg">
            Form submitted successfully!
          </div>
        )}
      </CardContent>
    </Card>
  )
}
