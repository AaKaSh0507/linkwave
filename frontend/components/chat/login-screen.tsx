"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { PhoneInput } from "./phone-input";
import { OtpInput } from "./otp-input";
import { MessageCircle, ArrowLeft, Loader2 } from "lucide-react";
import { authApi } from "@/lib/api";
import { useChatStore } from "@/lib/chat-store";

type LoginStep = "phone" | "otp";

export function LoginScreen() {
  const [step, setStep] = useState<LoginStep>("phone");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [countdown, setCountdown] = useState(0);

  const { setUser, setIsAuthenticated } = useChatStore();

  const handleRequestOtp = async () => {
    if (!phone || phone.replace(/\D/g, "").length < 10) {
      setError("Please enter a valid phone number");
      return;
    }

    if (!email || !email.includes("@")) {
      setError("Please enter a valid email address");
      return;
    }

    setIsLoading(true);
    setError(null);

    const result = await authApi.requestOtp({ phoneNumber: phone, email });

    if (result.success) {
      setStep("otp");
      // Start countdown for resend
      setCountdown(60);
      const interval = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(interval);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      setError(result.error || "Failed to send OTP. Please try again.");
    }

    setIsLoading(false);
  };

  const handleVerifyOtp = async () => {
    if (otp.length !== 6) {
      setError("Please enter the complete 6-digit code");
      return;
    }

    setIsLoading(true);
    setError(null);

    const result = await authApi.verifyOtp({ phoneNumber: phone, otp });

    if (result.success && result.data?.authenticated) {
      // Fetch user profile after successful verification
      const sessionResult = await authApi.getSession();
      if (sessionResult.success && sessionResult.data?.user) {
        setUser(sessionResult.data.user);
        setIsAuthenticated(true);
      } else {
        setError("Failed to load user profile. Please try again.");
      }
    } else {
      setError(result.error || "Invalid code. Please try again.");
      setOtp("");
    }

    setIsLoading(false);
  };

  const handleBack = () => {
    setStep("phone");
    setOtp("");
    setError(null);
  };

  const handleResendOtp = async () => {
    if (countdown > 0) return;
    await handleRequestOtp();
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="text-center space-y-4">
          <div className="mx-auto w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center">
            <MessageCircle className="w-8 h-8 text-primary" />
          </div>
          <div>
            <CardTitle className="text-2xl font-bold text-balance">
              {step === "phone" ? "Welcome Back" : "Verify Your Number"}
            </CardTitle>
            <CardDescription className="mt-2 text-pretty">
              {step === "phone"
                ? "Enter your phone number and email to sign in or create an account"
                : `We sent a verification code to ${email}`}
            </CardDescription>
          </div>
        </CardHeader>

        <CardContent className="space-y-6">
          {error && (
            <div className="p-3 text-sm text-destructive bg-destructive/10 rounded-lg text-center">
              {error}
            </div>
          )}

          {step === "phone" ? (
            <>
              <div className="space-y-2">
                <label htmlFor="phone" className="text-sm font-medium text-foreground">
                  Phone Number
                </label>
                <PhoneInput
                  value={phone}
                  onChange={setPhone}
                  disabled={isLoading}
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="email" className="text-sm font-medium text-foreground">
                  Email Address
                </label>
                <Input
                  id="email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={isLoading}
                  className="h-12 text-base"
                />
              </div>

              <Button
                className="w-full h-12 text-base font-medium"
                onClick={handleRequestOtp}
                disabled={isLoading || !phone || !email}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Sending Code...
                  </>
                ) : (
                  "Continue"
                )}
              </Button>

              <p className="text-xs text-muted-foreground text-center text-pretty">
                By continuing, you agree to receive a verification code via email.
              </p>
            </>
          ) : (
            <>
              <button
                type="button"
                onClick={handleBack}
                className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
                <ArrowLeft className="w-4 h-4" />
                Change number
              </button>

              <div className="space-y-4">
                <OtpInput
                  value={otp}
                  onChange={setOtp}
                  disabled={isLoading}
                />

                <Button
                  className="w-full h-12 text-base font-medium"
                  onClick={handleVerifyOtp}
                  disabled={isLoading || otp.length !== 6}
                >
                  {isLoading ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Verifying...
                    </>
                  ) : (
                    "Verify"
                  )}
                </Button>
              </div>

              <div className="text-center">
                <p className="text-sm text-muted-foreground">
                  {"Didn't receive a code? "}
                  {countdown > 0 ? (
                    <span>Resend in {countdown}s</span>
                  ) : (
                    <button
                      type="button"
                      onClick={handleResendOtp}
                      className="text-primary hover:underline font-medium"
                      disabled={isLoading}
                    >
                      Resend
                    </button>
                  )}
                </p>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
