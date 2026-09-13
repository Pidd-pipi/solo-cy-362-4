import { API_BASE_URL } from "../constants/app";

/**
 * 统一请求封装：后端业务错误返回 4xx + { message }，
 * 这里抛出带中文提示的 Error，供页面用 ElMessage 明确展示。
 */
export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { Accept: "application/json", ...(options.body ? { "Content-Type": "application/json" } : {}) },
    ...options,
  });

  if (!response.ok) {
    let message = `请求失败（${response.status}）`;
    try {
      const data = (await response.json()) as { message?: string };
      if (data.message) {
        message = data.message;
      }
    } catch {
      // 非 JSON 错误体时保留默认提示
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
