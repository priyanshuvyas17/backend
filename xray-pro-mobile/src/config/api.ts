/**
 * API configuration for X-Ray Pro 5000.
 * Use your laptop's local IP (e.g. 192.168.1.100) so the phone can reach the backend.
 * NEVER use localhost - on mobile it refers to the device itself.
 *
 * Find your IP:
 * - Mac: System Settings → Network → Wi-Fi → Details
 * - Windows: ipconfig
 * - Linux: ip addr
 */
export const API_BASE_URL =
  (typeof global !== 'undefined' && (global as any).__API_BASE_URL__) ||
  (typeof process !== 'undefined' && process.env?.EXPO_PUBLIC_API_URL) ||
  'http://192.168.1.100:8080';

export const api = {
  base: API_BASE_URL,
  health: `${API_BASE_URL}/health`,
  test: `${API_BASE_URL}/test`,
  /** Canonical host/port from server (matches app.base-url on the backend). */
  backendConnection: `${API_BASE_URL}/api/public/backend-connection`,
  patients: `${API_BASE_URL}/api/patients`,
  studies: `${API_BASE_URL}/api/studies`,
  dicom: `${API_BASE_URL}/api/dicom`,
  pacs: `${API_BASE_URL}/pacs`,
  preview: (fileName: string) => `${API_BASE_URL}/api/preview/${encodeURIComponent(fileName)}`,
};
