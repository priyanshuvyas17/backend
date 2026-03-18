import React from "react";
import { View, Image, Text, StyleSheet, ActivityIndicator } from "react-native";
import { getPreviewUrl } from "./ApiConfig";

type ScanPreviewCardProps = {
  fileName: string | null;
  originalName?: string | null;
  onPressView?: () => void;
};

export function ScanPreviewCard({ fileName, originalName, onPressView }: ScanPreviewCardProps) {
  const [loading, setLoading] = React.useState(!!fileName);
  const [error, setError] = React.useState<string | null>(null);

  const previewUrl = fileName ? getPreviewUrl(fileName) : null;

  React.useEffect(() => {
    if (!fileName) {
      setLoading(false);
      setError(null);
      return;
    }
    setLoading(true);
    setError(null);
    console.log("Preview URL:", previewUrl);
  }, [fileName, previewUrl]);

  if (!fileName) {
    return (
      <View style={styles.placeholder}>
        <Text style={styles.placeholderText}>No file selected</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.previewBox}>
        {loading && (
          <View style={styles.loadingOverlay}>
            <ActivityIndicator size="large" color="#007AFF" />
            <Text style={styles.loadingText}>Loading preview...</Text>
          </View>
        )}
        {previewUrl && (
          <Image
            source={{ uri: previewUrl }}
            style={styles.previewImage}
            resizeMode="contain"
            onLoadStart={() => setLoading(true)}
            onLoadEnd={() => setLoading(false)}
            onError={(e) => {
              setLoading(false);
              setError(e.nativeEvent.error || "Failed to load preview");
              console.warn("Preview load error:", e.nativeEvent.error);
            }}
          />
        )}
        {error && (
          <View style={styles.errorOverlay}>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}
      </View>
      <Text style={styles.fileName} numberOfLines={1}>
        {originalName || fileName}
      </Text>
      {onPressView && (
        <Text style={styles.tapToView} onPress={onPressView}>
          Tap to view
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { marginVertical: 8 },
  placeholder: {
    height: 200,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#f0f0f0",
    borderRadius: 8,
  },
  placeholderText: { color: "#666", fontSize: 14 },
  previewBox: {
    height: 240,
    backgroundColor: "#e8e8e8",
    borderRadius: 8,
    overflow: "hidden",
    position: "relative",
  },
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#e8e8e8",
  },
  loadingText: { marginTop: 8, color: "#666" },
  previewImage: {
    width: "100%",
    height: "100%",
  },
  errorOverlay: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: "center",
    alignItems: "center",
    padding: 16,
  },
  errorText: { color: "#c00", textAlign: "center" },
  fileName: { marginTop: 6, fontSize: 12, color: "#333" },
  tapToView: { marginTop: 4, fontSize: 12, color: "#007AFF" },
});
