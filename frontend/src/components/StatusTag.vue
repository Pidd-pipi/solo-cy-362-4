<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{ status: string }>();

const meta = computed(() => {
  switch (props.status) {
    case "SCHEDULED":
      return { label: "报名中", type: "success" as const };
    case "STARTED":
      return { label: "已开始", type: "info" as const };
    case "CANCELLED":
      return { label: "已取消", type: "danger" as const };
    case "REGISTERED":
      return { label: "已报名", type: "success" as const };
    case "WAITLISTED":
      return { label: "候补中", type: "warning" as const };
    default:
      return { label: props.status, type: "info" as const };
  }
});
</script>

<template>
  <el-tag :type="meta.type" disable-transitions effect="light">{{ meta.label }}</el-tag>
</template>
