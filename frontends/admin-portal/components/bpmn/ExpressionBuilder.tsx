'use client'

import { useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Plus, Trash2, Copy, CheckCircle2 } from 'lucide-react'

export interface ExpressionCondition {
  variable: string
  operator: string
  value: string
  logicalOperator?: 'AND' | 'OR'
}

interface ExpressionBuilderProps {
  value?: string
  onChange?: (expression: string) => void
  availableVariables?: string[]
  placeholder?: string
}

const OPERATORS = [
  { value: '==', label: 'equals', symbol: '==' },
  { value: '!=', label: 'not equals', symbol: '!=' },
  { value: '>', label: 'greater than', symbol: '>' },
  { value: '<', label: 'less than', symbol: '<' },
  { value: '>=', label: 'greater or equal', symbol: '>=' },
  { value: '<=', label: 'less or equal', symbol: '<=' },
  { value: 'contains', label: 'contains', symbol: '.contains' },
  { value: 'startsWith', label: 'starts with', symbol: '.startsWith' },
  { value: 'endsWith', label: 'ends with', symbol: '.endsWith' },
]

/**
 * ExpressionBuilder Component
 *
 * Visual builder for Flowable expressions used in gateways and conditions.
 *
 * Features:
 * - Visual condition builder
 * - Variable selection from process context
 * - Operator selection
 * - Multi-condition support with AND/OR
 * - Real-time expression preview
 * - Copy to clipboard
 */
