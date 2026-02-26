'use client'

import { useState, useEffect, useCallback } from 'react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deployForm } from '@/lib/api/flowable'
import { Save, Download, Upload } from 'lucide-react'
import { useRouter } from 'next/navigation'
import FormJsEditor from './FormJsEditor'

interface FormJsBuilderProps {
  initialForm?: string
  formKey?: string
  onSave?: (formJson: string) => void
}

/**
 * FormJsBuilder Component
 *
 * Wrapper around FormJsEditor that provides form management functionality
 * including saving, deploying, and importing/exporting forms.
 *
 * This component replaces the old Form.io-based FormBuilder with form-js.
 */
export default function FormJsBuilder({
  initialForm,
  formKey: initialFormKey,
  onSave
}: FormJsBuilderProps) {
  const [formSchema, setFormSchema] = useState<any>({
    type: 'default',
    components: [],
    schemaVersion: 9
  })
  const [formKey, setFormKey] = useState(initialFormKey || '')
  const [formName, setFormName] = useState('')
  const [hasChanges, setHasChanges] = useState(false)
  const queryClient = useQueryClient()
  const router = useRouter()

  // Load initial form if provided
  useEffect(() => {
    if (initialForm) {
      try {
        const parsed = JSON.parse(initialForm)

        // Check if it's a Form.io schema or form-js schema
        if (parsed.components && !parsed.type) {
          // Form.io schema - needs conversion
          console.warn('Form.io schema detected, converting to form-js format')
          setFormSchema({
            type: 'default',
            components: [],
            schemaVersion: 9
          })
        } else {
          // form-js schema
          setFormSchema(parsed)
        }

        if (parsed.title) setFormName(parsed.title)
      } catch (err) {
        console.error('Error parsing initial form:', err)
      }
    }
  }, [initialForm])

  // Deploy form mutation
  const deployMutation = useMutation({
    mutationFn: async () => {
      if (!formKey) {
        throw new Error('Form key is required')
      }

      // Create schema with metadata
      const schemaToSave = {
        ...formSchema,
        id: formKey,
        title: formName || formKey
      }

      return deployForm({
        key: formKey,
        name: formName || formKey,
        formJson: JSON.stringify(schemaToSave, null, 2)
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

  // Handle form changes from editor
  const handleSchemaChange = useCallback((newSchema: any) => {
    setFormSchema(newSchema)
    setHasChanges(true)
  }, [])

  // Save handler for FormJsEditor
  const handleEditorSave = useCallback(async (schema: any) => {
    const schemaToSave = {
      ...schema,
      id: formKey,
      title: formName || formKey
    }
    const formJson = JSON.stringify(schemaToSave, null, 2)

    if (onSave) {
      onSave(formJson)
      setHasChanges(false)
    }
  }, [formKey, formName, onSave])

  // Download as JSON
  const handleDownload = () => {
    const schemaToDownload = {
      ...formSchema,
      id: formKey,
      title: formName || formKey
    }
    const blob = new Blob([JSON.stringify(schemaToDownload, null, 2)], { type: 'application/json' })
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

        // Validate that it's a form-js schema
        if (!json.type || !json.components) {
          alert('Invalid form-js schema. Please ensure the file contains a valid form-js form definition.')
          return
        }

        setFormSchema(json)
        if (json.title) setFormName(json.title)
        if (json.id) setFormKey(json.id)
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
              disabled={!!initialFormKey}
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
                onClick={() => handleEditorSave(formSchema)}
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

      {/* Form Editor */}
      <div className="flex-1 overflow-hidden">
        <FormJsEditor
          schema={formSchema}
          onSchemaChange={handleSchemaChange}
          onSave={handleEditorSave}
          className="h-full"
        />
      </div>
    </div>
  )
}
