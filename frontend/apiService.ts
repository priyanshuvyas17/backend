import { API_BASE_URL, getPreviewUrl, getUploadUrl } from "./ApiConfig";

export type UploadResponse = {
  status: string;
  fileName: string;
  originalName?: string;
  patientName?: string;
  modality?: string;
  path?: string;
  message?: string;
};

/**
 * Upload a file to POST /api/upload.
 * Returns the stored fileName - use this for the preview URL, not the original name.
 */
export async function uploadFile(uri: string, fileName: string, mimeType: string): Promise<UploadResponse> {
  const formData = new FormData();
  formData.append("file", {
    uri,
    name: fileName,
    type: mimeType || "application/octet-stream",
  } as any);

  const url = getUploadUrl();
  const response = await fetch(url, {
    method: "POST",
    body: formData,
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    const text = await response.text();
    let errMsg = `Upload failed: ${response.status}`;
    try {
      const json = JSON.parse(text);
      errMsg = json.message || errMsg;
    } catch (_) {}
    throw new Error(errMsg);
  }

  const data = (await response.json()) as UploadResponse;
  return data;
}

/**
 * Build preview URL for a stored fileName (from upload response).
 * Always use the fileName returned by the server, and encode it for the path.
 */
export function getPreviewUrlForFile(fileName: string): string {
  return getPreviewUrl(fileName);
}
