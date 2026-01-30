'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { getAuthToken } from '@/lib/api'

export default function RootPage() {
  const router = useRouter()

  useEffect(() => {
    const token = getAuthToken()
    if (token) {
      router.push('/chat')
    } else {
      router.push('/auth')
    }
  }, [router])

  return null
}
