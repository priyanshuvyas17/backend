import React, { useMemo, useState } from "react";
import { View, StyleSheet, ActivityIndicator, Text } from "react-native";
import { WebView } from "react-native-webview";
import { getPreviewUrl } from "./ApiConfig";

type DicomViewerProps = {
  fileName: string | null;
  onError?: (message: string) => void;
};

/**
 * DICOM viewer using WebView with a minimal HTML viewer that displays
 * the preview image (backend serves PNG for DICOM). For full DICOM tools
 * you would load cornerstone/cornerstoneWADOImageLoader in the WebView.
 */
const DicomViewer: React.FC<DicomViewerProps> = ({ fileName, onError }) => {
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadMsg, setLoadMsg] = useState<string | null>(null);

  const previewUrl = fileName ? getPreviewUrl(fileName) : null;

  const viewerHtml = useMemo(() => {
    if (!previewUrl) return "";
    console.log("DicomViewer preview URL:", previewUrl);
    return `<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=3, user-scalable=yes"/>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
    .container { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
    img { max-width: 100%; max-height: 100%; object-fit: contain; }
    .error { color: #f44; padding: 16px; text-align: center; }
  </style>
</head>
<body>
  <div class="container">
    <img src="${previewUrl}" alt="DICOM preview" onload="window.onPreviewLoaded && window.onPreviewLoaded();" onerror="window.onPreviewError && window.onPreviewError();"/>
  </div>
  <script>
    window.onPreviewLoaded = function() { window.ReactNativeWebView && window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'loaded' })); };
    window.onPreviewError = function() { window.ReactNativeWebView && window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'error', message: 'Failed to load image' })); };
  </script>
</body>
</html>`;
  }, [previewUrl]);

  const handleMessage = (event: { nativeEvent: { data: string } }) => {
    try {
      const data = JSON.parse(event.nativeEvent.data);
      if (data.type === "loaded") {
        setLoadFailed(false);
        setLoadMsg(null);
      }
      if (data.type === "error") {
        setLoadFailed(true);
        setLoadMsg(data.message || "Viewer libraries did not load correctly.");
        onError?.(data.message || "Viewer libraries did not load correctly.");
      }
    } catch (_) {}
  };

  if (!fileName) {
    return (
      <View style={styles.placeholder}>
        <Text style={styles.placeholderText}>Select a DICOM file to view</Text>
      </View>
    );
  }

  if (!viewerHtml) {
    return (
      <View style={styles.placeholder}>
        <Text style={styles.errorText}>No preview URL</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <WebView
        originWhitelist={["*"]}
        javaScriptEnabled
        domStorageEnabled
        source={{ html: viewerHtml }}
        style={styles.webview}
        onMessage={handleMessage}
        onHttpError={(e) => {
          setLoadFailed(true);
          const msg = `HTTP ${e.nativeEvent.status}: ${e.nativeEvent.description || "Request failed"}`;
          setLoadMsg(msg);
          onError?.(msg);
        }}
        renderLoading={() => (
          <View style={styles.loadingOverlay}>
            <ActivityIndicator size="large" color="#fff" />
            <Text style={styles.loadingText}>Loading preview...</Text>
          </View>
        )}
        startInLoadingState
      />
      {loadFailed && loadMsg && (
        <View style={styles.errorOverlay}>
          <Text style={styles.errorText}>{loadMsg}</Text>
          <Text style={styles.errorSubtext}>
            The file may be compressed, corrupted, or in an unsupported format.
          </Text>
        </View>
      )}
    </View>
  );
};

export default DicomViewer;

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#000" },
  webview: { flex: 1, backgroundColor: "#000" },
  placeholder: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#000",
  },
  placeholderText: { color: "#888", fontSize: 14 },
  loadingOverlay: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#000",
  },
  loadingText: { color: "#fff", marginTop: 8 },
  errorOverlay: {
    position: "absolute",
    bottom: 0,
    left: 0,
    right: 0,
    padding: 16,
    backgroundColor: "rgba(0,0,0,0.8)",
  },
  errorText: { color: "#f44", fontSize: 14 },
  errorSubtext: { color: "#888", fontSize: 12, marginTop: 4 },
});
