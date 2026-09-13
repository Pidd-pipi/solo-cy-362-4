// 在两个浏览器时区（东八区 / 零时区）的独立进程中各执行一遍同一套测试，
// 固定同一开始时间在不同时区下的解释，并断言可报名结论只由门店墙钟时间决定。
import { spawnSync } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const vitestBin = resolve(root, "node_modules/vitest/vitest.mjs");

const timezones = ["Asia/Shanghai", "UTC"];
let failed = 0;

for (const tz of timezones) {
  console.log(`\n================= TZ=${tz} =================\n`);
  const result = spawnSync(process.execPath, [vitestBin, "run", "--reporter=verbose"], {
    cwd: root,
    env: { ...process.env, TZ: tz },
    stdio: "inherit",
  });
  if (result.status !== 0) {
    failed += 1;
  }
}

if (failed > 0) {
  console.error(`\n${failed} 个时区进程测试失败\n`);
  process.exit(1);
}
console.log("\n东八区与零时区两个进程全部通过\n");
