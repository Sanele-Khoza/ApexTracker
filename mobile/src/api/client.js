import AsyncStorage from '@react-native-async-storage/async-storage';
import Constants from 'expo-constants';

function resolveApiUrl() {
  try {
    if (typeof window !== 'undefined' && window.location && window.location.hostname) {
      return `http://${window.location.hostname}:8080/api`;
    }
    const hostUri =
      (Constants.expoConfig && Constants.expoConfig.hostUri) ||
      (Constants.expoGoConfig && Constants.expoGoConfig.debuggerHost);
    if (hostUri) {
      const host = hostUri.split(':')[0];
      if (host) return `http://${host}:8080/api`;
    }
  } catch (e) {
    // ignore
  }
  return 'http://localhost:8080/api';
}

export const API_URL = resolveApiUrl();

export class ApiError extends Error {
  constructor(message, status, fields) {
    super(message);
    this.status = status;
    this.fields = fields || null;
  }
}

export async function api(path, { method = 'GET', body, params } = {}) {
  let url = `${API_URL}${path}`;
  if (params) {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.append(k, v);
    });
    const q = qs.toString();
    if (q) url += `?${q}`;
  }
  const token = await AsyncStorage.getItem('apex_token');
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    throw new ApiError('Cannot reach the server. Make sure the backend is running.', 0);
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const msg = data && data.message ? data.message : `Request failed (${res.status})`;
    throw new ApiError(msg, res.status, data && data.fields);
  }
  return data;
}

export const Api = {
  login: (email, password) => api('/auth/login', { method: 'POST', body: { email, password } }),
  register: (name, email, password) =>
    api('/auth/register', { method: 'POST', body: { name, email, password } }),
  dashboard: () => api('/dashboard'),
  planVsActual: (date) => api('/dashboard/plan-vs-actual', { params: { date } }),

  tasks: (params) => api('/tasks', { params }),
  createTask: (body) => api('/tasks', { method: 'POST', body }),
  updateTask: (id, body) => api(`/tasks/${id}`, { method: 'PUT', body }),
  deleteTask: (id) => api(`/tasks/${id}`, { method: 'DELETE' }),
  completeTask: (id) => api(`/tasks/${id}/complete`, { method: 'POST' }),
  startTask: (id) => api(`/tasks/${id}/start`, { method: 'POST' }),
  skipTask: (id) => api(`/tasks/${id}/skip`, { method: 'POST' }),
  rescheduleTask: (id, newDate, newTime) =>
    api(`/tasks/${id}/reschedule`, { method: 'POST', body: { newDate, newTime } }),

  activities: () => api('/activities'),
  startActivity: (body) => api('/activities', { method: 'POST', body }),
  stopActivity: (id) => api(`/activities/${id}/stop`, { method: 'POST' }),
  stopCurrentActivity: () => api('/activities/stop', { method: 'POST' }),
  deleteActivity: (id) => api(`/activities/${id}`, { method: 'DELETE' }),

  studySessions: () => api('/study'),
  startStudy: (body) => api('/study', { method: 'POST', body }),
  pauseStudy: (id) => api(`/study/${id}/pause`, { method: 'POST' }),
  resumeStudy: (id) => api(`/study/${id}/resume`, { method: 'POST' }),
  finishStudy: (id) => api(`/study/${id}/finish`, { method: 'POST' }),
  deleteStudy: (id) => api(`/study/${id}`, { method: 'DELETE' }),

  sleepRecords: () => api('/sleep'),
  createSleep: (body) => api('/sleep', { method: 'POST', body }),
  updateSleep: (id, body) => api(`/sleep/${id}`, { method: 'PUT', body }),
  deleteSleep: (id) => api(`/sleep/${id}`, { method: 'DELETE' }),

  habits: () => api('/habits'),
  createHabit: (body) => api('/habits', { method: 'POST', body }),
  toggleHabit: (id, date) => api(`/habits/${id}/toggle`, { params: { date } }),
  deleteHabit: (id) => api(`/habits/${id}`, { method: 'DELETE' }),

  goals: () => api('/goals'),
  createGoal: (body) => api('/goals', { method: 'POST', body }),
  deleteGoal: (id) => api(`/goals/${id}`, { method: 'DELETE' }),

  reminders: () => api('/reminders'),
  dismissReminder: (id) => api(`/reminders/${id}/dismiss`, { method: 'POST' }),
  markReminderRead: (id) => api(`/reminders/${id}/read`, { method: 'POST' }),

  analytics: (from, to) => api('/analytics', { params: { from, to } }),
  weekly: () => api('/analytics/weekly'),
  insights: () => api('/insights'),
  review: (date) => api(`/reviews/${date}`),
  updateReviewReflection: (date, reflection) =>
    api(`/reviews/${date}/reflection`, { method: 'POST', body: { reflection } }),

  settings: () => api('/settings'),
  updateSettings: (body) => api('/settings', { method: 'PUT', body }),
  exportData: () => api('/settings/export'),
  deleteAccount: () => api('/settings/account', { method: 'DELETE' }),
};

export function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

export function minutesToLabel(min) {
  if (min === null || min === undefined) return '—';
  const h = Math.floor(min / 60);
  const m = min % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

export function secondsToLabel(sec) {
  return minutesToLabel(Math.round((sec || 0) / 60));
}