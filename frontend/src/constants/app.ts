export const APP_NAME = "剧本杀门店运营管理系统";
export const APP_CODE = "ldmurdergame";
export const API_BASE_URL = "/api";
export const FRONTEND_PORT = 28502;
export const BACKEND_PORT = 29502;

export const APP_THEME = {
  paper: "#f4f7fb",
  ink: "#19212e",
  accent: "#3268b8",
  warm: "#cf5c36",
  surface: "#dfe8f4",
};

/**
 * 门店统一业务时区：后端按此时区把场次开始时间下发为带偏移的绝对时刻。
 * 页面所有开场时间均按此时区展示，与浏览器所在时区解耦。
 */
export const STORE_TIME_ZONE = "Asia/Shanghai";
