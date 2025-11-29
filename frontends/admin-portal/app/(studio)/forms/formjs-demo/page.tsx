'use client';

import { useState } from 'react';
import FormJsViewer from '@/components/forms/FormJsViewer';
import FormJsEditor from '@/components/forms/FormJsEditor';

// Import form schemas
import capexRequestSchema from '@/../../../services/finance/src/main/resources/forms/formjs/capex-request-form.json';
import leaveRequestSchema from '@/../../../services/hr/src/main/resources/forms/formjs/leave-request-form.json';
import prSchema from '@/../../../services/procurement/src/main/resources/forms/formjs/purchase-requisition-form.json';

/**
 * Form.js Demo Page
 *
 * Demonstrates the usage of form-js library with various form types:
 * - CapEx Request Form
 * - Leave Request Form
 * - Purchase Requisition Form
 */
export default function FormJsDemoPage() {
  const [activeTab, setActiveTab] = useState<'capex' | 'leave' | 'pr' | 'editor'>('capex');
  const [formData, setFormData] = useState<Record<string, any>>({});
  const [submittedData, setSubmittedData] = useState<Record<string, any> | null>(null);
  const [editorSchema, setEditorSchema] = useState<any>(capexRequestSchema);

  const handleSubmit = (data: Record<string, any>) => {
    console.log('Form submitted:', data);
    setSubmittedData(data);
    alert('Form submitted successfully! Check console for data.');
  };

  const handleChange = (data: Record<string, any>) => {
    setFormData(data);
  };

  const handleSchemaChange = (schema: any) => {
    setEditorSchema(schema);
  };

  const handleSaveSchema = async (schema: any) => {
    console.log('Saving schema:', schema);
    // In production, save to backend
    return Promise.resolve();
  };

  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-3xl font-bold mb-6">Form.js Integration Demo</h1>

      {/* Tab Navigation */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex space-x-8">
          <button
            onClick={() => setActiveTab('capex')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'capex'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            CapEx Request
          </button>

          <button
            onClick={() => setActiveTab('leave')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'leave'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Leave Request
          </button>

          <button
            onClick={() => setActiveTab('pr')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'pr'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Purchase Requisition
          </button>

          <button
            onClick={() => setActiveTab('editor')}
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'editor'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Form Editor
          </button>
        </nav>
      </div>

      {/* Form Viewers */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Form Display */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4">
            {activeTab === 'capex' && 'Capital Expenditure Request Form'}
            {activeTab === 'leave' && 'Employee Leave Request Form'}
            {activeTab === 'pr' && 'Purchase Requisition Form'}
            {activeTab === 'editor' && 'Form Schema Editor'}
          </h2>

          {activeTab === 'capex' && (
            <FormJsViewer
              schema={capexRequestSchema}
              data={{
                requesterId: 'john.employee',
                requesterName: 'John Employee',
                requesterEmail: 'john.employee@werkflow.com',
                department: 'IT',
                budgetYear: 2025
              }}
              onSubmit={handleSubmit}
              onChange={handleChange}
            />
          )}

          {activeTab === 'leave' && (
            <FormJsViewer
              schema={leaveRequestSchema}
              data={{
                employeeId: 'john.employee',
                employeeName: 'John Employee',
                employeeEmail: 'john.employee@werkflow.com',
                department: 'Engineering',
                managerId: 'sarah.manager'
              }}
              onSubmit={handleSubmit}
              onChange={handleChange}
            />
          )}

          {activeTab === 'pr' && (
            <FormJsViewer
              schema={prSchema}
              data={{
                requesterId: 'john.employee',
                requesterName: 'John Employee',
                requesterEmail: 'john.employee@werkflow.com'
              }}
              onSubmit={handleSubmit}
              onChange={handleChange}
            />
          )}

          {activeTab === 'editor' && (
            <FormJsEditor
              schema={editorSchema}
              onSchemaChange={handleSchemaChange}
              onSave={handleSaveSchema}
            />
          )}
        </div>

        {/* Data Display */}
        {activeTab !== 'editor' && (
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold mb-4">Form Data</h2>

            <div className="space-y-4">
              {/* Current Form Data */}
              <div>
                <h3 className="text-sm font-medium text-gray-700 mb-2">
                  Current Data (Live Updates)
                </h3>
                <pre className="bg-gray-100 rounded p-4 text-xs overflow-auto max-h-64">
                  {JSON.stringify(formData, null, 2)}
                </pre>
              </div>

              {/* Submitted Data */}
              {submittedData && (
                <div>
                  <h3 className="text-sm font-medium text-gray-700 mb-2">
                    Submitted Data
                  </h3>
                  <pre className="bg-green-50 border border-green-200 rounded p-4 text-xs overflow-auto max-h-64">
                    {JSON.stringify(submittedData, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Schema Display for Editor */}
        {activeTab === 'editor' && (
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold mb-4">Form Schema (JSON)</h2>

            <pre className="bg-gray-100 rounded p-4 text-xs overflow-auto max-h-[600px]">
              {JSON.stringify(editorSchema, null, 2)}
            </pre>
          </div>
        )}
      </div>

      {/* Documentation Section */}
      <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h2 className="text-xl font-semibold text-blue-900 mb-4">
          Form.js Integration Guide
        </h2>

        <div className="prose prose-blue max-w-none">
          <h3 className="text-lg font-semibold text-blue-800">Features</h3>
          <ul className="list-disc list-inside text-blue-900 space-y-1">
            <li>JSON-based form definitions</li>
            <li>Visual form editor with drag-and-drop</li>
            <li>Built-in validation rules</li>
            <li>Conditional field visibility</li>
            <li>Multiple field types (text, number, date, select, etc.)</li>
            <li>Real-time data binding</li>
            <li>Import/Export form schemas</li>
          </ul>

          <h3 className="text-lg font-semibold text-blue-800 mt-4">Usage</h3>
          <p className="text-blue-900">
            Forms are defined as JSON schemas and can be rendered using the{' '}
            <code className="bg-blue-100 px-1 py-0.5 rounded">FormJsViewer</code>{' '}
            component. The editor allows you to create and modify forms visually.
          </p>

          <h3 className="text-lg font-semibold text-blue-800 mt-4">Form Schemas</h3>
          <p className="text-blue-900">
            Form schemas are stored in the services under{' '}
            <code className="bg-blue-100 px-1 py-0.5 rounded">
              src/main/resources/forms/formjs/
            </code>
          </p>
        </div>
      </div>
    </div>
  );
}
