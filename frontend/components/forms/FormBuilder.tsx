'use client'

import { FormBuilder } from '@formio/react'
import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deployForm } from '@/lib/api/flowable'
import { Save, Download, Upload, Eye } from 'lucide-react'
import { useRouter } from 'next/navigation'

// Import Form.io CSS
import 'formiojs/dist/formio.full.min.css'

interface DynamicFormBuilderProps {
  initialForm?: string // JSON string
  formKey?: string
  onSave?: (formJson: string) => void
}

export default function DynamicFormBuilder({ initialForm, formKey: initialFormKey, onSave }: DynamicFormBuilderProps) {
  const [formSchema, setFormSchema] = useState({
    display: 'form',
    components: []
  })
  const [formKey, setFormKey] = useState(initialFormKey || '')
  const [formName, setFormName] = useState('')
  const [hasChanges, setHasChanges] = useState(false)
  const [showPreview, setShowPreview] = useState(false)
  const queryClient = useQueryClient()
  const router = useRouter()

  // Load initial form if provided
  useEffect(() => {
    if (initialForm) {
      try {
        const parsed = JSON.parse(initialForm)
        setFormSchema(parsed)
        if (parsed.title) setFormName(parsed.title)
      } catch (err) {
        console.error('Error parsing initial form:', err)
      }
    }
  }, [initialForm])

  // Deploy form mutation
  const deployMutation = useMutation({
    mutationFn: async () => {
      // Add title to schema
      const schemaWithTitle = {
        ...formSchema,
        title: formName || formKey
      }

      return deployForm({
        key: formKey,
        name: formName || formKey,
        formJson: JSON.stringify(schemaWithTitle, null, 2)
      })
    },
    onSuccess: () => {
      setHasChanges(false)
      queryClient.invalidateQueries({ queryKey: ['formDefinitions'] })
      alert('Form deployed successfully!')
      router.push('/studio/forms')
    },
    onError: (error: Error) => {
      alert(`Deployment failed: ${error.message}`)
    }
  })

  // Handle form changes
  const handleFormChange = (schema: any) => {
    setFormSchema(schema)
    setHasChanges(true)
  }

  // Save locally (callback)
  const handleSave = () => {
    const schemaWithTitle = {
      ...formSchema,
      title: formName || formKey
    }
    const formJson = JSON.stringify(schemaWithTitle, null, 2)

    if (onSave) {
      onSave(formJson)
      setHasChanges(false)
    }
  }

  // Download as JSON
  const handleDownload = () => {
    const schemaWithTitle = {
      ...formSchema,
      title: formName || formKey
    }
    const blob = new Blob([JSON.stringify(schemaWithTitle, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${formKey || 'form'}.json`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }

  // Upload form JSON
  const handleUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const json = JSON.parse(e.target?.result as string)
        setFormSchema(json)
        if (json.title) setFormName(json.title)
        setHasChanges(true)
      } catch (err) {
        alert('Invalid JSON file')
      }
    }
    reader.readAsText(file)
  }

  return (
    <div className="flex flex-col h-screen">
      {/* Toolbar */}
      <Card className="border-b rounded-none">
        <div className="flex items-center justify-between p-4">
          <div className="flex items-center gap-4 flex-1">
            <input
              type="text"
              value={formKey}
              onChange={(e) => setFormKey(e.target.value)}
              placeholder="Form key (e.g., leave-request-form)"
              className="border rounded px-3 py-2 w-64 text-sm"
            />
            <input
              type="text"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              placeholder="Form name"
              className="border rounded px-3 py-2 w-64 text-sm"
            />
            {hasChanges && (
              <span className="text-sm text-amber-600">Unsaved changes</span>
            )}
          </div>

          <div className="flex items-center gap-2">
            {/* Preview toggle */}
            <Button
              variant={showPreview ? "default" : "outline"}
              size="sm"
              onClick={() => setShowPreview(!showPreview)}
              title="Toggle preview"
            >
              <Eye className="h-4 w-4" />
            </Button>

            {/* File operations */}
            <input
              type="file"
              accept=".json"
              onChange={handleUpload}
              className="hidden"
              id="form-upload"
            />
            <Button
              variant="outline"
              size="sm"
              onClick={() => document.getElementById('form-upload')?.click()}
              title="Load from file"
            >
              <Upload className="h-4 w-4 mr-2" />
              Load
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleDownload}
              title="Download as JSON"
            >
              <Download className="h-4 w-4 mr-2" />
              Download
            </Button>

            {/* Save/Deploy */}
            {onSave && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleSave}
                disabled={!hasChanges}
              >
                <Save className="h-4 w-4 mr-2" />
                Save
              </Button>
            )}
            <Button
              size="sm"
              onClick={() => deployMutation.mutate()}
              disabled={deployMutation.isPending || !formKey}
            >
              {deployMutation.isPending ? 'Deploying...' : 'Deploy Form'}
            </Button>
          </div>
        </div>
      </Card>

      {/* Main content */}
      <div className="flex-1 overflow-auto bg-gray-50 p-6">
        {!showPreview ? (
          <div className="max-w-7xl mx-auto bg-white rounded-lg shadow-sm">
            <FormBuilder
              form={formSchema}
              onChange={handleFormChange}
            />
          </div>
        ) : (
          <div className="max-w-4xl mx-auto">
            <Card>
              <div className="p-6">
                <h2 className="text-2xl font-bold mb-4">{formName || formKey || 'Form Preview'}</h2>
                <div className="border rounded p-4 bg-white">
                  <FormBuilder
                    form={formSchema}
                    onChange={handleFormChange}
                  />
                </div>
              </div>
            </Card>
          </div>
        )}
      </div>
    </div>
  )
}
