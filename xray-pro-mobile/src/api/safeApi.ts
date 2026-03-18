/**
 * Safe API service - never throws, always returns a result.
 * Prevents app crashes from network/PACS/WebSocket errors.
 */

import { api } from '../config/api';

const MAX_RETRIES = 2;
const RETRY_DELAY_MS = 1000;
const REQUEST_TIMEOUT_MS = 15000;

export type ApiResult<T> =
  | { ok: true; data: T }
  | { ok: false; error: string; offline?: boolean };

async function fetchWithTimeout(
  url: string,
  options: RequestInit = {},
  timeout = REQUEST_TIMEOUT_MS
): Promise<Response> {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeout);
  try {
    const res = await fetch(url, {
      ...options,
      signal: controller.signal,
    });
    clearTimeout(id);
    return res;
  } catch (e) {
    clearTimeout(id);
    throw e;
  }
}

async function fetchWithRetry<T>(
  url: string,
  options: RequestInit = {},
  retries = MAX_RETRIES
): Promise<Response> {
  try {
    return await fetchWithTimeout(url, options);
  } catch (e: any) {
    const isRetryable =
      e?.name === 'AbortError' ||
      e?.message === 'Network request failed' ||
      e?.message?.includes('network');
    if (isRetryable && retries > 0) {
      await new Promise((r) => setTimeout(r, RETRY_DELAY_MS));
      return fetchWithRetry(url, options, retries - 1);
    }
    throw e;
  }
}

export async function safeFetch<T>(
  url: string,
  options: RequestInit = {}
): Promise<ApiResult<T>> {
  try {
    const res = await fetchWithRetry(url, options);
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      return {
        ok: false,
        error: (data as any)?.message || `Request failed: ${res.status}`,
      };
    }
    return { ok: true, data: data as T };
  } catch (e: any) {
    const isNetworkError =
      e?.message === 'Network request failed' ||
      e?.message?.includes('network') ||
      e?.name === 'AbortError';
    return {
      ok: false,
      error: isNetworkError ? 'Backend unreachable' : (e?.message || 'Request failed'),
      offline: isNetworkError,
    };
  }
}

export async function checkHealth(): Promise<ApiResult<{ status: string }>> {
  return safeFetch<{ status: string }>(api.health);
}

export async function checkTest(): Promise<ApiResult<Record<string, unknown>>> {
  return safeFetch<Record<string, unknown>>(api.test);
}
