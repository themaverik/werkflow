import { AppShell } from "@/components/layout/app-shell"

export default function ProcurementLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <AppShell>
      {children}
    </AppShell>
  )
}
