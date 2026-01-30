'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { LoginForm } from '@/components/auth/login-form'
import { OTPVerification } from '@/components/auth/otp-verification'
import { useAuth } from '@/lib/auth-context'

type AuthStep = 'login' | 'otp'

export default function AuthPage() {
  const router = useRouter()
  const { isAuthenticated, isLoading } = useAuth()
  const [step, setStep] = useState<AuthStep>('login')
  const [otpId, setOtpId] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')

  // Redirect if already authenticated
  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.push('/chat')
    }
  }, [isAuthenticated, isLoading, router])

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-secondary/30 to-background">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-primary/30 border-t-primary rounded-full animate-spin mx-auto mb-4" />
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-background via-secondary/30 to-background">
      <div className="w-full">
        <div className="flex flex-col items-center gap-8">
          {/* Logo/Header */}
          <div className="text-center">
            <h1 className="text-4xl font-bold text-primary mb-2">Messenger</h1>
            <p className="text-muted-foreground">Stay connected with instant messaging</p>
          </div>

          {/* Auth Forms */}
          {step === 'login' ? (
            <LoginForm
              onOTPSent={(id, phone) => {
                setOtpId(id)
                setPhoneNumber(phone)
                setStep('otp')
              }}
            />
          ) : (
            <OTPVerification
              otpId={otpId}
              phoneNumber={phoneNumber}
              onVerified={() => {
                router.push('/chat')
              }}
              onBack={() => setStep('login')}
            />
          )}

          {/* Footer */}
          <p className="text-xs text-muted-foreground text-center max-w-md">
            By signing in, you agree to our Terms of Service and Privacy Policy
          </p>
        </div>
      </div>
    </div>
  )
}
