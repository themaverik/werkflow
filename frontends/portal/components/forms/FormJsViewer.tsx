'use client';

import { useEffect, useRef, useCallback } from 'react';
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
  const onSubmitRef = useRef(onSubmit);
  const onChangeRef = useRef(onChange);

  // Keep refs in sync without triggering re-renders
  onSubmitRef.current = onSubmit;
  onChangeRef.current = onChange;

  useEffect(() => {
    if (!containerRef.current) return;

    const form = new Form({
      container: containerRef.current
    });

    formRef.current = form;

    form.importSchema(schema, data).catch((err) => {
      console.error('Failed to import form schema:', err);
    });

    if (readonly && containerRef.current) {
      containerRef.current.classList.add('form-js-readonly');
    }

    form.on('changed', (event: any) => {
      if (onChangeRef.current) {
        onChangeRef.current(event.data);
      }
    });

    form.on('submit', (event: any) => {
      if (onSubmitRef.current) {
        onSubmitRef.current(event.data);
      }
    });

    return () => {
      formRef.current = null;
      form.destroy();
    };
  // Only re-create the form when schema identity changes
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [schema, readonly]);

  const handleSubmit = useCallback(() => {
    if (!formRef.current) return;

    formRef.current.submit();
  }, []);

  return (
    <div className={`form-js-viewer-wrapper ${className}`}>
      <div
        ref={containerRef}
        className="form-js-container"
        style={{ minHeight: '200px' }}
      />

      {onSubmit && !readonly && (
        <div className="mt-4 flex justify-end">
          <button
            type="button"
            onClick={handleSubmit}
            className="inline-flex items-center justify-center rounded-md bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground shadow hover:bg-primary/90 transition-colors"
          >
            Submit
          </button>
        </div>
      )}
    </div>
  );
}
