<script setup lang="ts">
import { APP_CODE, APP_NAME } from "./constants/app";
import { REQUEST_MESSAGES } from "./constants/messages";

function goHealth() {
  window.location.href = REQUEST_MESSAGES.healthPath;
}
</script>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div class="topbar-brand">
        <span class="brand-code">{{ APP_CODE }}</span>
        <h1 class="brand-title">{{ APP_NAME }}</h1>
      </div>
      <nav class="topnav" aria-label="主导航">
        <router-link class="topnav-link" to="/">运营总览</router-link>
        <router-link class="topnav-link" to="/sessions">场次报名</router-link>
      </nav>
      <el-button type="primary" plain @click="goHealth">API Health</el-button>
    </header>
    <router-view />
  </main>
</template>
