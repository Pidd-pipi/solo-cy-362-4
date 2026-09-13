export interface FeatureItem {
  id: number;
  title: string;
  description: string;
  status: string;
  metric: string;
}

export interface KpiItem {
  label: string;
  value: string;
  globalTrend?: string;
  trend?: string;
  tone: string;
}

export interface OperationRecord {
  key: string;
  name: string;
  owner: string;
  status: string;
  metric: string;
  priority: string;
}

export interface OverviewResponse {
  appName: string;
  appCode: string;
  description: string;
  features: FeatureItem[];
  kpis: KpiItem[];
  records: OperationRecord[];
}

// ---------- 场次报名与候补模块 ----------

export type SessionStatusFilter = "" | "SCHEDULED" | "STARTED" | "CANCELLED";

export interface ScriptItem {
  id: number;
  name: string;
  genre: string;
  difficulty: string;
  durationMinutes: number;
  minPlayers: number;
  maxPlayers: number;
  dmRequired: boolean;
  description: string | null;
}

export interface PlayerItem {
  id: number;
  name: string;
  phone: string;
  memberLevel: string;
}

export interface SessionItem {
  id: number;
  scriptName: string;
  genre: string;
  dmName: string | null;
  startTime: string;
  capacity: number;
  status: "SCHEDULED" | "STARTED" | "CANCELLED";
  registeredCount: number;
  waitlistCount: number;
  remainingSlots: number;
  full: boolean;
  /** 可报名：状态为报名中且未到开始时间；时间到达后即使未手动开场也为 false */
  registerable: boolean;
}

export interface RegistrationItem {
  id: number;
  sessionId: number;
  scriptName: string;
  sessionStartTime: string;
  capacity: number;
  sessionStatus: string;
  playerId: number;
  playerName: string;
  playerPhone: string;
  memberLevel: string;
  status: "REGISTERED" | "WAITLISTED" | "CANCELLED";
  waitlistPosition: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface RegistrationAction {
  registrationId: number;
  sessionId: number;
  playerId: number;
  playerName: string;
  result: "REGISTERED" | "WAITLISTED" | "CANCELLED"
  status: string;
  waitlistPosition: number | null;
  registeredCount: number;
  waitlistCount: number;
  remainingSlots: number;
  promotedPlayerId: number | null;
  promotedPlayerName: string | null;
  message: string;
}

export interface SessionStats {
  totalSessions: number;
  scheduledSessions: number;
  startedSessions: number;
  cancelledSessions: number;
  totalRegistered: number;
  totalWaitlisted: number;
  fullSessions: number;
  availableSessions: number;
  averageFillRate: number;
}

export interface CreateSessionPayload {
  scriptId: number;
  dmName: string | null;
  startTime: string;
  capacity: number;
  operator: string;
}
