<template>
  <v-app>
    <v-main>
      <!-- Application content will go here -->
      <router-view v-if="false" />
      <!-- Placeholder: Router not yet configured -->
      <v-container class="fill-height">
        <v-row align="center" justify="center">
          <v-col cols="12" class="text-center">
            <h1 class="text-h3 mb-4">Linkwave</h1>
            <p class="text-subtitle-1">Realtime Chat Application</p>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
// Application root component
</script>

<style scoped>
</style>