export default function ExpressionBuilder({
  value,
  onChange,
  availableVariables = [],
  placeholder = 'Build your expression'
}: ExpressionBuilderProps) {
  const [conditions, setConditions] = useState<ExpressionCondition[]>([])
  const [manualExpression, setManualExpression] = useState(value || '')
  const [copied, setCopied] = useState(false)
  const [mode, setMode] = useState<'visual' | 'manual'>('visual')

  const addCondition = () => {
    setConditions([
      ...conditions,
      {
        variable: '',
        operator: '==',
        value: '',
        logicalOperator: conditions.length > 0 ? 'AND' : undefined
      }
    ])
  }

  const updateCondition = (index: number, updates: Partial<ExpressionCondition>) => {
    const updated = conditions.map((cond, i) =>
      i === index ? { ...cond, ...updates } : cond
    )
    setConditions(updated)
    const expression = buildExpression(updated)
    if (onChange) onChange(expression)
  }

  const removeCondition = (index: number) => {
    const updated = conditions.filter((_, i) => i !== index)
    if (updated.length > 0 && !updated[0].logicalOperator) {
      updated[0] = { ...updated[0], logicalOperator: undefined }
    }
    setConditions(updated)
    const expression = buildExpression(updated)
    if (onChange) onChange(expression)
  }

  const buildExpression = (conds: ExpressionCondition[]): string => {
    if (conds.length === 0) return ''

    const parts = conds.map((cond, index) => {
      let expr = ''

      // Add logical operator for non-first conditions
      if (index > 0 && cond.logicalOperator) {
        expr += ` ${cond.logicalOperator === 'AND' ? '&&' : '||'} `
      }

      // Build condition
      if (cond.operator === 'contains') {
        expr += `${cond.variable}.contains('${cond.value}')`
      } else if (cond.operator === 'startsWith') {
        expr += `${cond.variable}.startsWith('${cond.value}')`
      } else if (cond.operator === 'endsWith') {
        expr += `${cond.variable}.endsWith('${cond.value}')`
      } else {
        const value = isNaN(Number(cond.value)) ? `'${cond.value}'` : cond.value
        expr += `${cond.variable} ${cond.operator} ${value}`
      }

      return expr
    })

    return `\${${parts.join('')}}`
  }

  const currentExpression = mode === 'visual' ? buildExpression(conditions) : manualExpression

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(currentExpression)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (error) {
      console.error('Failed to copy:', error)
    }
  }

  const handleManualChange = (expr: string) => {
    setManualExpression(expr)
    if (onChange) onChange(expr)
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-sm">Expression Builder</CardTitle>
            <CardDescription>
              Build conditions visually or write manually
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <Button
              variant={mode === 'visual' ? 'default' : 'outline'}
              size="sm"
              onClick={() => setMode('visual')}
              className="text-xs"
            >
              Visual
            </Button>
            <Button
              variant={mode === 'manual' ? 'default' : 'outline'}
              size="sm"
              onClick={() => setMode('manual')}
              className="text-xs"
            >
              Manual
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {mode === 'visual' ? (
          <>
            {/* Conditions */}
            {conditions.map((condition, index) => (
              <Card key={index} className="p-3">
                <div className="space-y-2">
                  {index > 0 && (
                    <div>
                      <Select
                        value={condition.logicalOperator}
                        onValueChange={(value: 'AND' | 'OR') =>
                          updateCondition(index, { logicalOperator: value })
                        }
                      >
                        <SelectTrigger className="h-7 w-20 text-xs">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="AND">AND</SelectItem>
                          <SelectItem value="OR">OR</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  )}

                  <div className="grid grid-cols-3 gap-2">
                    <div>
                      <Label className="text-xs">Variable</Label>
                      {availableVariables.length > 0 ? (
                        <Select
                          value={condition.variable}
                          onValueChange={(value) => updateCondition(index, { variable: value })}
                        >
                          <SelectTrigger className="h-8 text-xs">
                            <SelectValue placeholder="Select" />
                          </SelectTrigger>
                          <SelectContent>
                            {availableVariables.map((variable) => (
                              <SelectItem key={variable} value={variable}>
                                {variable}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      ) : (
                        <Input
                          value={condition.variable}
                          onChange={(e) => updateCondition(index, { variable: e.target.value })}
                          className="h-8 text-xs"
                          placeholder="Variable"
                        />
                      )}
                    </div>

                    <div>
                      <Label className="text-xs">Operator</Label>
                      <Select
                        value={condition.operator}
                        onValueChange={(value) => updateCondition(index, { operator: value })}
                      >
                        <SelectTrigger className="h-8 text-xs">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {OPERATORS.map((op) => (
                            <SelectItem key={op.value} value={op.value}>
                              {op.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div>
                      <Label className="text-xs">Value</Label>
                      <div className="flex gap-1">
                        <Input
                          value={condition.value}
                          onChange={(e) => updateCondition(index, { value: e.target.value })}
                          className="h-8 text-xs flex-1"
                          placeholder="Value"
                        />
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => removeCondition(index)}
                          className="h-8 w-8 p-0"
                        >
                          <Trash2 className="h-3 w-3 text-destructive" />
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
              </Card>
            ))}

            <Button
              variant="outline"
              size="sm"
              onClick={addCondition}
              className="w-full text-xs"
            >
              <Plus className="h-3 w-3 mr-2" />
              Add Condition
            </Button>
          </>
        ) : (
          <div>
            <Label className="text-xs">Expression</Label>
            <Input
              value={manualExpression}
              onChange={(e) => handleManualChange(e.target.value)}
              placeholder={placeholder}
              className="font-mono text-xs"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Example: ${'{totalAmount > 100000 && departmentId == "HR"}'}
            </p>
          </div>
        )}

        {/* Preview */}
        {currentExpression && (
          <div>
            <div className="flex items-center justify-between mb-1">
              <Label className="text-xs font-semibold">Expression Preview</Label>
              <Button
                variant="ghost"
                size="sm"
                onClick={copyToClipboard}
                className="h-6 w-6 p-0"
              >
                {copied ? (
                  <CheckCircle2 className="h-3 w-3 text-green-500" />
                ) : (
                  <Copy className="h-3 w-3" />
                )}
              </Button>
            </div>
            <pre className="text-xs bg-muted p-2 rounded overflow-x-auto font-mono">
              {currentExpression}
            </pre>
          </div>
        )}

        {/* Common Examples */}
        <div>
          <Label className="text-xs font-semibold">Common Examples</Label>
          <div className="space-y-1 mt-2">
            <code className="block text-xs bg-muted px-2 py-1 rounded">
              ${'{totalAmount > 100000}'}
            </code>
            <code className="block text-xs bg-muted px-2 py-1 rounded">
              ${'{status == "APPROVED"}'}
            </code>
            <code className="block text-xs bg-muted px-2 py-1 rounded">
              ${'{totalAmount > 50000 && departmentId == "HR"}'}
            </code>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
