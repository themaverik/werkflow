'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { getFormDefinition } from '@/lib/api/flowable'
import DynamicFormBuilder from '@/components/forms/FormBuilder'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ArrowLeft } from 'lucide-react'
import Link from 'next/link'

export default function EditFormPage() {
  const params = useParams()
  const formKey = params.key as string

  // Fetch form definition
  const { data: formDef, isLoading, error } = useQuery({
    queryKey: ['form', formKey],
    queryFn: () => getFormDefinition(formKey),
    enabled: !!formKey
  })

  if (isLoading) {
    return (
      <div className="container py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">Loading form definition...</p>
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
              Failed to load form: {error instanceof Error ? error.message : 'Unknown error'}
            </p>
            <Button asChild>
              <Link href="/studio/forms">
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Forms
              </Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="h-screen flex flex-col">
      <DynamicFormBuilder
        initialForm={formDef?.formJson}
        formKey={formKey}
      />
    </div>
  )
}
