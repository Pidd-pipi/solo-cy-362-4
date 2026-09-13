<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  cancelRegistration,
  cancelSession,
  fetchPlayerRegistrations,
  fetchPlayers,
  fetchScripts,
  fetchSessions,
  startSession,
} from "../api/sessions";
import { useNow } from "../composables/useNow";
import { isSessionRegisterable } from "../utils/session";
import type {
  PlayerItem,
  RegistrationAction,
  RegistrationItem,
  ScriptItem,
  SessionItem,
  SessionStatusFilter,
} from "../types";
import { formatStoreDateTime } from "../utils/instant";
import StatusTag from "../components/StatusTag.vue";
import CreateSessionDialog from "../components/CreateSessionDialog.vue";
import RegisterDialog from "../components/RegisterDialog.vue";
import WaitlistDialog from "../components/WaitlistDialog.vue";
import MyRegistrationsDialog from "../components/MyRegistrationsDialog.vue";

const loading = ref(false);
const sessions = ref<SessionItem[]>([]);
// 秒级时钟：页面停留跨过开始时间时，报名按钮/提示无需刷新即自动截止
const now = useNow(1000);
// 定时静默刷新，同步名额与候补变化（他人报名/候补转正）
const AUTO_REFRESH_MS = 15000;
let autoRefreshTimer: number | undefined;
const players = ref<PlayerItem[]>([]);
const scripts = ref<ScriptItem[]>([]);
const statusFilter = ref<SessionStatusFilter>("");
const currentPlayerId = ref<number | null>(null);
const myRegistrations = ref<RegistrationItem[]>([]);

const createVisible = ref(false);
const registerVisible = ref(false);
const waitlistVisible = ref(false);
const mineVisible = ref(false);
const activeSessionId = ref<number | null>(null);

// 弹窗始终跟随列表中该场次的最新数据：静默刷新后 registerable/名额会即时同步进弹窗
const activeSession = computed<SessionItem | null>(
  () => sessions.value.find((session) => session.id === activeSessionId.value) ?? null,
);

const currentPlayer = computed(() =>
  players.value.find((player) => player.id === currentPlayerId.value) ?? null,
);

/** 当前玩家在各场次的有效报名：sessionId -> 报名记录 */
const mySessionMap = computed(() => {
  const map = new Map<number, RegistrationItem>();
  for (const item of myRegistrations.value) {
    map.set(item.sessionId, item);
  }
  return map;
});

async function loadPlayersAndScripts() {
  const [playerRows, scriptRows] = await Promise.all([fetchPlayers(), fetchScripts()]);
  players.value = playerRows;
  scripts.value = scriptRows;
  if (!currentPlayerId.value && playerRows.length > 0) {
    currentPlayerId.value = playerRows[0].id;
  }
}

async function loadSessions() {
  loading.value = true;
  try {
    sessions.value = await fetchSessions(statusFilter.value);
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    loading.value = false;
  }
}

