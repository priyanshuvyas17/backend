import React, { useMemo, useState } from 'react';
import { View, Text, StyleSheet, ActivityIndicator } from 'react-native';
import { WebView } from 'react-native-webview';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/types';
import { API_BASE_URL } from '../config/ApiConfig';

type Props = NativeStackScreenProps<RootStackParamList, 'PacsViewer'>;

/** DICOM viewer - uses same host as backend for DICOMweb (no localhost). */
const DICOMWEB_HOST = API_BASE_URL.replace(/:\d+$/, ':8042');
const DICOMWEB_URL = `${DICOMWEB_HOST}/dicom-web`;
const OHIF_VIEWER_URL = `https://viewer.ohif.org/viewer?url=${encodeURIComponent(DICOMWEB_URL)}`;

export default function PacsViewerScreen({ route }: Props) {
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadMsg, setLoadMsg] = useState<string | null>(null);

  const viewerHtml = useMemo(() => {
    return `<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=3, user-scalable=yes"/>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body { width: 100%; height: 100%; background: #0f172a; color: #94a3b8; font-family: system-ui; }
    .header { padding: 20px; text-align: center; }
    .title { font-size: 18px; color: #fff; margin-bottom: 8px; }
    .info { font-size: 13px; margin-bottom: 4px; }
    .link { display: inline-block; margin-top: 16px; padding: 12px 24px; background: #3b82f6; color: #fff; text-decoration: none; border-radius: 8px; font-weight: bold; }
    iframe { width: 100%; height: calc(100% - 180px); border: none; }
  </style>
</head>
<body>
  <div class="header">
    <div class="title">X-Ray Pro 5000 - DICOM Viewer</div>
    <div class="info">Features: Zoom, Pan, Window Level, Measurements</div>
    <div class="info">DICOMweb: ${DICOMWEB_URL}</div>
    <a class="link" href="${OHIF_VIEWER_URL}">Open OHIF Viewer</a>
  </div>
  <iframe src="${OHIF_VIEWER_URL}" title="OHIF DICOM Viewer"></iframe>
</body>
</html>`;
  }, []);

  return (
    <View style={styles.container}>
      <WebView
        originWhitelist={['*']}
        javaScriptEnabled
        domStorageEnabled
        source={{ html: viewerHtml }}
        style={styles.webview}
        onHttpError={(e) => {
          setLoadFailed(true);
          setLoadMsg(`HTTP ${e.nativeEvent.statusCode}: ${e.nativeEvent.description || 'Request failed'}`);
        }}
        renderLoading={() => (
          <View style={styles.loading}>
            <ActivityIndicator size="large" color="#3b82f6" />
            <Text style={styles.loadingText}>Loading DICOM viewer...</Text>
          </View>
        )}
        startInLoadingState
      />
      {loadFailed && loadMsg && (
        <View style={styles.error}>
          <Text style={styles.errorText}>{loadMsg}</Text>
          <Text style={styles.errorSubtext}>Ensure Orthanc is running and DICOMweb plugin is enabled.</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a' },
  webview: { flex: 1 },
  loading: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, justifyContent: 'center', alignItems: 'center', backgroundColor: '#0f172a' },
  loadingText: { color: '#94a3b8', marginTop: 12 },
  error: { position: 'absolute', bottom: 0, left: 0, right: 0, padding: 16, backgroundColor: 'rgba(239,68,68,0.2)' },
  errorText: { color: '#ef4444', fontSize: 14 },
  errorSubtext: { color: '#94a3b8', fontSize: 12, marginTop: 4 },
});
