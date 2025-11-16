'use client'

import { useEffect, useRef, useState } from 'react'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deployBpmn } from '@/lib/api/flowable'
import { generateBlankBpmn, downloadBpmn, extractProcessName } from '@/lib/bpmn/utils'
import { Save, Download, Upload, ZoomIn, ZoomOut, Maximize2, Settings } from 'lucide-react'
import { useRouter } from 'next/navigation'

// Import BPMN.js CSS
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-js.css'

// Import Properties Panel CSS
import 'bpmn-js-properties-panel/dist/assets/properties-panel.css'

// Import Properties Panel modules
import {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  CamundaPlatformPropertiesProviderModule
} from 'bpmn-js-properties-panel'

interface BpmnDesignerProps {
  initialXml?: string
  processId?: string
  onSave?: (xml: string) => void
}

export default function BpmnDesigner({ initialXml, processId, onSave }: BpmnDesignerProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const propertiesPanelRef = useRef<HTMLDivElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [modeler, setModeler] = useState<BpmnModeler | null>(null)
  const [processName, setProcessName] = useState('')
  const [hasChanges, setHasChanges] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showProperties, setShowProperties] = useState(true)
  const queryClient = useQueryClient()
  const router = useRouter()

  // Initialize BPMN modeler
  useEffect(() => {
    if (!containerRef.current || !propertiesPanelRef.current) return

    const bpmnModeler = new BpmnModeler({
      container: containerRef.current,
      keyboard: {
        bindTo: document
      },
      height: '100%',
      propertiesPanel: {
        parent: propertiesPanelRef.current
      },
      additionalModules: [
        BpmnPropertiesPanelModule,
        BpmnPropertiesProviderModule,
        CamundaPlatformPropertiesProviderModule
      ]
    })

    // Load initial diagram
    const xmlToLoad = initialXml || generateBlankBpmn()
    bpmnModeler
      .importXML(xmlToLoad)
      .then(() => {
        const canvas = bpmnModeler.get('canvas')
        canvas.zoom('fit-viewport')

        // Extract process name
        const name = extractProcessName(xmlToLoad)
        if (name) setProcessName(name)
      })
      .catch((err: Error) => {
        console.error('Error importing BPMN:', err)
        setError('Failed to load BPMN diagram')
      })

    setModeler(bpmnModeler)

    // Listen for diagram changes
    const eventBus = bpmnModeler.get('eventBus')
    const handleChange = () => setHasChanges(true)

    eventBus.on('commandStack.changed', handleChange)

    return () => {
      eventBus.off('commandStack.changed', handleChange)
      bpmnModeler.destroy()
    }
  }, [initialXml])

  // Deploy to backend
  const deployMutation = useMutation({
    mutationFn: async () => {
      if (!modeler) throw new Error('Modeler not initialized')

      const { xml } = await modeler.saveXML({ format: true })
      if (!xml) throw new Error('Failed to generate XML')

      const resourceName = `${processName || 'process'}.bpmn20.xml`

      return deployBpmn({
        name: processName || 'New Process',
        resourceName,
        bpmnXml: xml
      })
    },
    onSuccess: () => {
      setHasChanges(false)
      queryClient.invalidateQueries({ queryKey: ['processDefinitions'] })
      alert('Process deployed successfully!')
      router.push('/studio/processes')
    },
    onError: (error: Error) => {
      alert(`Deployment failed: ${error.message}`)
    }
  })

  // Save locally (callback)
  const handleSave = async () => {
    if (!modeler) return

    try {
      const { xml } = await modeler.saveXML({ format: true })
      if (xml && onSave) {
        onSave(xml)
        setHasChanges(false)
      }
    } catch (err) {
      console.error('Error saving BPMN:', err)
      setError('Failed to save diagram')
    }
  }

  // Download as file
  const handleDownload = async () => {
    if (!modeler) return

    try {
      const { xml } = await modeler.saveXML({ format: true })
      if (xml) {
        const filename = `${processName || 'process'}.bpmn20.xml`
        downloadBpmn(xml, filename)
      }
    } catch (err) {
      console.error('Error downloading BPMN:', err)
      setError('Failed to download diagram')
    }
  }

  // Load from file
  const handleUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file || !modeler) return

    const reader = new FileReader()
    reader.onload = async (e) => {
      const xml = e.target?.result as string
      try {
        await modeler.importXML(xml)
        const canvas = modeler.get('canvas')
        canvas.zoom('fit-viewport')
        setHasChanges(true)

        const name = extractProcessName(xml)
        if (name) setProcessName(name)
      } catch (err) {
        console.error('Error loading BPMN:', err)
        setError('Failed to load BPMN file')
      }
    }
    reader.readAsText(file)
  }

  // Zoom controls
  const handleZoomIn = () => {
    if (!modeler) return
    const canvas = modeler.get('canvas')
    canvas.zoom(canvas.zoom() + 0.1)
  }

  const handleZoomOut = () => {
    if (!modeler) return
    const canvas = modeler.get('canvas')
    canvas.zoom(canvas.zoom() - 0.1)
  }

  const handleFitViewport = () => {
    if (!modeler) return
    const canvas = modeler.get('canvas')
    canvas.zoom('fit-viewport')
  }

  return (
    <div className="flex flex-col h-screen">
      {/* Toolbar */}
      <Card className="border-b rounded-none">
        <div className="flex items-center justify-between p-4">
          <div className="flex items-center gap-4 flex-1">
            <input
              type="text"
              value={processName}
              onChange={(e) => setProcessName(e.target.value)}
              placeholder="Process name"
              className="border rounded px-3 py-2 w-64"
            />
            {hasChanges && (
              <span className="text-sm text-amber-600">Unsaved changes</span>
            )}
          </div>

          <div className="flex items-center gap-2">
            {/* Properties panel toggle */}
            <Button
              variant={showProperties ? "default" : "outline"}
              size="sm"
              onClick={() => setShowProperties(!showProperties)}
              title="Toggle properties panel"
            >
              <Settings className="h-4 w-4" />
            </Button>

            {/* Zoom controls */}
            <div className="flex items-center gap-1 border-l pl-2">
              <Button variant="outline" size="sm" onClick={handleZoomOut} title="Zoom out">
                <ZoomOut className="h-4 w-4" />
              </Button>
              <Button variant="outline" size="sm" onClick={handleZoomIn} title="Zoom in">
                <ZoomIn className="h-4 w-4" />
              </Button>
              <Button variant="outline" size="sm" onClick={handleFitViewport} title="Fit to viewport">
                <Maximize2 className="h-4 w-4" />
              </Button>
            </div>

            {/* File operations */}
            <input
              ref={fileInputRef}
              type="file"
              accept=".bpmn,.bpmn20.xml,.xml"
              onChange={handleUpload}
              className="hidden"
            />
            <Button
              variant="outline"
              size="sm"
              onClick={() => fileInputRef.current?.click()}
              title="Load from file"
            >
              <Upload className="h-4 w-4 mr-2" />
              Load
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleDownload}
              title="Download as file"
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
              disabled={deployMutation.isPending || !processName}
            >
              {deployMutation.isPending ? 'Deploying...' : 'Deploy to Flowable'}
            </Button>
          </div>
        </div>
      </Card>

      {/* Error display */}
      {error && (
        <div className="bg-destructive/10 text-destructive p-3 text-sm">
          {error}
        </div>
      )}

      {/* Main content area with canvas and properties panel */}
      <div className="flex flex-1 overflow-hidden">
        {/* BPMN Canvas */}
        <div ref={containerRef} className="flex-1 bg-gray-50" />

        {/* Properties Panel */}
        {showProperties && (
          <div className="w-80 border-l bg-white overflow-auto">
            <div ref={propertiesPanelRef} className="h-full" />
          </div>
        )}
      </div>
    </div>
  )
}
