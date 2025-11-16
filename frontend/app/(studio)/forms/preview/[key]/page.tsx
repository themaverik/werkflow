'use client'

import { useParams, useRouter } from 'next/navigation'
import FormRenderer from '@/components/forms/FormRenderer'
import { Button } from '@/components/ui/button'
import { ArrowLeft } from 'lucide-react'
import Link from 'next/link'

export default function PreviewFormPage() {
  const params = useParams()
  const router = useRouter()
  const formKey = params.key as string

  return (
    <div className="container py-6 max-w-4xl">
      <div className="mb-6 flex items-center gap-4">
        <Button asChild variant="outline" size="sm">
          <Link href="/studio/forms">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Forms
          </Link>
        </Button>
        <h1 className="text-2xl font-bold">Form Preview</h1>
      </div>

      <FormRenderer
        formKey={formKey}
        onComplete={(data) => {
          console.log('Form submitted:', data)
          alert('Form submitted! Check console for data.')
        }}
        onCancel={() => router.push('/studio/forms')}
      />
    </div>
  )
}
