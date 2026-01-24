<template>
  <v-container fluid class="fill-height pa-0">
    <v-row no-gutters class="fill-height">
      <!-- Chat sidebar -->
      <v-col cols="12" md="4" lg="3">
        <v-card height="100vh" rounded="0">
          <v-card-title class="bg-primary">
            <v-toolbar color="primary" dark flat>
              <v-toolbar-title>Conversations</v-toolbar-title>
            </v-toolbar>
          </v-card-title>
          <v-card-text class="pa-0">
            <v-list>
              <!-- Placeholder conversation items -->
              <v-list-item
                v-for="n in 5"
                :key="n"
                :title="`Conversation ${n}`"
                :subtitle="`Last message preview...`"
              >
                <template v-slot:prepend>
                  <v-avatar color="primary">
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
        <v-card height="100vh" rounded="0">
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
                Messages will appear here
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
                  >
                    <template v-slot:append>
                      <v-btn
                        icon="mdi-send"
                        color="primary"
                        variant="text"
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
// Chat view component - no business logic
</script>

<style scoped>
</style>
