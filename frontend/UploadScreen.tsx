import React, { useState } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ActivityIndicator,
} from "react-native";
import * as DocumentPicker from "expo-document-picker";
import { ScanPreviewCard } from "./ScanPreviewCard";
import DicomViewer from "./DicomViewer";
import { uploadFile, getPreviewUrlForFile } from "./apiService";

export default function UploadScreen() {
  const [selectedFile, setSelectedFile] = useState<{
    uri: string;
    name: string;
    mimeType: string;
  } | null>(null);
  const [storedFileName, setStoredFileName] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const [viewerVisible, setViewerVisible] = useState(false);

  const previewFileName = storedFileName || null;
  const previewUrl = previewFileName ? getPreviewUrlForFile(previewFileName) : null;

  console.log("Preview URL:", previewUrl);

  const pickDicom = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ["image/dicom", "application/dicom", "*/*"],
        copyToCacheDirectory: true,
      });
      if (result.canceled) return;
      const file = result.assets[0];
      setSelectedFile({
        uri: file.uri,
        name: file.name,
        mimeType: file.mimeType || "application/octet-stream",
      });
      setStoredFileName(null);
      setUploadSuccess(false);
    } catch (e) {
      console.warn("Pick DICOM failed", e);
      Alert.alert("Error", "Failed to select file");
    }
  };

  const pickImage = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ["image/*"],
        copyToCacheDirectory: true,
      });
      if (result.canceled) return;
      const file = result.assets[0];
      setSelectedFile({
        uri: file.uri,
        name: file.name,
        mimeType: file.mimeType || "image/jpeg",
      });
      setStoredFileName(null);
      setUploadSuccess(false);
    } catch (e) {
      console.warn("Pick image failed", e);
      Alert.alert("Error", "Failed to select image");
    }
  };

  const upload = async () => {
    if (!selectedFile) {
      Alert.alert("No file", "Select a file first");
      return;
    }
    setUploading(true);
    setUploadSuccess(false);
    try {
      const response = await uploadFile(
        selectedFile.uri,
        selectedFile.name,
        selectedFile.mimeType
      );
      if (response.status === "success" && response.fileName) {
        setStoredFileName(response.fileName);
        setUploadSuccess(true);
      } else {
        Alert.alert("Upload failed", response.message || "Unknown error");
      }
    } catch (e: any) {
      console.warn("Upload error", e);
      Alert.alert("Upload failed", e?.message || "Network or server error");
    } finally {
      setUploading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Upload Scan</Text>

      <View style={styles.buttonRow}>
        <TouchableOpacity style={styles.button} onPress={pickImage}>
          <Text style={styles.buttonText}>Select Image</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.button} onPress={pickDicom}>
          <Text style={styles.buttonText}>Select DICOM</Text>
        </TouchableOpacity>
      </View>

      <ScanPreviewCard
        fileName={previewFileName}
        originalName={selectedFile?.name}
        onPressView={() => setViewerVisible(true)}
      />

      <TouchableOpacity
        style={[styles.uploadButton, uploading && styles.uploadButtonDisabled]}
        onPress={upload}
        disabled={uploading || !selectedFile}
      >
        {uploading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.uploadButtonText}>Upload</Text>
        )}
      </TouchableOpacity>

      {uploadSuccess && (
        <Text style={styles.successText}>Upload Successful</Text>
      )}

      {viewerVisible && (
        <View style={StyleSheet.absoluteFill}>
          <View style={styles.viewerHeader}>
            <TouchableOpacity onPress={() => setViewerVisible(false)}>
              <Text style={styles.viewerClose}>← Back</Text>
            </TouchableOpacity>
          </View>
          <DicomViewer fileName={previewFileName} />
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, paddingTop: 48 },
  title: { fontSize: 22, fontWeight: "600", marginBottom: 16 },
  buttonRow: { flexDirection: "row", gap: 12, marginBottom: 16 },
  button: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    backgroundColor: "#e0e0e0",
    borderRadius: 8,
  },
  buttonText: { fontSize: 14 },
  uploadButton: {
    marginTop: 16,
    paddingVertical: 14,
    backgroundColor: "#007AFF",
    borderRadius: 8,
    alignItems: "center",
  },
  uploadButtonDisabled: { opacity: 0.6 },
  uploadButtonText: { color: "#fff", fontSize: 16, fontWeight: "600" },
  successText: { marginTop: 8, color: "#0a0", fontSize: 14 },
  viewerHeader: {
    flexDirection: "row",
    padding: 12,
    backgroundColor: "#333",
    alignItems: "center",
  },
  viewerClose: { color: "#fff", fontSize: 16 },
});
