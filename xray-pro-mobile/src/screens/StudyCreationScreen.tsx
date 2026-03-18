import React, { useState } from 'react';
import { View, Text, TextInput, StyleSheet, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { useWorkflow } from '../context/WorkflowContext';
import { createStudy } from '../api/pacsApi';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'StudyCreation'>;

export default function StudyCreationScreen({ navigation }: Props) {
  const { state, setStudy } = useWorkflow();
  const [studyDate, setStudyDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [loading, setLoading] = useState(false);

  const handleCreate = async () => {
    if (!state.patientUid) {
      Alert.alert('Error', 'No patient selected. Please register a patient first.');
      navigation.goBack();
      return;
    }

    setLoading(true);
    try {
      const res = await createStudy(state.patientUid, studyDate);
      setStudy({ studyUid: res.studyUid, studyDate });
      Alert.alert('Success', 'Study created. Proceed to Acquisition Console.', [
        { text: 'OK', onPress: () => navigation.navigate('AcquisitionConsole') },
      ]);
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Study creation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Create Study</Text>
      <Text style={styles.subtitle}>Patient: {state.patientName || 'N/A'}</Text>
      <Text style={styles.subtitle}>Patient UID: {state.patientUid || 'N/A'}</Text>

      <Text style={styles.label}>Study Date</Text>
      <TextInput
        style={styles.input}
        value={studyDate}
        onChangeText={setStudyDate}
        placeholder="YYYY-MM-DD"
      />

      <TouchableOpacity style={styles.button} onPress={handleCreate} disabled={loading}>
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Create Study</Text>}
      </TouchableOpacity>

      <TouchableOpacity style={styles.back} onPress={() => navigation.goBack()}>
        <Text style={styles.backText}>Back</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a', padding: 24 },
  title: { color: '#fff', fontSize: 22, fontWeight: 'bold', marginBottom: 8 },
  subtitle: { color: '#94a3b8', fontSize: 14, marginBottom: 4 },
  label: { color: '#cbd5e1', fontSize: 14, marginTop: 20, marginBottom: 6 },
  input: {
    backgroundColor: '#1e293b',
    borderRadius: 10,
    padding: 14,
    color: '#fff',
    fontSize: 16,
  },
  button: {
    backgroundColor: '#3b82f6',
    borderRadius: 10,
    padding: 16,
    alignItems: 'center',
    marginTop: 24,
  },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  back: { marginTop: 16, alignItems: 'center' },
  backText: { color: '#60a5fa', fontSize: 14 },
});