async function loadMine() {
  if (currentPlayerId.value === null) {
    myRegistrations.value = [];
    return;
  }
  try {
    myRegistrations.value = await fetchPlayerRegistrations(currentPlayerId.value);
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

async function refreshAll() {
  await Promise.all([loadSessions(), loadMine()]);
}

onMounted(async () => {
  document.addEventListener("visibilitychange", onVisibilityChange);
  startAutoRefresh();
  try {
    await loadPlayersAndScripts();
    await refreshAll();
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
});

onUnmounted(() => {
  document.removeEventListener("visibilitychange", onVisibilityChange);
  stopAutoRefresh();
});

/** 静默刷新：不显示整表 loading、不弹错误，避免停留期间打扰用户 */
async function silentRefresh() {
  if (document.hidden) {
    return;
  }
  try {
    const [rows, mine] = await Promise.all([
      fetchSessions(statusFilter.value),
      currentPlayerId.value === null
        ? Promise.resolve([])
        : fetchPlayerRegistrations(currentPlayerId.value),
    ]);
    sessions.value = rows;
    myRegistrations.value = mine;
  } catch {
    // 静默轮询失败时保留当前数据，等待下一次
  }
}

function startAutoRefresh() {
  stopAutoRefresh();
  autoRefreshTimer = window.setInterval(silentRefresh, AUTO_REFRESH_MS);
}

function stopAutoRefresh() {
  if (autoRefreshTimer !== undefined) {
    window.clearInterval(autoRefreshTimer);
    autoRefreshTimer = undefined;
  }
}

function onVisibilityChange() {
  if (document.hidden) {
    stopAutoRefresh();
  } else {
    void silentRefresh();
    startAutoRefresh();
  }
}

function myStatusOf(session: SessionItem): RegistrationItem | undefined {
  return mySessionMap.value.get(session.id);
}

/** 实时可报名：服务端标记 + 本地时钟未到开始时间，跨过开始时刻立即变 false */
function isRegisterable(session: SessionItem): boolean {
  return isSessionRegisterable(session, now.value);
}

/** 报名按钮文案：先看截止，再看满员。 */
function registerLabel(session: SessionItem): string {
  if (!isRegisterable(session)) {
    return "报名已截止";
  }
  return session.full ? "报名（候补）" : "立即报名";
}

function openRegister(session: SessionItem) {
  activeSessionId.value = session.id;
  registerVisible.value = true;
}

function openWaitlist(session: SessionItem) {
  activeSessionId.value = session.id;
  waitlistVisible.value = true;
}

async function onRegistered(action: RegistrationAction) {
  if (action.playerId === currentPlayerId.value) {
    await refreshAll();
  } else {
    await loadSessions();
  }
}

async function onCancelRegistration(session: SessionItem) {
  if (currentPlayerId.value === null) {
    return;
  }
  const mine = myStatusOf(session);
  const actionText = mine?.status === "WAITLISTED" ? "取消候补" : "取消报名";
  try {
    await ElMessageBox.confirm(
      `确认${actionText}「${session.scriptName}」吗？${
        mine?.status === "REGISTERED" && session.waitlistCount > 0
          ? "取消后首位候补玩家将自动转正。"
          : ""
      }`,
      actionText,
      { type: "warning", confirmButtonText: actionText, cancelButtonText: "再想想" },
    );
  } catch {
    return;
  }
  try {
    const result = await cancelRegistration(session.id, currentPlayerId.value);
    if (result.promotedPlayerName) {
      ElMessage.success(result.message);
    } else {
      ElMessage.info(result.message);
    }
    await refreshAll();
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

async function onStart(session: SessionItem) {
  try {
    await ElMessageBox.confirm(`确认开场「${session.scriptName}」？开场后不能再报名。`, "开场", {
      type: "warning",
      confirmButtonText: "确认开场",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await startSession(session.id);
    ElMessage.success("场次已开始");
    await refreshAll();
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

async function onCancelSession(session: SessionItem) {
  try {
    await ElMessageBox.confirm(
      `确认取消场次「${session.scriptName}」？全部报名与候补将同步失效。`,
      "取消场次",
      { type: "error", confirmButtonText: "取消场次", cancelButtonText: "保留" },
    );
  } catch {
    return;
  }
  try {
    await cancelSession(session.id);
    ElMessage.success("场次已取消");
    await refreshAll();
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

function percentOf(session: SessionItem): number {
  if (session.capacity <= 0) {
    return 0;
  }
  return Math.min(100, Math.round((session.registeredCount / session.capacity) * 100));
}
</script>

<template>
  <section class="workspace">
    <article class="work-panel session-toolbar">
      <div>
        <h2 class="panel-title">场次报名与候补队列</h2>
        <p class="panel-subtitle">门店排期创建场次，玩家在线报名；满员自动进入候补，取消即顺位转正。</p>
      </div>
      <div class="toolbar-actions">
        <el-select
          v-model="currentPlayerId"
          placeholder="选择玩家身份"
          style="width: 220px"
          @change="loadMine"
        >
          <el-option
            v-for="player in players"
            :key="player.id"
            :label="`${player.name}（${player.memberLevel}）`"
            :value="player.id"
          />
        </el-select>
        <el-button @click="refreshAll">刷新</el-button>
        <el-button @click="mineVisible = true" :disabled="currentPlayerId === null">我的报名</el-button>
        <el-button type="primary" @click="createVisible = true">创建场次</el-button>
      </div>
    </article>

    <article class="work-panel">
      <div class="filter-row">
        <el-radio-group v-model="statusFilter" @change="loadSessions">
          <el-radio-button value="">全部场次</el-radio-button>
          <el-radio-button value="SCHEDULED">报名中</el-radio-button>
          <el-radio-button value="STARTED">已开始</el-radio-button>
          <el-radio-button value="CANCELLED">已取消</el-radio-button>
        </el-radio-group>
      </div>

      <el-table v-loading="loading" :data="sessions" style="width: 100%" row-key="id">
        <el-table-column label="剧本场次" min-width="210">
          <template #default="{ row }">
            <strong>{{ row.scriptName }}</strong>
            <div class="text-muted">{{ row.genre }} · #{{ row.id }}</div>
          </template>
        </el-table-column>
        <el-table-column label="DM" width="110">
          <template #default="{ row }">{{ row.dmName ?? "待指派" }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ formatStoreDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="报名名额" width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="percentOf(row)"
              :status="row.full ? 'exception' : undefined"
              :stroke-width="14"
              :text-inside="true"
            />
            <small class="text-muted">
              已报 {{ row.registeredCount }}/{{ row.capacity }} · 余 {{ row.remainingSlots }}
            </small>
          </template>
        </el-table-column>
        <el-table-column label="候补" width="120">
          <template #default="{ row }">
            <el-button link :type="row.waitlistCount > 0 ? 'warning' : 'info'" @click="openWaitlist(row)">
              {{ row.waitlistCount > 0 ? `候补 ${row.waitlistCount} 人` : "无候补" }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
            <div v-if="row.status === 'SCHEDULED' && !isRegisterable(row)" class="cutoff-hint">
              报名已截止
            </div>
          </template>
        </el-table-column>
        <el-table-column label="我的状态 / 操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <template v-if="myStatusOf(row)?.status === 'REGISTERED'">
              <el-tag type="success" size="small" style="margin-right: 8px">已报名</el-tag>
              <el-button
                v-if="row.status === 'SCHEDULED'"
                size="small"
                type="danger"
                plain
                @click="onCancelRegistration(row)"
              >
                取消报名
              </el-button>
            </template>
            <template v-else-if="myStatusOf(row)?.status === 'WAITLISTED'">
              <el-tag type="warning" size="small" style="margin-right: 8px">
                候补第 {{ myStatusOf(row)?.waitlistPosition }} 位
              </el-tag>
              <el-button
                v-if="row.status === 'SCHEDULED'"
                size="small"
                type="danger"
                plain
                @click="onCancelRegistration(row)"
              >
                取消候补
              </el-button>
            </template>
            <template v-else>
              <el-button
                size="small"
                type="primary"
                :disabled="!isRegisterable(row)"
                @click="openRegister(row)"
              >
                {{ registerLabel(row) }}
              </el-button>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="门店操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'SCHEDULED'">
              <el-button size="small" @click="onStart(row)">开场</el-button>
              <el-button size="small" type="danger" plain @click="onCancelSession(row)">
                取消场次
              </el-button>
            </template>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无符合条件的场次" />
        </template>
      </el-table>
    </article>

    <CreateSessionDialog v-model="createVisible" :scripts="scripts" @saved="refreshAll" />
    <RegisterDialog
      v-model="registerVisible"
      :session="activeSession"
      :players="players"
      :default-player-id="currentPlayerId"
      @registered="onRegistered"
    />
    <WaitlistDialog v-model="waitlistVisible" :session="activeSession" />
    <MyRegistrationsDialog v-model="mineVisible" :player="currentPlayer" />
  </section>
</template>
