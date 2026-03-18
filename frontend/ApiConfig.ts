/**
 * API configuration for React Native app.
 * Set API_BASE_URL to your machine's IP so the device/emulator can reach the backend.
 * Example: http://192.168.1.100:8080 (no trailing slash)
 */
export const API_BASE_URL = "http://10.171.43.31:8080";

export const getPreviewUrl = (fileName: string): string => {
  const encoded = encodeURIComponent(fileName);
  return `${API_BASE_URL}/api/preview/${encoded}`;
};

export const getSystemInfoUrl = (): string => `${API_BASE_URL}/system/info`;

export const getUploadUrl = (): string => `${API_BASE_URL}/api/upload`;
