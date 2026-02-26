'use client';

import { useEffect, useRef, useState } from 'react';
import { Form } from '@bpmn-io/form-js';
import '@bpmn-io/form-js/dist/assets/form-js.css';
import '@bpmn-io/form-js/dist/assets/form-js-editor.css';

interface FormJsViewerProps {
  schema: any;
  data?: Record<string, any>;
  onSubmit?: (data: Record<string, any>) => void;
  onChange?: (data: Record<string, any>) => void;
  readonly?: boolean;
  className?: string;
}

/**
 * FormJsViewer Component
 *
 * Wrapper component for bpmn-io/form-js library.
 * Renders forms based on JSON schema and handles form submissions.
 *
 * Usage:
 * ```tsx
 * <FormJsViewer
 *   schema={formSchema}
 *   data={initialData}
 *   onSubmit={handleSubmit}
 *   onChange={handleChange}
 * />
 * ```
 */
export default function FormJsViewer({
  schema,
  data = {},
  onSubmit,
  onChange,
  readonly = false,
  className = ''
}: FormJsViewerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const formRef = useRef<Form | null>(null);
  const [formData, setFormData] = useState<Record<string, any>>(data);
  const [errors, setErrors] = useState<any[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!containerRef.current) return;

    // Initialize form-js
    const form = new Form({
      container: containerRef.current
    });

    formRef.current = form;

    // Import schema and data
    form.importSchema(schema, data).catch((err) => {
      console.error('Failed to import form schema:', err);
    });

    // Apply readonly mode if specified
    if (readonly && containerRef.current) {
      containerRef.current.classList.add('form-js-readonly');
    }

    // Listen to form changes
    form.on('changed', (event: any) => {
      const updatedData = event.data;
      setFormData(updatedData);

      if (onChange) {
        onChange(updatedData);
      }
    });

    // Listen to submit events
    form.on('submit', async (event: any) => {
      event.preventDefault();

      if (onSubmit && !isSubmitting) {
        setIsSubmitting(true);
        setErrors([]);

        try {
          // Validate form before submission
          const result = form.validate();

          if (result.errors && Object.keys(result.errors).length > 0) {
            setErrors(Object.values(result.errors));
            console.error('Form validation errors:', result.errors);
            return;
          }

          await onSubmit(event.data);
        } catch (error) {
          console.error('Form submission error:', error);
          setErrors([{ message: 'Failed to submit form. Please try again.' }]);
        } finally {
          setIsSubmitting(false);
        }
      }
    });

    // Cleanup
    return () => {
      if (formRef.current) {
        formRef.current.destroy();
      }
    };
  }, [schema, readonly]);

  // Update form data when props change
  useEffect(() => {
    if (formRef.current && data) {
      formRef.current.importSchema(schema, data).catch((err) => {
        console.error('Failed to update form data:', err);
      });
    }
  }, [data, schema]);

  return (
    <div className={`form-js-viewer-wrapper ${className}`}>
      {errors.length > 0 && (
        <div className="form-errors bg-red-50 border border-red-200 rounded-md p-4 mb-4">
          <h3 className="text-red-800 font-semibold mb-2">Validation Errors:</h3>
          <ul className="list-disc list-inside text-red-700">
            {errors.map((error, index) => (
              <li key={index}>{error.message || JSON.stringify(error)}</li>
            ))}
          </ul>
        </div>
      )}

      <div
        ref={containerRef}
        className="form-js-container"
        style={{ minHeight: '400px' }}
      />

      {isSubmitting && (
        <div className="form-submitting-overlay fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 shadow-xl">
            <div className="flex items-center space-x-3">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              <span className="text-lg font-medium">Submitting form...</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
