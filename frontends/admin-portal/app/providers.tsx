'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SessionProvider, useSession } from 'next-auth/react'
import { useEffect, useState } from 'react'
import { setApiClientToken } from '@/lib/api/client'

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000, // 1 minute
            refetchOnWindowFocus: false,
          },
        },
      })
  )

  return (
    <SessionProvider>
      <QueryClientProvider client={queryClient}>
        <ApiTokenProvider>{children}</ApiTokenProvider>
      </QueryClientProvider>
    </SessionProvider>
  )
}

// Separate component to handle API client token initialization
function ApiTokenProvider({ children }: { children: React.ReactNode }) {
  const { data: session } = useSession()

  useEffect(() => {
    // Set up the token getter for the API client
    setApiClientToken(async () => {
      return session?.accessToken || null
    })
  }, [session])

  return <>{children}</>
}
