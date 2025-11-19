'use client';

import { useState, useEffect } from 'react';
import { Form } from '@formio/react';

interface DynamicFormEngineProps {
  formDefinition?: any; // Form.io JSON schema
  formKey?: string; // Load form by key from backend
  processInstanceId?: string; // For workflow integration
  taskId?: string; // For task forms
  onSubmit?: (submission: any) => void;
  onCancel?: () => void;
  readOnly?: boolean;
  initialData?: any;
}

export function DynamicFormEngine({
  formDefinition,
  formKey,
  processInstanceId,
  taskId,
  onSubmit,
  onCancel,
  readOnly = false,
  initialData,
}: DynamicFormEngineProps) {
  const [form, setForm] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [submission, setSubmission] = useState<any>(initialData || {});

  useEffect(() => {
    const loadForm = async () => {
      try {
        setLoading(true);

        // If formDefinition is provided, use it directly
        if (formDefinition) {
          setForm(formDefinition);
          setLoading(false);
          return;
        }

        // If formKey is provided, load from backend
        if (formKey) {
          const response = await fetch(`/api/forms/${formKey}`);
          if (!response.ok) {
            throw new Error('Failed to load form');
          }
          const formData = await response.json();
          setForm(formData.definition);
          setLoading(false);
          return;
        }

        // If taskId is provided, load task form
        if (taskId) {
          const response = await fetch(`/api/tasks/${taskId}/form`);
          if (!response.ok) {
            throw new Error('Failed to load task form');
          }
          const taskData = await response.json();
          setForm(taskData.form);
          setSubmission(taskData.variables || {});
          setLoading(false);
          return;
        }

        setLoading(false);
      } catch (error) {
        console.error('Error loading form:', error);
        alert('Failed to load form');
        setLoading(false);
      }
    };

    loadForm();
  }, [formDefinition, formKey, taskId]);

  const handleSubmit = async (formSubmission: any) => {
    try {
      // If taskId is provided, complete the task
      if (taskId) {
        const response = await fetch(`/api/tasks/${taskId}/complete`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            variables: formSubmission.data,
          }),
        });

        if (!response.ok) {
          throw new Error('Failed to complete task');
        }

        alert('Task completed successfully');
      }

      // If processInstanceId is provided, update process variables
      if (processInstanceId && !taskId) {
        const response = await fetch(
          `/api/process-instances/${processInstanceId}/variables`,
          {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify(formSubmission.data),
          }
        );

        if (!response.ok) {
          throw new Error('Failed to update process variables');
        }

        alert('Process variables updated');
      }

      // Call custom onSubmit handler if provided
      if (onSubmit) {
        onSubmit(formSubmission);
      }
    } catch (error) {
      console.error('Error submitting form:', error);
      alert('Failed to submit form');
    }
  };

  const handleCancel = () => {
    if (onCancel) {
      onCancel();
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!form) {
    return (
      <div className="text-center p-8">
        <p className="text-gray-500">No form available</p>
      </div>
    );
  }

  return (
    <div className="dynamic-form-engine">
      <Form
        form={form}
        submission={{ data: submission }}
        onSubmit={handleSubmit}
        options={{
          readOnly: readOnly,
        }}
      />
      {onCancel && (
        <div className="mt-4 flex justify-end">
          <button
            type="button"
            onClick={handleCancel}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 mr-2"
          >
            Cancel
          </button>
        </div>
      )}

      <style jsx global>{`
        .dynamic-form-engine .formio-component-submit button[type='submit'] {
          background-color: #2563eb;
          color: white;
          padding: 0.5rem 1rem;
          border-radius: 0.375rem;
          font-weight: 500;
          border: none;
          cursor: pointer;
        }
        .dynamic-form-engine .formio-component-submit button[type='submit']:hover {
          background-color: #1d4ed8;
        }
        .dynamic-form-engine .form-control:focus {
          border-color: #2563eb;
          outline: none;
          box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
        }
      `}</style>
    </div>
  );
}
