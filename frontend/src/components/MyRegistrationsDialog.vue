<script setup lang="ts">
import { ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { fetchPlayerRegistrations } from "../api/sessions";
import { formatStoreTimeShort } from "../utils/instant";
import type { PlayerItem, RegistrationItem } from "../types";
import StatusTag from "./StatusTag.vue";

const props = defineProps<{ modelValue: boolean; player: PlayerItem | null }>();
const emit = defineEmits<{ "update:modelValue": [value: boolean] }>();

const loading = ref(false);
const rows = ref<RegistrationItem[]>([]);

watch(
  () => [props.modelValue, props.player?.id] as const,
  async ([open]) => {
    if (open && props.player) {
      loading.value = true;
      try {
        rows.value = await fetchPlayerRegistrations(props.player.id);
      } catch (error) {
        ElMessage.error((error as Error).message);
        rows.value = [];
      } finally {
        loading.value = false;
      }
    }
  },
);
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="player ? `${player.name} 的报名记录` : '我的报名'"
    width="580px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-table v-loading="loading" :data="rows" size="large" empty-text="暂无有效报名或候补">
      <el-table-column label="剧本 / 开场">
        <template #default="{ row }">
          <strong>{{ row.scriptName }}</strong>
          <div class="text-muted">{{ formatStoreTimeShort(row.sessionStartTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
          <div v-if="row.status === 'WAITLISTED'" class="waitlist-pos">
            候补第 {{ row.waitlistPosition }} 位
          </div>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button type="primary" @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>
