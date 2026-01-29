"use client";

import React from "react"

import { useState } from "react";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface PhoneInputProps {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  placeholder?: string;
  className?: string;
}

export function PhoneInput({
  value,
  onChange,
  disabled,
  placeholder = "+91 98765 43210",
  className,
}: PhoneInputProps) {
  const [focused, setFocused] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = e.target.value;
    
    // Allow only digits, +, spaces, and common phone characters
    // But store only + and digits
    const cleaned = rawValue.replace(/[^\d+\s()-]/g, "");
    
    // Extract just + and digits for storage
    const hasPlus = cleaned.startsWith("+");
    const digits = cleaned.replace(/\D/g, "");
    
    // Limit to reasonable phone number length (max 15 digits per E.164)
    const limitedDigits = digits.slice(0, 15);
    
    onChange(hasPlus ? `+${limitedDigits}` : limitedDigits);
  };

  // Format display value
  const formatForDisplay = (input: string) => {
    if (!input) return "";
    
    const hasPlus = input.startsWith("+");
    const digits = input.replace(/\D/g, "");
    
    if (!digits) return hasPlus ? "+" : "";
    
    // For international numbers (starts with +)
    if (hasPlus) {
      // Indian numbers: +91 XXXXX XXXXX
      if (digits.startsWith("91") && digits.length <= 12) {
        const cc = digits.slice(0, 2);
        const rest = digits.slice(2);
        const p1 = rest.slice(0, 5);
        const p2 = rest.slice(5, 10);
        let result = `+${cc}`;
        if (p1) result += ` ${p1}`;
        if (p2) result += ` ${p2}`;
        return result;
      }
      // US/Canada: +1 XXX XXX XXXX
      if (digits.startsWith("1") && digits.length <= 11) {
        const cc = digits.slice(0, 1);
        const rest = digits.slice(1);
        const p1 = rest.slice(0, 3);
        const p2 = rest.slice(3, 6);
        const p3 = rest.slice(6, 10);
        let result = `+${cc}`;
        if (p1) result += ` ${p1}`;
        if (p2) result += ` ${p2}`;
        if (p3) result += ` ${p3}`;
        return result;
      }
      // Generic: +CC XXX XXX XXXX
      return `+${digits.slice(0, 3)} ${digits.slice(3, 6)} ${digits.slice(6, 9)} ${digits.slice(9)}`.trim().replace(/\s+/g, ' ');
    }
    
    // Local number without country code
    return `${digits.slice(0, 3)} ${digits.slice(3, 6)} ${digits.slice(6, 10)}`.trim().replace(/\s+/g, ' ');
  };

  return (
    <Input
      type="tel"
      value={formatForDisplay(value)}
      onChange={handleChange}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
      disabled={disabled}
      placeholder={placeholder}
      className={cn(
        "h-12 text-base",
        focused && "ring-2 ring-primary/20",
        className
      )}
    />
  );
}
