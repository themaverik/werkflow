'use client'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { FileText, Plus, Trash2, Download, Eye, Edit } from "lucide-react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { getFormDefinitions, deleteFormDefinition } from "@/lib/api/flowable"
import { useState } from "react"

export default function FormsPage() {
  const [deletingKey, setDeletingKey] = useState<string | null>(null)
  const queryClient = useQueryClient()

  // Fetch form definitions
  const { data: forms, isLoading } = useQuery({
    queryKey: ['formDefinitions'],
    queryFn: getFormDefinitions
  })

  // Delete form mutation
  const deleteMutation = useMutation({
    mutationFn: deleteFormDefinition,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['formDefinitions'] })
      setDeletingKey(null)
      alert('Form deleted successfully')
    },
    onError: (error: Error) => {
      alert(`Failed to delete form: ${error.message}`)
      setDeletingKey(null)
    }
  })

  // Download form JSON
  const handleDownload = (formKey: string, formJson: string) => {
    const blob = new Blob([formJson], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${formKey}.json`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }

  return (
    <div className="container py-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Form Designer</h1>
          <p className="text-muted-foreground">
            Visual form builder with Form.io
          </p>
        </div>
        <Button asChild size="lg">
          <Link href="/studio/forms/new">
            <Plus className="h-4 w-4 mr-2" />
            Create New Form
          </Link>
        </Button>
      </div>

      {/* Loading state */}
      {isLoading && (
        <Card className="mb-6">
          <CardContent className="py-8 text-center">
            <p className="text-muted-foreground">Loading form definitions...</p>
          </CardContent>
        </Card>
      )}

      {/* Deployed Forms */}
      {!isLoading && forms && forms.length > 0 && (
        <div className="mb-6">
          <h2 className="text-xl font-semibold mb-4">Deployed Forms</h2>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {forms.map((form) => (
              <Card key={form.key}>
                <CardHeader>
                  <CardTitle className="text-lg flex items-center gap-2">
                    <FileText className="h-5 w-5" />
                    {form.name}
                  </CardTitle>
                  <CardDescription className="font-mono text-xs">
                    Key: {form.key}
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    <div className="flex gap-2">
                      <Button
                        asChild
                        variant="outline"
                        size="sm"
                        className="flex-1"
                      >
                        <Link href={`/studio/forms/edit/${form.key}`}>
                          <Edit className="h-4 w-4 mr-2" />
                          Edit
                        </Link>
                      </Button>
                      <Button
                        asChild
                        variant="outline"
                        size="sm"
                        className="flex-1"
                      >
                        <Link href={`/studio/forms/preview/${form.key}`}>
                          <Eye className="h-4 w-4 mr-2" />
                          Preview
                        </Link>
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleDownload(form.key, form.formJson)}
                      >
                        <Download className="h-4 w-4" />
                      </Button>
                    </div>
                    <Button
                      variant="destructive"
                      size="sm"
                      className="w-full"
                      onClick={() => {
                        if (confirm(`Delete form "${form.name}"?`)) {
                          setDeletingKey(form.key)
                          deleteMutation.mutate(form.key)
                        }
                      }}
                      disabled={deletingKey === form.key}
                    >
                      <Trash2 className="h-4 w-4 mr-2" />
                      {deletingKey === form.key ? 'Deleting...' : 'Delete'}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Empty state */}
      {!isLoading && (!forms || forms.length === 0) && (
        <Card className="mb-6">
          <CardContent className="py-12 text-center">
            <FileText className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
            <h3 className="text-lg font-semibold mb-2">No forms created yet</h3>
            <p className="text-muted-foreground mb-4">
              Create your first form to collect data from users
            </p>
            <Button asChild>
              <Link href="/studio/forms/new">
                <Plus className="h-4 w-4 mr-2" />
                Create New Form
              </Link>
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Quick start card */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 mb-6">
        <Card className="border-2 border-dashed hover:border-primary/50 transition-colors">
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <Plus className="h-5 w-5" />
              Blank Form
            </CardTitle>
            <CardDescription>
              Start with an empty form
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild variant="outline" className="w-full">
              <Link href="/studio/forms/new">Create</Link>
            </Button>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>✅ Phase 3: Form.io Integration Complete!</CardTitle>
          <CardDescription>
            Dynamic form builder and renderer fully integrated
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="text-sm">
              <p className="font-semibold mb-2">Week 5 Features (Form Builder):</p>
              <ul className="list-disc list-inside space-y-1 text-muted-foreground">
                <li>✅ Form.io FormBuilder component with drag-drop UI</li>
                <li>✅ Visual form designer with field types (text, email, dropdown, date, etc.)</li>
                <li>✅ Form validation rules configuration</li>
                <li>✅ Save/deploy forms to backend</li>
                <li>✅ Load/download form JSON</li>
                <li>✅ Form preview mode</li>
                <li>✅ Form list page with management</li>
              </ul>
            </div>
            <div className="text-sm">
              <p className="font-semibold mb-2">Week 6 Features (Form Renderer):</p>
              <ul className="list-disc list-inside space-y-1 text-muted-foreground">
                <li>✅ FormRenderer component for task completion</li>
                <li>✅ Fetch form definitions from backend</li>
                <li>✅ Client-side form validation</li>
                <li>✅ Form submission with task completion</li>
                <li>✅ Read-only form viewing mode</li>
                <li>✅ Error handling and loading states</li>
              </ul>
            </div>
            <div className="text-sm">
              <p className="font-semibold mb-2">Integration with BPMN:</p>
              <ul className="list-disc list-inside space-y-1 text-muted-foreground">
                <li>✅ Link forms to BPMN tasks via form key property</li>
                <li>✅ Forms render dynamically based on task definition</li>
                <li>✅ Form data submitted as process variables</li>
                <li>✅ Complete workflow-to-form integration</li>
              </ul>
            </div>
            <div className="text-sm">
              <p className="font-semibold mb-2">Coming in Phase 4 (Weeks 7-8):</p>
              <ul className="list-disc list-inside space-y-1 text-muted-foreground">
                <li>Task list page with filters and search</li>
                <li>Task claiming and assignment</li>
                <li>Process instance tracking</li>
                <li>Real-time task updates</li>
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
