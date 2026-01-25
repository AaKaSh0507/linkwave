<template>
  <v-container fluid class="fill-height">
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="6" lg="4">
        <v-card elevation="8">
          <v-card-title class="text-h4 text-center pa-6">
            <v-icon size="x-large" class="mr-2">mdi-message-text</v-icon>
            Linkwave
          </v-card-title>
          
          <v-card-text class="pa-6">
            <!-- Error Alert -->
            <v-alert
              v-if="authStore.error"
              type="error"
              variant="tonal"
              closable
              @click:close="authStore.error = null"
              class="mb-4"
            >
              {{ authStore.error }}
            </v-alert>

            <!-- Success Alert -->
            <v-alert
              v-if="successMessage"
              type="success"
              variant="tonal"
              closable
              @click:close="successMessage = null"
              class="mb-4"
            >
              {{ successMessage }}
            </v-alert>

            <!-- Step 1: Request OTP -->
            <v-form v-if="!otpRequested" @submit.prevent="handleRequestOtp">
              <v-text-field
                v-model="phoneNumber"
                label="Phone Number"
                placeholder="+1234567890"
                prepend-icon="mdi-phone"
                variant="outlined"
                type="tel"
                :rules="phoneRules"
                :disabled="authStore.isLoading"
                class="mb-3"
                required
              />
              
              <v-text-field
                v-model="email"
                label="Email"
                placeholder="your@email.com"
                prepend-icon="mdi-email"
                variant="outlined"
                type="email"
                :rules="emailRules"
                :disabled="authStore.isLoading"
                class="mb-4"
                required
              />
              
              <v-btn
                type="submit"
                color="primary"
                size="large"
                block
                :loading="authStore.isLoading"
                :disabled="!isPhoneValid || !isEmailValid"
              >
                Request OTP
              </v-btn>
            </v-form>

            <!-- Step 2: Verify OTP -->
            <v-form v-else @submit.prevent="handleVerifyOtp">
              <v-alert type="info" variant="tonal" class="mb-4">
                Enter the 6-digit code sent to {{ email }}
              </v-alert>

              <v-text-field
                v-model="otp"
                label="OTP Code"
                placeholder="123456"
                prepend-icon="mdi-lock"
                variant="outlined"
                type="text"
                inputmode="numeric"
                maxlength="6"
                :rules="otpRules"
                :disabled="authStore.isLoading"
                class="mb-4"
                required
                autofocus
              />
              
              <v-btn
                type="submit"
                color="primary"
                size="large"
                block
                :loading="authStore.isLoading"
                :disabled="otp.length !== 6"
                class="mb-2"
              >
                Verify OTP
              </v-btn>

              <v-btn
                variant="text"
                size="large"
                block
                :disabled="authStore.isLoading"
                @click="resetForm"
              >
                Back
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

// Form state
const phoneNumber = ref('')
const email = ref('')
const otp = ref('')
const otpRequested = ref(false)
const successMessage = ref(null)

// Validation rules
const phoneRules = [
  v => !!v || 'Phone number is required',
  v => /^\+?[1-9]\d{1,14}$/.test(v) || 'Invalid phone number (use E.164 format, e.g., +1234567890)'
]

const emailRules = [
  v => !!v || 'Email is required',
  v => /.+@.+\..+/.test(v) || 'Invalid email address'
]

const otpRules = [
  v => !!v || 'OTP is required',
  v => /^\d{6}$/.test(v) || 'OTP must be 6 digits'
]

// Computed validation
const isPhoneValid = computed(() => {
  return phoneNumber.value && /^\+?[1-9]\d{1,14}$/.test(phoneNumber.value)
})

const isEmailValid = computed(() => {
  return email.value && /.+@.+\..+/.test(email.value)
})

// Request OTP
async function handleRequestOtp() {
  authStore.error = null
  successMessage.value = null
  
  const result = await authStore.requestOtp(phoneNumber.value, email.value)
  
  if (result.success) {
    otpRequested.value = true
    successMessage.value = 'OTP sent successfully! Check your email.'
  }
}

// Verify OTP
async function handleVerifyOtp() {
  authStore.error = null
  successMessage.value = null
  
  const result = await authStore.verifyOtp(phoneNumber.value, otp.value)
  
  if (result.success) {
    // Redirect to chat on successful authentication
    router.push({ name: 'Chat' })
  }
}

// Reset form to request OTP again
function resetForm() {
  otpRequested.value = false
  otp.value = ''
  authStore.error = null
  successMessage.value = null
}
</script>

<style scoped>
.fill-height {
  min-height: 100vh;
}
</style>
