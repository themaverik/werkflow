'use client'

import { useEffect, useImperativeHandle, useRef, forwardRef } from 'react'

// dmn-js CSS — loaded from public/vendor/ (copied at Docker build time from node_modules)
// JS is NOT imported here: dmn-js references window at module parse time and cannot be
// processed by webpack. The UMD bundle is loaded as a browser <script> tag at runtime.

export interface DmnEditorHandle {
  /** Returns the current DMN XML from the editor */
  getXml(): Promise<string>
}

interface DmnEditorProps {
  /** Initial DMN XML to load into the editor */
  xml?: string
  /** Whether the editor is in read-only mode */
  readOnly?: boolean
  /** Called whenever the DMN XML changes */
  onChange?: (xml: string) => void
}

/** Loads the dmn-js UMD bundle from /vendor/dmn-modeler.min.js exactly once. */
function loadDmnJs(): Promise<any> {
  return new Promise((resolve, reject) => {
    const win = window as any
    // Already loaded — resolve immediately
    if (win.DmnJS) {
      const Cls = win.DmnJS?.default ?? win.DmnJS
      return resolve(Cls)
    }
    // Already loading — wait for existing script tag
    const existing = document.querySelector('script[data-dmn-js]')
    if (existing) {
      existing.addEventListener('load', () => resolve(win.DmnJS?.default ?? win.DmnJS))
      existing.addEventListener('error', reject)
      return
    }
    // First load — create script tag
    const script = document.createElement('script')
    script.src = '/vendor/dmn-modeler.min.js'
    script.setAttribute('data-dmn-js', '1')
    script.onload = () => resolve(win.DmnJS?.default ?? win.DmnJS)
    script.onerror = () => reject(new Error('Failed to load dmn-js from /vendor/dmn-modeler.min.js'))
    document.head.appendChild(script)
  })
}

/**
 * React wrapper for dmn-js DmnModeler.
 * Loads the dmn-js UMD bundle at runtime (browser-only) to avoid webpack
 * CJS/ESM interop failures — dmn-js uses window APIs at module parse time.
 */
const DmnEditor = forwardRef<DmnEditorHandle, DmnEditorProps>(function DmnEditor(
  { xml, readOnly = false, onChange },
  ref
) {
  const containerRef = useRef<HTMLDivElement>(null)
  const modelerRef = useRef<any>(null)

  useImperativeHandle(ref, () => ({
    async getXml(): Promise<string> {
      if (!modelerRef.current) throw new Error('DMN editor not initialised')
      const { xml: result } = await modelerRef.current.saveXML({ format: true })
      return result
    },
  }))

  useEffect(() => {
    if (!containerRef.current) return

    // Load CSS dynamically alongside the UMD bundle
    const cssFiles = [
      '/vendor/diagram-js.css',
      '/vendor/dmn-js-shared.css',
      '/vendor/dmn-js-drd.css',
      '/vendor/dmn-js-decision-table.css',
      '/vendor/dmn-js-decision-table-controls.css',
      '/vendor/dmn-font/css/dmn.css',
    ]
    cssFiles.forEach((href) => {
      if (!document.querySelector(`link[href="${href}"]`)) {
        const link = document.createElement('link')
        link.rel = 'stylesheet'
        link.href = href
        document.head.appendChild(link)
      }
    })

    let modeler: any

    async function init() {
      const DmnModelerCls = await loadDmnJs()
      if (!DmnModelerCls || typeof DmnModelerCls !== 'function') {
        throw new Error(`dmn-js did not export a constructor (got ${typeof DmnModelerCls})`)
      }

      modeler = new DmnModelerCls({ container: containerRef.current })
      modelerRef.current = modeler

      const initialXml = xml || DEFAULT_EMPTY_DMN

      try {
        await modeler.importXML(initialXml)
        const views: any[] = modeler.getViews()
        const tableView = views.find((v: any) => v.type === 'decisionTable')
        if (tableView) modeler.open(tableView)
      } catch (err) {
        console.error('Failed to import DMN XML:', err)
      }

      if (onChange) {
        modeler.on('commandStack.changed', async () => {
          try {
            const { xml: updated } = await modeler.saveXML({ format: true })
            onChange(updated)
          } catch {
            // ignore save errors during live editing
          }
        })
      }

      if (readOnly && containerRef.current) {
        containerRef.current.style.pointerEvents = 'none'
      }
    }

    init().catch((err) => console.error('DmnEditor init failed:', err))

    return () => {
      if (modeler) {
        modeler.destroy()
        modelerRef.current = null
      }
    }
    // xml and readOnly intentionally excluded — reinitialising on every change breaks editing
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Reload XML when the prop changes from outside (e.g. switching between decisions)
  useEffect(() => {
    if (!modelerRef.current || !xml) return
    modelerRef.current.importXML(xml).then(() => {
      const views: any[] = modelerRef.current.getViews()
      const tableView = views.find((v: any) => v.type === 'decisionTable')
      if (tableView) modelerRef.current.open(tableView)
    }).catch((err: unknown) => {
      console.error('Failed to reload DMN XML:', err)
    })
  }, [xml])

  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height: '600px', border: '1px solid #e2e8f0', borderRadius: '8px' }}
    />
  )
})

export default DmnEditor

// ---- minimal valid DMN 1.3 used when creating a new decision ----
// DMNDI is required so dmn-js can render the DRD and register decisionTable sub-views.
const DEFAULT_EMPTY_DMN = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
             xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/"
             xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/"
             id="new_decision_definitions"
             name="New Decision"
             namespace="http://werkflow.com/dmn">
  <decision id="new_decision" name="New Decision">
    <decisionTable id="new_decision_table" hitPolicy="FIRST">
      <input id="input_1" label="Input">
        <inputExpression id="input_1_expr" typeRef="string">
          <text>inputVariable</text>
        </inputExpression>
      </input>
      <output id="output_1" label="Output" name="outputVariable" typeRef="string"/>
    </decisionTable>
  </decision>
  <dmndi:DMNDI>
    <dmndi:DMNDiagram>
      <dmndi:DMNShape dmnElementRef="new_decision">
        <dc:Bounds height="80" width="180" x="160" y="100"/>
      </dmndi:DMNShape>
    </dmndi:DMNDiagram>
  </dmndi:DMNDI>
</definitions>`
