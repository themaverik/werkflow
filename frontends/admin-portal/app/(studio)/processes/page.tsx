'use client'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { FileText, Plus, Trash2, Download, Eye } from "lucide-react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { getProcessDefinitions, deleteDeployment, getProcessDefinitionXml } from "@/lib/api/flowable"
import { downloadBpmn } from "@/lib/bpmn/utils"
import { useState } from "react"

export default function ProcessesPage() {
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const queryClient = useQueryClient()

  // Fetch process definitions
  const { data: processes, isLoading } = useQuery({
    queryKey: ['processDefinitions'],
    queryFn: getProcessDefinitions
  })

  // Delete deployment mutation
  const deleteMutation = useMutation({
    mutationFn: deleteDeployment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['processDefinitions'] })
      setDeletingId(null)
      alert('Process deployment deleted successfully')
    },
    onError: (error: Error) => {
      alert(`Failed to delete deployment: ${error.message}`)
      setDeletingId(null)
    }
  })

  // Download BPMN XML
  const handleDownload = async (processDefinitionId: string, name: string) => {
    try {
      const xml = await getProcessDefinitionXml(processDefinitionId)
      downloadBpmn(xml, `${name}.bpmn20.xml`)
    } catch (error) {
      alert(`Failed to download BPMN: ${error instanceof Error ? error.message : 'Unknown error'}`)
    }
  }

  // Group processes by key to show versions
  const groupedProcesses = processes?.reduce((acc, process) => {
    if (!acc[process.key]) {
      acc[process.key] = []
    }
    acc[process.key].push(process)
    return acc
  }, {} as Record<string, typeof processes>)

  return (
    <div className="container py-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Process Designer</h1>
          <p className="text-muted-foreground">
            Visual BPMN workflow designer with bpmn-js
          </p>
        </div>
        <Button asChild size="lg">
          <Link href="/studio/processes/new">
            <Plus className="h-4 w-4 mr-2" />
            Create New Process
          </Link>
        </Button>
      </div>

      {/* Loading state */}
      {isLoading && (
        <Card className="mb-6">
          <CardContent className="py-8 text-center">
            <p className="text-muted-foreground">Loading process definitions...</p>
          </CardContent>
        </Card>
      )}

      {/* Deployed Processes */}
      {!isLoading && groupedProcesses && Object.keys(groupedProcesses).length > 0 && (
        <div className="mb-6">
          <h2 className="text-xl font-semibold mb-4">Deployed Processes</h2>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {Object.entries(groupedProcesses).map(([key, versions]) => {
              // Sort by version descending to get latest version
              const sortedVersions = [...versions].sort((a, b) => b.version - a.version)
              const latestVersion = sortedVersions[0]

              return (
                <Card key={key}>
                  <CardHeader>
                    <CardTitle className="text-lg flex items-center gap-2">
                      <FileText className="h-5 w-5" />
                      {latestVersion.name || key}
                    </CardTitle>
                    <CardDescription>
                      Version {latestVersion.version} • {versions.length} version{versions.length > 1 ? 's' : ''} deployed
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
                          <Link href={`/studio/processes/edit/${latestVersion.id}`}>
                            <Eye className="h-4 w-4 mr-2" />
                            Edit
                          </Link>
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleDownload(latestVersion.id, latestVersion.name || key)}
                        >
                          <Download className="h-4 w-4" />
                        </Button>
                      </div>
                      {versions.length > 1 && (
                        <div className="text-xs text-muted-foreground">
                          <details className="cursor-pointer">
                            <summary>Show all versions</summary>
                            <div className="mt-2 space-y-1">
                              {sortedVersions.map((version) => (
                                <div key={version.id} className="flex justify-between items-center p-2 bg-muted/50 rounded">
                                  <span>v{version.version}</span>
                                  <div className="flex gap-1">
                                    <Button
                                      asChild
                                      variant="ghost"
                                      size="sm"
                                    >
                                      <Link href={`/studio/processes/edit/${version.id}`}>
                                        Edit
                                      </Link>
                                    </Button>
                                    <Button
                                      variant="ghost"
                                      size="sm"
                                      onClick={() => handleDownload(version.id, version.name || key)}
                                    >
                                      <Download className="h-3 w-3" />
                                    </Button>
                                  </div>
                                </div>
                              ))}
                            </div>
                          </details>
                        </div>
                      )}
                      <Button
                        variant="destructive"
                        size="sm"
                        className="w-full"
                        onClick={() => {
                          if (confirm(`Delete all versions of "${latestVersion.name || key}"?`)) {
                            setDeletingId(latestVersion.deploymentId)
                            deleteMutation.mutate(latestVersion.deploymentId)
                          }
                        }}
                        disabled={deletingId === latestVersion.deploymentId}
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        {deletingId === latestVersion.deploymentId ? 'Deleting...' : 'Delete'}
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              )
            })}
          </div>
        </div>
      )}

      {/* Empty state */}
      {!isLoading && (!processes || processes.length === 0) && (
        <Card className="mb-6">
          <CardContent className="py-12 text-center">
            <FileText className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
            <h3 className="text-lg font-semibold mb-2">No processes deployed yet</h3>
            <p className="text-muted-foreground mb-4">
              Create your first BPMN process to get started
            </p>
            <Button asChild>
              <Link href="/studio/processes/new">
                <Plus className="h-4 w-4 mr-2" />
                Create New Process
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
              Blank Process
            </CardTitle>
            <CardDescription>
              Start with an empty BPMN diagram
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild variant="outline" className="w-full">
              <Link href="/studio/processes/new">Create</Link>
            </Button>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>✅ Phase 2 Week 4: Process Management & Properties Panel!</CardTitle>
          <CardDescription>
            Full BPMN process lifecycle management
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="text-sm">
              <p className="font-semibold mb-2">Week 3 Features:</p>
              <ul className="list-disc list-inside space-y-1 text-muted-foreground">
                <li>✅ Visual BPMN editor with bpmn-js</li>
                <li>✅ Drag-and-drop workflow designer</li>
                <li>✅ Blank process template generator</li>
                <li>✅ Load BPMN from file</li>
                <li>✅ Download BPMN as XML file</li>
                <li>✅ Zoom controls (in, out, fit viewport)</li>
                <li>✅ One-click deployment to Flowable backend</li>
                <li>✅ Real-time change detection</li>
              </ul>
            </div>
            <div className="text-sm">
              <p className="font-semibold mb-2">Week 4 Features (In Progress):</p>
              <ul className="list-disc list-inside space-y-1 text-muted-foreground">
                <li>✅ Properties panel for element configuration</li>
                <li>✅ Flowable-specific properties (assignee, groups, form keys)</li>
                <li>✅ Process definition list with versions</li>
                <li>🔄 Load and edit existing processes from backend</li>
                <li>✅ Process delete functionality</li>
                <li>✅ Version history display</li>
              </ul>
            </div>
            <div className="text-sm">
              <p className="font-semibold mb-2">Coming in Phase 3 (Weeks 5-6):</p>
              <ul className="list-disc list-inside space-y-1 text-muted-foreground">
                <li>Form.io form builder integration</li>
                <li>Dynamic form renderer for tasks</li>
                <li>Form-task linking via form keys</li>
                <li>Form deployment to backend</li>
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
