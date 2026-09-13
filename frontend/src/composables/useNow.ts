import { onScopeDispose, ref } from "vue";

/**
 * 响应式当前时间，每秒推进一次。
 *
 * 用于场次报名的“到点自动截止”：页面停留期间即使不刷新，跨过开始时间后
 * 依赖 now 的计算属性也会立即重算，按钮与提示同步失效。
 * 组件卸载时自动清理定时器。
 */
export function useNow(intervalMs = 1000) {
  const now = ref(new Date());
  const timer = window.setInterval(() => {
    now.value = new Date();
  }, intervalMs);

  onScopeDispose(() => window.clearInterval(timer));

  return now;
}
