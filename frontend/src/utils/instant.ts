import { STORE_TIME_ZONE } from "../constants/app";

/**
 * 时间基准工具：场次开始时间端到端表示为“同一绝对时刻”。
 *
 * 后端下发的是带偏移的 ISO-8601（如 2026-09-13T19:00:00+08:00），
 * 这里统一解析为 epoch 毫秒做比较，使任意浏览器时区得到一致的截止结论；
 * 展示则固定按门店时区渲染，避免因用户所在时区不同而显示不同开场时间。
 */

/** 解析带偏移/无偏移 ISO 串为绝对毫秒；非法返回 NaN。 */
export function parseInstant(value: string | null | undefined): number {
  if (!value) {
    return NaN;
  }
  return new Date(value).getTime();
}

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

/** 使用 Intl 按门店时区取出可格式化的时间分量。 */
function storeParts(value: string | Date): Record<string, string> {
  const date = typeof value === "string" ? new Date(value) : value;
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: STORE_TIME_ZONE,
    hour12: false,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).formatToParts(date).reduce<Record<string, string>>((acc, part) => {
    if (part.type !== "literal") {
      acc[part.type] = part.value;
    }
    return acc;
  }, {});
}

/** 按门店时区格式化为 YYYY-MM-DD HH:mm。 */
export function formatStoreDateTime(value: string | null | undefined): string {
  if (!value || Number.isNaN(new Date(value).getTime())) {
    return "—";
  }
  const p = storeParts(value);
  return `${p.year}-${p.month}-${p.day} ${p.hour}:${p.minute}`;
}

/** 按门店时区格式化为 MM-DD HH:mm。 */
export function formatStoreTimeShort(value: string | null | undefined): string {
  if (!value || Number.isNaN(new Date(value).getTime())) {
    return "—";
  }
  const p = storeParts(value);
  return `${p.month}-${p.day} ${p.hour}:${p.minute}`;
}
