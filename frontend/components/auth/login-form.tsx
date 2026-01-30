'use client'

import React from "react"

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/lib/auth-context'

interface LoginFormProps {
  onOTPSent: (otpId: string, phoneNumber: string) => void
}

export function LoginForm({ onOTPSent }: LoginFormProps) {
  const [phoneNumber, setPhoneNumber] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  const { requestOTP } = useAuth()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setIsLoading(true)

    try {
      // Validate phone number
      const cleanPhone = phoneNumber.replace(/\D/g, '')
      if (cleanPhone.length < 10) {
        setError('Please enter a valid phone number')
        setIsLoading(false)
        return
      }

      const result = await requestOTP(phoneNumber)
      onOTPSent(result.otpId, phoneNumber)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to request OTP')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Card className="w-full max-w-md border-2 border-primary/20 shadow-lg">
      <CardHeader className="bg-gradient-to-br from-primary/5 to-accent/5">
        <CardTitle className="text-2xl text-primary">Welcome to Messenger</CardTitle>
        <CardDescription>Enter your phone number to get started</CardDescription>
      </CardHeader>
      <CardContent className="pt-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="phone" className="text-foreground font-medium">
              Phone Number
            </Label>
            <Input
              id="phone"
              type="tel"
              placeholder="+1 (555) 000-0000"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              disabled={isLoading}
              className="text-base border-2 border-primary/20 focus:border-primary"
            />
          </div>

          {error && <div className="text-sm text-destructive font-medium">{error}</div>}

          <Button
            type="submit"
            disabled={isLoading || !phoneNumber.trim()}
            className="w-full bg-primary hover:bg-primary/90 text-primary-foreground font-semibold h-10"
          >
            {isLoading ? 'Sending...' : 'Send OTP'}
          </Button>

          <p className="text-xs text-muted-foreground text-center">
            We'll send a verification code to your phone number
          </p>
        </form>
      </CardContent>
    </Card>
  )
}
