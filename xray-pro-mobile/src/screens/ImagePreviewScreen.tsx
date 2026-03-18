import React, { useState } from 'react';
import { View, Text, Image, StyleSheet, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { useWorkflow } from '../context/WorkflowContext';
import { convertAndStoreDicom } from '../api/pacsApi';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'ImagePreview'>;

export default function ImagePreviewScreen({ route, navigation }: Props) {
  const { imageUri } = route.params;
  const { state } = useWorkflow();
  const [loading, setLoading] = useState(false);

  const handleRetake = () => {
    navigation.goBack();
  };

  const handleAccept = async () => {
    if (!state.patientUid || !state.patientName || !state.patientId || !state.studyUid || !state.studyDate || !state.bodyPartExamined) {
      Alert.alert('Error', 'Missing patient/study data. Please complete registration.');
      return;
    }

    setLoading(true);
    try {
      await convertAndStoreDicom(imageUri, {
        patientName: state.patientName,
        patientId: state.patientId,
        studyUid: state.studyUid,
        modality: state.modality,
        bodyPartExamined: state.bodyPartExamined,
        studyDate: state.studyDate,
      });
      Alert.alert('Success', 'Image converted to DICOM and stored in PACS.', [
        { text: 'OK', onPress: () => navigation.navigate('AcquisitionConsole') },
      ]);
    } catch (e: any) {
      Alert.alert('Error', e.message || 'DICOM conversion or PACS storage failed. Check that Orthanc is running.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Image Preview</Text>
      <View style={styles.imageContainer}>
        <Image source={{ uri: imageUri }} style={styles.image} resizeMode="contain" />
      </View>
      <View style={styles.buttons}>
        <TouchableOpacity style={styles.retakeButton} onPress={handleRetake} disabled={loading}>
          <Text style={styles.buttonText}>Retake</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.acceptButton} onPress={handleAccept} disabled={loading}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Accept</Text>}
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a', padding: 16 },
  title: { color: '#fff', fontSize: 18, fontWeight: 'bold', marginBottom: 16 },
  imageContainer: { flex: 1, backgroundColor: '#111', borderRadius: 10, overflow: 'hidden', marginBottom: 16 },
  image: { width: '100%', height: '100%' },
  buttons: { flexDirection: 'row', gap: 12 },
  retakeButton: { flex: 1, backgroundColor: '#64748b', paddingVertical: 14, borderRadius: 10, alignItems: 'center' },
  acceptButton: { flex: 1, backgroundColor: '#10b981', paddingVertical: 14, borderRadius: 10, alignItems: 'center' },
  buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 16 },
});
