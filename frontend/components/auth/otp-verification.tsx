'use client'

import React from "react"

import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { InputOTP, InputOTPGroup, InputOTPSlot } from '@/components/ui/input-otp'
import { useAuth } from '@/lib/auth-context'

interface OTPVerificationProps {
  otpId: string
  phoneNumber: string
  onVerified: () => void
  onBack: () => void
}

export function OTPVerification({ otpId, phoneNumber, onVerified, onBack }: OTPVerificationProps) {
  const [otp, setOtp] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  const [timeLeft, setTimeLeft] = useState(300) // 5 minutes
  const { verifyOTP } = useAuth()

  // Countdown timer
  useEffect(() => {
    if (timeLeft <= 0) return

    const interval = setInterval(() => {
      setTimeLeft((prev) => prev - 1)
    }, 1000)

    return () => clearInterval(interval)
  }, [timeLeft])

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (otp.length !== 6) {
      setError('Please enter a 6-digit code')
      return
    }

    setIsLoading(true)

    try {
      await verifyOTP(otpId, otp)
      onVerified()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Verification failed')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Card className="w-full max-w-md border-2 border-primary/20 shadow-lg">
      <CardHeader className="bg-gradient-to-br from-primary/5 to-accent/5">
        <CardTitle className="text-2xl text-primary">Verify Your Number</CardTitle>
        <CardDescription>We sent a code to {phoneNumber}</CardDescription>
      </CardHeader>
      <CardContent className="pt-6">
        <form onSubmit={handleVerify} className="space-y-6">
          <div className="space-y-3">
            <Label className="text-foreground font-medium">Enter verification code</Label>
            <div className="flex justify-center">
              <InputOTP value={otp} onChange={setOtp} maxLength={6}>
                <InputOTPGroup className="gap-2">
                  <InputOTPSlot
                    index={0}
                    className="w-12 h-12 text-lg border-2 border-primary/20 rounded-lg"
                  />
                  <InputOTPSlot
                    index={1}
                    className="w-12 h-12 text-lg border-2 border-primary/20 rounded-lg"
                  />
                  <InputOTPSlot
                    index={2}
                    className="w-12 h-12 text-lg border-2 border-primary/20 rounded-lg"
                  />
                  <InputOTPSlot
                    index={3}
                    className="w-12 h-12 text-lg border-2 border-primary/20 rounded-lg"
                  />
                  <InputOTPSlot
                    index={4}
                    className="w-12 h-12 text-lg border-2 border-primary/20 rounded-lg"
                  />
                  <InputOTPSlot
                    index={5}
                    className="w-12 h-12 text-lg border-2 border-primary/20 rounded-lg"
                  />
                </InputOTPGroup>
              </InputOTP>
            </div>
          </div>

          {error && <div className="text-sm text-destructive font-medium text-center">{error}</div>}

          <div className="text-center">
            <p className="text-sm text-muted-foreground">
              Code expires in <span className="font-semibold text-foreground">{formatTime(timeLeft)}</span>
            </p>
          </div>

          <div className="space-y-2">
            <Button
              type="submit"
              disabled={isLoading || otp.length !== 6 || timeLeft <= 0}
              className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-semibold h-10"
            >
              {isLoading ? 'Verifying...' : 'Verify'}
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={onBack}
              disabled={isLoading}
              className="w-full bg-transparent"
            >
              Back
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
