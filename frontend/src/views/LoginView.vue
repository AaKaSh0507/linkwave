<template>
  <v-container fluid class="fill-height">
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="6" lg="4">
        <v-card elevation="8">
          <v-card-title class="text-h4 text-center pa-6">
            Linkwave Login
          </v-card-title>
          <v-card-text class="pa-6">
            <v-form>
              <v-text-field
                label="Username"
                prepend-icon="mdi-account"
                variant="outlined"
                class="mb-4"
              />
              <v-text-field
                label="Password"
                prepend-icon="mdi-lock"
                type="password"
                variant="outlined"
                class="mb-4"
              />
              <v-btn
                color="primary"
                size="large"
                block
              >
                Login
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
// Login view component - no business logic
</script>

<style scoped>
</style>
