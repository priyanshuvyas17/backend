import { api } from '../config/ApiConfig';

const MAX_RETRIES = 2;
const RETRY_DELAY_MS = 1000;

async function fetchWithRetry(
  url: string,
  options: RequestInit = {},
  retries = MAX_RETRIES
): Promise<Response> {
  try {
    const res = await fetch(url, options);
    return res;
  } catch (e: any) {
    if (e?.message === 'Network request failed' && retries > 0) {
      await new Promise((r) => setTimeout(r, RETRY_DELAY_MS));
      return fetchWithRetry(url, options, retries - 1);
    }
    throw e;
  }
}

export type PatientRegistration = {
  patientName: string;
  patientId: string;
  age: number;
  gender: string;
  bodyPartExamined: string;
  studyType: string;
};

export type PatientResponse = {
  patientUid: string;
  patientId: number;
  patientName: string;
  patientIdExternal?: string;
  message: string;
};

export type StudyResponse = {
  studyUid: string;
  studyId: number;
  patientUid: string;
  modality: string;
  studyDate: string;
  message: string;
};

export type PatientRecord = {
  patientName: string;
  patientId: string;
  studyDate: string;
  modality: string;
  bodyPartExamined: string;
  imageCount: number;
  images: { fileName: string; uploadDate: string; fileSize: number }[];
};

export async function registerPatient(data: PatientRegistration): Promise<PatientResponse> {
  const res = await fetchWithRetry(api.patients, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Registration failed: ${res.status}`);
  }
  return res.json();
}

export async function createStudy(patientUid: string, studyDate: string): Promise<StudyResponse> {
  const res = await fetchWithRetry(api.studies, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ patientUid, studyDate }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Study creation failed: ${res.status}`);
  }
  return res.json();
}

export async function finishStudy(studyUid: string): Promise<{ message: string }> {
  const res = await fetchWithRetry(`${api.studies}/${studyUid}/finish`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Finish study failed: ${res.status}`);
  }
  return res.json();
}

export async function convertAndStoreDicom(
  uri: string,
  metadata: {
    patientName: string;
    patientId: string;
    studyUid: string;
    modality: string;
    bodyPartExamined: string;
    studyDate: string;
  }
): Promise<{ instanceId: string }> {
  const formData = new FormData();
  formData.append('file', {
    uri,
    name: 'capture.png',
    type: 'image/png',
  } as any);
  formData.append('patientName', metadata.patientName);
  formData.append('patientId', metadata.patientId);
  formData.append('studyUid', metadata.studyUid);
  formData.append('modality', metadata.modality);
  formData.append('bodyPartExamined', metadata.bodyPartExamined);
  formData.append('studyDate', metadata.studyDate);

  const res = await fetchWithRetry(`${api.dicom}/convert-and-store`, {
    method: 'POST',
    body: formData,
    headers: { Accept: 'application/json' },
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `DICOM conversion failed: ${res.status}`);
  }
  const data = await res.json();
  return { instanceId: data.instanceId };
}

export async function getPatientRecords(patientUid?: string): Promise<PatientRecord[]> {
  const url = patientUid ? `${api.patients}/${patientUid}/records` : `${api.patients}/records`;
  const res = await fetchWithRetry(url);
  if (!res.ok) throw new Error(`Failed to fetch records: ${res.status}`);
  const data = await res.json();
  return data.records || [];
}

export async function getPacsStatus(): Promise<{ reachable: boolean }> {
  const res = await fetchWithRetry(`${api.pacs}/status`);
  if (!res.ok) throw new Error('PACS Offline');
  const data = await res.json();
  return { reachable: data.reachable };
}

export async function getPacsPatients(): Promise<string[]> {
  const res = await fetchWithRetry(`${api.pacs}/patients`);
  const data = await res.json();
  if ((data as any)?.error) throw new Error((data as any).message || 'PACS Offline');
  return Array.isArray(data) ? data : [];
}

export async function getPacsPatientStudies(patientId: string): Promise<unknown> {
  const res = await fetchWithRetry(`${api.pacs}/patients/${patientId}/studies`);
  const data = await res.json();
  if ((data as any)?.error) throw new Error((data as any).message || 'PACS Offline');
  return data;
}

export async function getPacsStudy(studyId: string): Promise<Record<string, unknown>> {
  const res = await fetchWithRetry(`${api.pacs}/studies/${studyId}`);
  const data = await res.json();
  if ((data as any)?.error) throw new Error((data as any).message || 'PACS Offline');
  return (data as Record<string, unknown>) || {};
}

export async function getPacsSeries(seriesId: string): Promise<Record<string, unknown>> {
  const res = await fetchWithRetry(`${api.pacs}/series/${seriesId}`);
  const data = await res.json();
  if ((data as any)?.error) throw new Error((data as any).message || 'PACS Offline');
  return (data as Record<string, unknown>) || {};
}
