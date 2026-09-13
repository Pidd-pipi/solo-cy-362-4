<script setup lang="ts">
import { ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { fetchWaitlist } from "../api/sessions";
import type { RegistrationItem, SessionItem } from "../types";

const props = defineProps<{ modelValue: boolean; session: SessionItem | null }>();
const emit = defineEmits<{ "update:modelValue": [value: boolean] }>();

const loading = ref(false);
const rows = ref<RegistrationItem[]>([]);

watch(
  () => [props.modelValue, props.session?.id] as const,
  async ([open]) => {
    if (open && props.session) {
      loading.value = true;
      try {
        rows.value = await fetchWaitlist(props.session.id);
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
    :title="session ? `候补队列 · ${session.scriptName}` : '候补队列'"
    width="520px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-alert
      v-if="session"
      :closable="false"
      type="info"
      :title="`名额 ${session.capacity} 人 · 已报名 ${session.registeredCount} 人 · 候补 ${session.waitlistCount} 人`"
      description="有人取消时，顺位第 1 位自动转正，其余顺位自动前移。"
      style="margin-bottom: 14px"
    />
    <el-table v-loading="loading" :data="rows" size="large" empty-text="候补队列为空">
      <el-table-column label="顺位" width="80">
        <template #default="{ row }">
          <el-tag :type="row.waitlistPosition === 1 ? 'danger' : 'info'">
            第 {{ row.waitlistPosition }} 位
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="玩家">
        <template #default="{ row }">
          {{ row.playerName }}
          <small class="text-muted">{{ row.memberLevel }}</small>
        </template>
      </el-table-column>
      <el-table-column prop="playerPhone" label="手机号" width="140" />
    </el-table>
    <template #footer>
      <el-button type="primary" @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>
