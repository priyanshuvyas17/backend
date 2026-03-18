import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { useWorkflow } from '../context/WorkflowContext';
import { finishStudy } from '../api/pacsApi';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'FinishStudy'>;

export default function FinishStudyScreen({ navigation }: Props) {
  const { state, reset } = useWorkflow();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!state.studyUid) {
      setError('No study to finish');
      setLoading(false);
      return;
    }
    finishStudy(state.studyUid)
      .then(() => {
        reset();
        navigation.reset({ index: 0, routes: [{ name: 'PatientRecords' }] });
      })
      .catch((e) => {
        setError(e.message || 'Failed to finish study');
        Alert.alert('Error', e.message || 'Failed to finish study', [
          { text: 'OK', onPress: () => navigation.navigate('PatientRecords') },
        ]);
      })
      .finally(() => setLoading(false));
  }, [state.studyUid]);

  if (loading) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" color="#3b82f6" />
        <Text style={styles.text}>Finishing study...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.container}>
        <Text style={styles.error}>{error}</Text>
      </View>
    );
  }

  return null;
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a', justifyContent: 'center', alignItems: 'center', padding: 24 },
  text: { color: '#94a3b8', marginTop: 12 },
  error: { color: '#ef4444', fontSize: 16 },
});
