<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage } from "element-plus";
import { registerForSession } from "../api/sessions";
import { useNow } from "../composables/useNow";
import { isSessionRegisterable } from "../utils/session";
import type { PlayerItem, RegistrationAction, SessionItem } from "../types";
import { formatStoreDateTime } from "../utils/instant";

const props = defineProps<{
  modelValue: boolean;
  session: SessionItem | null;
  players: PlayerItem[];
  defaultPlayerId: number | null;
}>();
const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  registered: [action: RegistrationAction];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);
const form = reactive({ playerId: null as number | null });

// 弹窗自身的秒级时钟：弹窗打开后跨过开始时间，无需关闭重开即自动截止
const now = useNow(1000);
const canRegister = computed(() =>
  props.session ? isSessionRegisterable(props.session, now.value) : false,
);

const rules: FormRules<typeof form> = {
  playerId: [{ required: true, message: "请选择报名玩家", trigger: "change" }],
};

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      form.playerId = props.defaultPlayerId;
    }
  },
);

async function submit() {
  if (!formRef.value || !props.session) {
    return;
  }
  await formRef.value.validate(async (valid) => {
    if (!valid || form.playerId === null) {
      return;
    }
    // 防御性校验：弹窗打开后场次可能恰好跨过开始时间
    if (!canRegister.value) {
      ElMessage.error("该场次报名已截止，无法报名");
      emit("update:modelValue", false);
      return;
    }
    submitting.value = true;
    try {
      const action = await registerForSession(props.session!.id, form.playerId);
      emit("registered", action);
      emit("update:modelValue", false);
      if (action.result === "WAITLISTED") {
        ElMessage.warning(action.message);
      } else {
        ElMessage.success(action.message);
      }
    } catch (error) {
      ElMessage.error((error as Error).message);
    } finally {
      submitting.value = false;
    }
  });
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="场次报名"
    width="440px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="session" class="register-summary">
      <h3>{{ session.scriptName }}</h3>
      <p>开场时间：{{ formatStoreDateTime(session.startTime) }}</p>
      <p>
        名额：已报 {{ session.registeredCount }} / {{ session.capacity }}，
        剩余 <strong>{{ session.remainingSlots }}</strong> 个空位，候补 {{ session.waitlistCount }} 人
      </p>
      <el-alert
        v-if="session.full"
        type="warning"
        :closable="false"
        title="本场次已满员，报名后将进入候补队列；有人取消时首位候补自动转正。"
        style="margin-top: 8px"
      />
      <el-alert
        v-if="!canRegister"
        type="error"
        :closable="false"
        title="该场次报名已截止（已到开始时间或场次已取消/开始），无法报名或候补。"
        style="margin-top: 8px"
      />
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="72px" style="margin-top: 16px">
      <el-form-item label="玩家" prop="playerId">
        <el-select v-model="form.playerId" placeholder="选择玩家身份" style="width: 100%">
          <el-option
            v-for="player in players"
            :key="player.id"
            :label="`${player.name}（${player.phone} · ${player.memberLevel}）`"
            :value="player.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="!canRegister"
        @click="submit"
      >
        确认报名
      </el-button>
    </template>
  </el-dialog>
</template>
