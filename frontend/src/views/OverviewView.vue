<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { fetchOverview } from "../api/client";
import { fetchSessionStats } from "../api/sessions";
import { REQUEST_MESSAGES } from "../constants/messages";
import { createFallbackOverview } from "../state/dashboard";
import type { OverviewResponse, SessionStats } from "../types";
import FeatureStrip from "../components/FeatureStrip.vue";
import MetricGrid from "../components/MetricGrid.vue";
import OperationsTable from "../components/OperationsTable.vue";

const overview = ref<OverviewResponse>(createFallbackOverview());
const notice = ref(REQUEST_MESSAGES.overviewFallback);
const stats = ref<SessionStats | null>(null);

onMounted(async () => {
  try {
    overview.value = await fetchOverview();
    notice.value = "后端服务已联通，当前展示实时接口数据。";
  } catch {
    notice.value = REQUEST_MESSAGES.overviewFallback;
  }
  try {
    stats.value = await fetchSessionStats();
  } catch {
    // 场次指标加载失败不影响总览主体
  }
});
</script>

<template>
  <section class="workspace">
    <div class="lead-grid">
      <article class="hero-panel">
        <span class="pill">{{ notice }}</span>
        <h2>{{ overview.appName }}</h2>
        <p>{{ overview.description }}</p>
        <RouterLink class="hero-entry" to="/sessions">
          进入场次报名与候补队列 →
        </RouterLink>
      </article>
      <MetricGrid :items="overview.kpis" />
    </div>

    <section v-if="stats" class="session-stats-panel" aria-label="场次实时指标">
      <div class="session-stat">
        <span>报名中场次</span>
        <strong>{{ stats.scheduledSessions }}</strong>
        <small>可报名 {{ stats.availableSessions }} 场 · 满员 {{ stats.fullSessions }} 场</small>
      </div>
      <div class="session-stat">
        <span>有效报名人次</span>
        <strong>{{ stats.totalRegistered }}</strong>
        <small>候补队列 {{ stats.totalWaitlisted }} 人次</small>
      </div>
      <div class="session-stat">
        <span>平均上座率</span>
        <strong>{{ stats.averageFillRate }}%</strong>
        <small>全部场次口径</small>
      </div>
      <div class="session-stat">
        <span>已开始 / 已取消</span>
        <strong>{{ stats.startedSessions }} / {{ stats.cancelledSessions }}</strong>
        <small>共 {{ stats.totalSessions }} 场</small>
      </div>
    </section>

    <FeatureStrip :items="overview.features" />
    <section class="work-panel">
      <h2>运营任务流</h2>
      <OperationsTable :records="overview.records" />
    </section>
  </section>
</template>
