'use client';

import { useEffect, useRef, useState } from 'react';
import { FormEditor } from '@bpmn-io/form-js-editor';
import '@bpmn-io/form-js/dist/assets/form-js.css';
import '@bpmn-io/form-js/dist/assets/form-js-editor.css';
import '@bpmn-io/form-js-editor/dist/assets/form-js-editor.css';

interface FormJsEditorProps {
  schema?: any;
  onSchemaChange?: (schema: any) => void;
  onSave?: (schema: any) => void;
  className?: string;
}

/**
 * FormJsEditor Component
 *
 * Wrapper component for bpmn-io/form-js-editor library.
 * Provides a visual editor for creating and modifying form schemas.
 *
 * Usage:
 * ```tsx
 * <FormJsEditor
 *   schema={initialSchema}
 *   onSchemaChange={handleSchemaChange}
 *   onSave={handleSave}
 * />
 * ```
 */
export default function FormJsEditor({
  schema,
  onSchemaChange,
  onSave,
  className = ''
}: FormJsEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<FormEditor | null>(null);
  const [currentSchema, setCurrentSchema] = useState<any>(schema);
  const [isSaving, setIsSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState<string>('');

  useEffect(() => {
    if (!containerRef.current) return;

    // Initialize form-js editor
    const editor = new FormEditor({
      container: containerRef.current
    });

    editorRef.current = editor;

    // Import initial schema
    const initialSchema = schema || {
      type: 'default',
      components: [],
      schemaVersion: 9
    };

    editor.importSchema(initialSchema).catch((err) => {
      console.error('Failed to import form schema:', err);
    });

    // Listen to schema changes
    editor.on('changed', () => {
      editor.saveSchema().then((updatedSchema) => {
        setCurrentSchema(updatedSchema);

        if (onSchemaChange) {
          onSchemaChange(updatedSchema);
        }
      }).catch((err) => {
        console.error('Failed to save schema:', err);
      });
    });

    // Cleanup
    return () => {
      if (editorRef.current) {
        editorRef.current.destroy();
      }
    };
  }, []);

  // Update editor schema when props change
  useEffect(() => {
    if (editorRef.current && schema) {
      editorRef.current.importSchema(schema).catch((err) => {
        console.error('Failed to update editor schema:', err);
      });
    }
  }, [schema]);

  const handleSave = async () => {
    if (!editorRef.current || !onSave) return;

    setIsSaving(true);
    setSaveMessage('');

    try {
      const schemaToSave = await editorRef.current.saveSchema();
      await onSave(schemaToSave);
      setSaveMessage('Form saved successfully!');

      setTimeout(() => {
        setSaveMessage('');
      }, 3000);
    } catch (error) {
      console.error('Failed to save form:', error);
      setSaveMessage('Failed to save form. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleExportJson = async () => {
    if (!editorRef.current) return;

    try {
      const schemaToExport = await editorRef.current.saveSchema();
      const jsonString = JSON.stringify(schemaToExport, null, 2);
      const blob = new Blob([jsonString], { type: 'application/json' });
      const url = URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = url;
      link.download = `form-schema-${Date.now()}.json`;
      link.click();

      URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to export schema:', error);
    }
  };

  const handleImportJson = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'application/json';

    input.onchange = (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;

      const reader = new FileReader();
      reader.onload = (event) => {
        try {
          const importedSchema = JSON.parse(event.target?.result as string);

          if (editorRef.current) {
            editorRef.current.importSchema(importedSchema).catch((err) => {
              console.error('Failed to import schema:', err);
              alert('Failed to import schema. Please check the file format.');
            });
          }
        } catch (error) {
          console.error('Failed to parse JSON:', error);
          alert('Invalid JSON file. Please check the file format.');
        }
      };

      reader.readAsText(file);
    };

    input.click();
  };

  return (
    <div className={`form-js-editor-wrapper ${className}`}>
      {/* Toolbar */}
      <div className="form-editor-toolbar bg-gray-100 border-b border-gray-300 p-3 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <h2 className="text-lg font-semibold text-gray-800">Form Editor</h2>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={handleImportJson}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Import JSON
          </button>

          <button
            onClick={handleExportJson}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Export JSON
          </button>

          {onSave && (
            <button
              onClick={handleSave}
              disabled={isSaving}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSaving ? 'Saving...' : 'Save Form'}
            </button>
          )}
        </div>
      </div>

      {/* Save message */}
      {saveMessage && (
        <div
          className={`save-message p-3 text-center font-medium ${
            saveMessage.includes('success')
              ? 'bg-green-100 text-green-800'
              : 'bg-red-100 text-red-800'
          }`}
        >
          {saveMessage}
        </div>
      )}

      {/* Editor container */}
      <div
        ref={containerRef}
        className="form-js-editor-container"
        style={{ height: 'calc(100vh - 200px)', minHeight: '600px' }}
      />
    </div>
  );
}
