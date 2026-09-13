import { request } from "./http";
import type {
  CreateSessionPayload,
  PlayerItem,
  RegistrationAction,
  RegistrationItem,
  ScriptItem,
  SessionItem,
  SessionStats,
  SessionStatusFilter,
} from "../types";

// ---------- 场次 ----------

export function fetchSessions(status: SessionStatusFilter = ""): Promise<SessionItem[]> {
  const query = status ? `?status=${status}` : "";
  return request<SessionItem[]>(`/sessions${query}`);
}

export function fetchSession(id: number): Promise<SessionItem> {
  return request<SessionItem>(`/sessions/${id}`);
}

export function fetchSessionStats(): Promise<SessionStats> {
  return request<SessionStats>("/sessions/stats");
}

export function createSession(payload: CreateSessionPayload): Promise<SessionItem> {
  return request<SessionItem>("/sessions", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function startSession(id: number): Promise<SessionItem> {
  return request<SessionItem>(`/sessions/${id}/start`, { method: "POST" });
}

export function cancelSession(id: number): Promise<SessionItem> {
  return request<SessionItem>(`/sessions/${id}/cancel`, { method: "POST" });
}

// ---------- 报名 / 候补 ----------

export function registerForSession(
  sessionId: number,
  playerId: number,
): Promise<RegistrationAction> {
  return request<RegistrationAction>(`/sessions/${sessionId}/register`, {
    method: "POST",
    body: JSON.stringify({ playerId }),
  });
}

export function cancelRegistration(
  sessionId: number,
  playerId: number,
): Promise<RegistrationAction> {
  return request<RegistrationAction>(
    `/sessions/${sessionId}/register?playerId=${playerId}`,
    { method: "DELETE" },
  );
}

export function fetchWaitlist(sessionId: number): Promise<RegistrationItem[]> {
  return request<RegistrationItem[]>(`/sessions/${sessionId}/waitlist`);
}

export function fetchSessionRegistrations(sessionId: number): Promise<RegistrationItem[]> {
  return request<RegistrationItem[]>(`/sessions/${sessionId}/registrations`);
}

// ---------- 剧本 / 玩家 ----------

export function fetchScripts(): Promise<ScriptItem[]> {
  return request<ScriptItem[]>("/scripts");
}

export function fetchPlayers(): Promise<PlayerItem[]> {
  return request<PlayerItem[]>("/players");
}

export function createPlayer(payload: { name: string; phone: string }): Promise<PlayerItem> {
  return request<PlayerItem>("/players", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function fetchPlayerRegistrations(playerId: number): Promise<RegistrationItem[]> {
  return request<RegistrationItem[]>(`/players/${playerId}/registrations`);
}
