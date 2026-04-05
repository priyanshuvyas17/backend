/**
 * Parses API_BASE_URL so screens can show host and port without squeezing a long URL
 * into one line (which causes middle-ellipsis truncation on iOS).
 */
export type ParsedBackendUrl = {
  baseUrl: string;
  scheme: string;
  host: string;
  /** Effective port (443 for https when omitted in URL, etc.) */
  port: number;
  /** Human-readable, e.g. "443 (HTTPS default)" */
  portLabel: string;
};

export function parseBackendUrl(apiBaseUrl: string): ParsedBackendUrl {
  const trimmed = apiBaseUrl.trim().replace(/\/+$/, '');
  let url: URL;
  try {
    url = new URL(trimmed);
  } catch {
    return {
      baseUrl: trimmed,
      scheme: 'https',
      host: trimmed,
      port: 443,
      portLabel: '443',
    };
  }
  const scheme = url.protocol.replace(/:$/, '') || 'https';
  const host = url.hostname;
  let port = url.port ? parseInt(url.port, 10) : NaN;
  if (Number.isNaN(port)) {
    port = scheme === 'https' ? 443 : scheme === 'http' ? 80 : 0;
  }
  const explicitPort = url.port !== '';
  const portLabel = explicitPort
    ? String(port)
    : scheme === 'https'
      ? '443 (HTTPS default)'
      : scheme === 'http'
        ? '80 (HTTP default)'
        : String(port);

  return {
    baseUrl: trimmed,
    scheme,
    host,
    port,
    portLabel,
  };
}
