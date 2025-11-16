import Link from "next/link"

export default function HomePage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-24">
      <div className="max-w-5xl w-full items-center justify-between font-mono text-sm">
        <h1 className="text-6xl font-bold text-center mb-8">
          Welcome to <span className="text-primary">Werkflow</span>
        </h1>
        <p className="text-xl text-center mb-12 text-muted-foreground">
          Visual BPMN Workflow Designer & HR Management Platform
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-12">
          <Link
            href="/studio/processes"
            className="group rounded-lg border border-transparent px-5 py-4 transition-colors hover:border-gray-300 hover:bg-gray-100 dark:hover:border-neutral-700 dark:hover:bg-neutral-800/30"
          >
            <h2 className="mb-3 text-2xl font-semibold">
              Process Studio{" "}
              <span className="inline-block transition-transform group-hover:translate-x-1 motion-reduce:transform-none">
                →
              </span>
            </h2>
            <p className="m-0 max-w-[30ch] text-sm opacity-50">
              Design BPMN workflows visually with drag-and-drop interface
            </p>
          </Link>

          <Link
            href="/studio/forms"
            className="group rounded-lg border border-transparent px-5 py-4 transition-colors hover:border-gray-300 hover:bg-gray-100 dark:hover:border-neutral-700 dark:hover:bg-neutral-800/30"
          >
            <h2 className="mb-3 text-2xl font-semibold">
              Form Builder{" "}
              <span className="inline-block transition-transform group-hover:translate-x-1 motion-reduce:transform-none">
                →
              </span>
            </h2>
            <p className="m-0 max-w-[30ch] text-sm opacity-50">
              Create dynamic forms and link them to workflow tasks
            </p>
          </Link>

          <Link
            href="/portal/tasks"
            className="group rounded-lg border border-transparent px-5 py-4 transition-colors hover:border-gray-300 hover:bg-gray-100 dark:hover:border-neutral-700 dark:hover:bg-neutral-800/30"
          >
            <h2 className="mb-3 text-2xl font-semibold">
              My Tasks{" "}
              <span className="inline-block transition-transform group-hover:translate-x-1 motion-reduce:transform-none">
                →
              </span>
            </h2>
            <p className="m-0 max-w-[30ch] text-sm opacity-50">
              View and complete your assigned workflow tasks
            </p>
          </Link>
        </div>

        <div className="mt-16 text-center text-sm text-muted-foreground">
          <p>Built with Next.js 14, React 18, Tailwind CSS, bpmn-js, and Form.io</p>
          <p className="mt-2">Backend: Spring Boot 3 + Flowable BPM + Keycloak</p>
        </div>
      </div>
    </div>
  )
}
