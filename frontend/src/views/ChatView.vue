<template>
  <v-app-bar color="primary" dark>
    <v-toolbar-title>
      <v-icon class="mr-2">mdi-message-text</v-icon>
      Linkwave
    </v-toolbar-title>
    <v-spacer />
    <span class="mr-4">{{ authStore.phoneNumber }}</span>
    <v-btn
      icon="mdi-logout"
      @click="handleLogout"
      :loading="authStore.isLoading"
    />
  </v-app-bar>

  <v-container fluid class="fill-height pa-0">
    <v-row no-gutters class="fill-height">
      <!-- Chat sidebar -->
      <v-col cols="12" md="4" lg="3">
        <v-card height="calc(100vh - 64px)" rounded="0">
          <v-card-title class="bg-primary">
            <v-toolbar color="primary" dark flat>
              <v-toolbar-title>Conversations</v-toolbar-title>
            </v-toolbar>
          </v-card-title>
          <v-card-text class="pa-0">
            <v-alert type="info" variant="tonal" class="ma-4">
              Chat functionality coming in Phase C+
            </v-alert>
            <v-list>
              <!-- Placeholder conversation items -->
              <v-list-item
                v-for="n in 5"
                :key="n"
                :title="`Conversation ${n}`"
                :subtitle="`Last message preview...`"
                disabled
              >
                <template v-slot:prepend>
                  <v-avatar color="grey">
                    <span class="text-white">C{{ n }}</span>
                  </v-avatar>
                </template>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Chat area -->
      <v-col cols="12" md="8" lg="9">
        <v-card height="calc(100vh - 64px)" rounded="0">
          <v-card-title>
            <v-toolbar color="white" flat>
              <v-toolbar-title>Chat Area</v-toolbar-title>
            </v-toolbar>
          </v-card-title>
          <v-divider />
          <v-card-text class="d-flex flex-column fill-height">
            <!-- Messages area -->
            <v-container class="flex-grow-1 overflow-y-auto">
              <div class="text-center text-grey">
                <v-icon size="64" class="mb-4">mdi-message-outline</v-icon>
                <p>Messages will appear here</p>
                <p class="text-caption">Chat functionality coming in Phase C+</p>
              </div>
            </v-container>
            
            <!-- Message input -->
            <v-divider />
            <v-container>
              <v-row no-gutters>
                <v-col>
                  <v-text-field
                    placeholder="Type a message..."
                    variant="outlined"
                    hide-details
                    density="comfortable"
                    disabled
                  >
                    <template v-slot:append>
                      <v-btn
                        icon="mdi-send"
                        color="primary"
                        variant="text"
                        disabled
                      />
                    </template>
                  </v-text-field>
                </v-col>
              </v-row>
            </v-container>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

// Logout handler
async function handleLogout() {
  await authStore.logout()
  router.push({ name: 'Login' })
}
</script>

<style scoped>
.fill-height {
  min-height: 100vh;
}
</style>
