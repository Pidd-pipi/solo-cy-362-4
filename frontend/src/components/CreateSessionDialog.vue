<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage } from "element-plus";
import { createSession } from "../api/sessions";
import { STORE_TIME_ZONE } from "../constants/app";
import type { ScriptItem } from "../types";

const props = defineProps<{ modelValue: boolean; scripts: ScriptItem[] }>();
const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  saved: [];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  scriptId: null as number | null,
  dmName: "",
  startTime: null as number | null,
  capacity: 6,
  operator: "门店管理员",
});

const selectedScript = computed(() =>
  props.scripts.find((script) => script.id === form.scriptId) ?? null,
);

const rules: FormRules<typeof form> = {
  scriptId: [{ required: true, message: "请选择剧本", trigger: "change" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  capacity: [{ required: true, message: "请填写人数上限", trigger: "blur" }],
};

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      form.scriptId = props.scripts[0]?.id ?? null;
      form.dmName = "";
      form.startTime = null;
      form.capacity = props.scripts[0]?.minPlayers ?? 6;
      form.operator = "门店管理员";
    }
  },
);

function close() {
  emit("update:modelValue", false);
}

async function submit() {
  if (!formRef.value) {
    return;
  }
  await formRef.value.validate(async (valid) => {
    if (!valid || form.scriptId === null || form.startTime === null) {
      return;
    }
    submitting.value = true;
    try {
      // 选择器给出的毫秒时间戳是确定的绝对时刻，toISOString 带 Z 提交，
      // 由后端按门店统一时区换算为门店墙钟时间，跨浏览器时区结果一致
      const iso = new Date(form.startTime).toISOString();
      await createSession({
        scriptId: form.scriptId,
        dmName: form.dmName.trim() || null,
        startTime: iso,
        capacity: form.capacity,
        operator: form.operator,
      });
      ElMessage.success("场次创建成功");
      emit("saved");
      close();
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
    title="创建场次"
    width="460px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="剧本" prop="scriptId">
        <el-select v-model="form.scriptId" placeholder="选择剧本" style="width: 100%">
          <el-option
            v-for="script in scripts"
            :key="script.id"
            :label="`${script.name}（${script.minPlayers}-${script.maxPlayers}人 · ${script.genre}）`"
            :value="script.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          placeholder="选择开场日期与时间"
          format="YYYY-MM-DD HH:mm"
          value-format="x"
          style="width: 100%"
        />
        <div class="form-hint">按门店时区（{{ STORE_TIME_ZONE }}）排期，提交后统一换算</div>
      </el-form-item>
      <el-form-item label="主持 DM">
        <el-input v-model="form.dmName" maxlength="80" placeholder="可留空，稍后指派" />
      </el-form-item>
      <el-form-item label="人数上限" prop="capacity">
        <el-input-number v-model="form.capacity" :min="1" :max="50" />
        <span v-if="selectedScript" class="form-hint">
          剧本要求 {{ selectedScript.minPlayers }}-{{ selectedScript.maxPlayers }} 人
        </span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>
