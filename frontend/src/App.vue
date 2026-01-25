<template>
  <v-app>
    <v-main>
      <router-view v-if="!isInitializing" />
      
      <!-- Loading spinner during session check -->
      <v-container v-else class="fill-height">
        <v-row align="center" justify="center">
          <v-col cols="12" class="text-center">
            <v-progress-circular
              indeterminate
              color="primary"
              size="64"
            />
            <p class="mt-4">Loading...</p>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()
const isInitializing = ref(true)

// Check session on app mount
onMounted(async () => {
  try {
    // Initialize CSRF token
    await authStore.initializeCsrf()
    
    // Check if user has valid session
    const isAuthenticated = await authStore.checkSession()
    
    // Route user based on authentication status
    if (isAuthenticated && router.currentRoute.value.name === 'Login') {
      router.push({ name: 'Chat' })
    } else if (!isAuthenticated && router.currentRoute.value.meta.requiresAuth) {
      router.push({ name: 'Login' })
    }
  } catch (error) {
    // On error, redirect to login if on protected route
    if (router.currentRoute.value.meta.requiresAuth) {
      router.push({ name: 'Login' })
    }
  } finally {
    isInitializing.value = false
  }
})
</script>

<style scoped>
.fill-height {
  min-height: 100vh;
}
</style>
