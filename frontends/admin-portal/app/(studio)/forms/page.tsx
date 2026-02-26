'use client'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { FileText, Plus, Trash2, Download, Eye, Edit, Activity, ExternalLink } from "lucide-react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { getFormDefinitions, deleteFormDefinition } from "@/lib/api/flowable"
import { useState } from "react"
import { ErrorDisplay, LoadingState } from "@/components/ui/error-display"

export default function FormsPage() {
  const [deletingKey, setDeletingKey] = useState<string | null>(null)
  const queryClient = useQueryClient()

  // Fetch form definitions
  const { data: forms, isLoading, error, refetch } = useQuery({
    queryKey: ['formDefinitions'],
    queryFn: getFormDefinitions,
    retry: 2,
    retryDelay: 1000,
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
            Visual form builder with form-js
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
      {isLoading && <LoadingState message="Loading form definitions..." className="mb-6" />}

      {/* Error state */}
      {error && !isLoading && (
        <ErrorDisplay
          error={error as Error}
          onRetry={() => refetch()}
          title="Failed to load forms"
          className="mb-6"
        />
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

      {/* How to Create a Form Guide */}
      <Card className="border-2">
        <CardHeader className="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-950 dark:to-indigo-950">
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-6 w-6 text-blue-600 dark:text-blue-400" />
            How to Create a Form
          </CardTitle>
          <CardDescription>
            Follow these steps to create and deploy a custom form for your workflow
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-6">
          <div className="space-y-6">
            {/* Step 1 */}
            <div className="flex gap-4">
              <div className="flex-shrink-0">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100 text-blue-600 dark:bg-blue-900 dark:text-blue-400 font-bold">
                  1
                </div>
              </div>
              <div className="flex-1 pt-1">
                <h3 className="font-semibold text-lg mb-2">Click Create New Form</h3>
                <p className="text-muted-foreground mb-3">
                  Start by clicking the "Create New Form" button above. This opens the visual form designer powered by form-js.
                </p>
                <div className="flex items-center gap-2 text-sm text-blue-600 dark:text-blue-400">
                  <Plus className="h-4 w-4" />
                  <span>Located in the top right corner</span>
                </div>
              </div>
            </div>

            {/* Step 2 */}
            <div className="flex gap-4">
              <div className="flex-shrink-0">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-purple-100 text-purple-600 dark:bg-purple-900 dark:text-purple-400 font-bold">
                  2
                </div>
              </div>
              <div className="flex-1 pt-1">
                <h3 className="font-semibold text-lg mb-2">Design Your Form</h3>
                <p className="text-muted-foreground mb-3">
                  Use drag-and-drop to add fields like text inputs, dropdowns, date pickers, and more. Configure validation rules and field properties.
                </p>
                <div className="grid grid-cols-2 gap-2 text-sm">
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <div className="h-2 w-2 rounded-full bg-purple-500" />
                    <span>20+ field types available</span>
                  </div>
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <div className="h-2 w-2 rounded-full bg-purple-500" />
                    <span>Built-in validation</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Step 3 */}
            <div className="flex gap-4">
              <div className="flex-shrink-0">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-green-100 text-green-600 dark:bg-green-900 dark:text-green-400 font-bold">
                  3
                </div>
              </div>
              <div className="flex-1 pt-1">
                <h3 className="font-semibold text-lg mb-2">Configure Field Settings</h3>
                <p className="text-muted-foreground mb-3">
                  Set field labels, placeholders, default values, and validation rules. Add conditional logic to show/hide fields based on user input.
                </p>
                <div className="flex items-center gap-2 text-sm text-green-600 dark:text-green-400">
                  <Activity className="h-4 w-4" />
                  <span>Support for conditional visibility</span>
                </div>
              </div>
            </div>

            {/* Step 4 */}
            <div className="flex gap-4">
              <div className="flex-shrink-0">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-orange-100 text-orange-600 dark:bg-orange-900 dark:text-orange-400 font-bold">
                  4
                </div>
              </div>
              <div className="flex-1 pt-1">
                <h3 className="font-semibold text-lg mb-2">Preview and Test</h3>
                <p className="text-muted-foreground mb-3">
                  Use the preview mode to test your form. Fill out fields, trigger validations, and ensure the form works as expected.
                </p>
                <div className="flex items-center gap-2 text-sm text-orange-600 dark:text-orange-400">
                  <Eye className="h-4 w-4" />
                  <span>Real-time form preview</span>
                </div>
              </div>
            </div>

            {/* Step 5 */}
            <div className="flex gap-4">
              <div className="flex-shrink-0">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-100 text-indigo-600 dark:bg-indigo-900 dark:text-indigo-400 font-bold">
                  5
                </div>
              </div>
              <div className="flex-1 pt-1">
                <h3 className="font-semibold text-lg mb-2">Save and Deploy</h3>
                <p className="text-muted-foreground mb-3">
                  Give your form a unique key (e.g., "capex-request-form") and save it. The form is automatically deployed and ready to use in your workflows.
                </p>
                <div className="flex items-center gap-2 text-sm text-indigo-600 dark:text-indigo-400">
                  <Download className="h-4 w-4" />
                  <span>Export form schema as JSON</span>
                </div>
              </div>
            </div>

            {/* Step 6 */}
            <div className="flex gap-4">
              <div className="flex-shrink-0">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-pink-100 text-pink-600 dark:bg-pink-900 dark:text-pink-400 font-bold">
                  6
                </div>
              </div>
              <div className="flex-1 pt-1">
                <h3 className="font-semibold text-lg mb-2">Link to Workflow</h3>
                <p className="text-muted-foreground mb-3">
                  Reference your form in BPMN user tasks using the form key. The form will automatically appear when users complete tasks in the workflow.
                </p>
                <div className="flex items-center gap-2 text-sm text-pink-600 dark:text-pink-400">
                  <ExternalLink className="h-4 w-4" />
                  <span>Seamless BPMN integration</span>
                </div>
              </div>
            </div>
          </div>

          {/* Call to Action */}
          <div className="mt-8 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-950 dark:to-indigo-950 rounded-lg border border-blue-200 dark:border-blue-800">
            <p className="text-sm text-muted-foreground mb-3">
              Ready to create your first form? Click the button below to get started.
            </p>
            <Button asChild className="w-full sm:w-auto">
              <Link href="/studio/forms/new">
                <Plus className="h-4 w-4 mr-2" />
                Create New Form
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
