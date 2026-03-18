import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { useWorkflow } from '../context/WorkflowContext';
import { getPacsStatus } from '../api/pacsApi';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'AcquisitionConsole'>;

const EXPOSURE_PLANS = [
  { id: 'skull-pa', name: 'Skull PA', kVp: 70, mAs: 4, dap: 0.4 },
  { id: 'chest-pa', name: 'Chest PA', kVp: 80, mAs: 5, dap: 0.5 },
  { id: 'nasal-bones', name: 'Nasal Bones', kVp: 55, mAs: 3, dap: 0.2 },
  { id: 'skull-lat', name: 'Skull LAT', kVp: 72, mAs: 4, dap: 0.45 },
];

export default function AcquisitionConsoleScreen({ navigation }: Props) {
  const { state } = useWorkflow();
  const [selectedPlan, setSelectedPlan] = useState(EXPOSURE_PLANS[0]);
  const [pacsOnline, setPacsOnline] = useState<boolean | null>(null);
  const [exposing, setExposing] = useState(false);

  React.useEffect(() => {
    getPacsStatus()
      .then((r) => setPacsOnline(r.reachable))
      .catch(() => setPacsOnline(false));
  }, []);

  const handleExpose = async () => {
    if (!state.studyUid || !state.patientUid) {
      Alert.alert('Error', 'No study/patient. Please complete registration and study creation.');
      return;
    }

    setExposing(true);
    try {
      const { status } = await ImagePicker.requestCameraPermissionsAsync();
      if (status !== 'granted') {
        const result = await ImagePicker.launchImageLibraryAsync({
          mediaTypes: ImagePicker.MediaTypeOptions.Images,
          allowsEditing: true,
          quality: 1,
        });
        if (result.canceled) {
          setExposing(false);
          return;
        }
        navigation.navigate('ImagePreview', { imageUri: result.assets[0].uri });
      } else {
        const result = await ImagePicker.launchCameraAsync({
          allowsEditing: true,
          quality: 1,
        });
        if (result.canceled) {
          setExposing(false);
          return;
        }
        navigation.navigate('ImagePreview', { imageUri: result.assets[0].uri });
      }
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Failed to capture');
    } finally {
      setExposing(false);
    }
  };

  const handleFinishStudy = () => {
    if (!state.studyUid) {
      Alert.alert('Error', 'No active study');
      return;
    }
    Alert.alert('Finish Study', 'Mark this study as completed and go to Patient Records?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Finish', onPress: () => navigation.navigate('FinishStudy') },
    ]);
  };

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.header}>X-Ray Pro 5000</Text>
      <Text style={styles.subheader}>Acquisition Console</Text>

      <View style={styles.row}>
        <View style={styles.card}>
          <Text style={styles.cardLabel}>Operator</Text>
          <Text style={styles.cardValue}>{state.operatorName}</Text>
          <Text style={styles.infoText}>Device IP: {state.deviceIp}</Text>
        </View>
        <View style={styles.card}>
          <Text style={styles.cardLabel}>PACS Status</Text>
          <View style={[styles.statusDot, pacsOnline ? styles.online : styles.offline]} />
          <Text style={styles.statusText}>{pacsOnline === null ? 'Checking...' : pacsOnline ? 'Online' : 'Offline'}</Text>
        </View>
      </View>

      <Text style={styles.sectionTitle}>Exposure Plan</Text>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.planScroll}>
        {EXPOSURE_PLANS.map((plan) => (
          <TouchableOpacity
            key={plan.id}
            style={[styles.planButton, selectedPlan.id === plan.id && styles.planButtonActive]}
            onPress={() => setSelectedPlan(plan)}
          >
            <Text style={[styles.planText, selectedPlan.id === plan.id && styles.planTextActive]}>{plan.name}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      <Text style={styles.sectionTitle}>Parameters</Text>
      <View style={styles.paramsRow}>
        <View style={styles.paramBox}>
          <Text style={styles.paramLabel}>kVp</Text>
          <Text style={styles.paramValue}>{selectedPlan.kVp}</Text>
        </View>
        <View style={styles.paramBox}>
          <Text style={styles.paramLabel}>mAs</Text>
          <Text style={styles.paramValue}>{selectedPlan.mAs}</Text>
        </View>
        <View style={styles.paramBox}>
          <Text style={styles.paramLabel}>DAP</Text>
          <Text style={styles.paramValue}>{selectedPlan.dap}</Text>
        </View>
      </View>

      <View style={styles.xrayArea}>
        <Text style={styles.xrayIcon}>☢</Text>
      </View>

      <View style={styles.footer}>
        <TouchableOpacity
          style={[styles.exposeButton, exposing && styles.disabled]}
          onPress={handleExpose}
          disabled={exposing}
        >
          {exposing ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.actionButtonText}>EXPOSE</Text>
          )}
        </TouchableOpacity>
        <TouchableOpacity style={styles.finishButton} onPress={handleFinishStudy}>
          <Text style={styles.actionButtonText}>Finish Study</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a', padding: 16 },
  header: { color: '#fff', fontSize: 20, fontWeight: 'bold', marginBottom: 4 },
  subheader: { color: '#94a3b8', fontSize: 14, marginBottom: 20 },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 16 },
  card: { backgroundColor: '#1e293b', borderRadius: 10, padding: 14, width: '48%' },
  cardLabel: { color: '#94a3b8', fontSize: 12, marginBottom: 4 },
  cardValue: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  infoText: { color: '#64748b', fontSize: 12, marginTop: 6 },
  statusDot: { width: 10, height: 10, borderRadius: 5, marginTop: 8 },
  online: { backgroundColor: '#10b981' },
  offline: { backgroundColor: '#ef4444' },
  statusText: { color: '#fff', fontSize: 12, marginTop: 4 },
  sectionTitle: { color: '#fff', fontSize: 14, fontWeight: 'bold', marginBottom: 10 },
  planScroll: { flexDirection: 'row', marginBottom: 16 },
  planButton: { backgroundColor: '#334155', paddingVertical: 10, paddingHorizontal: 16, borderRadius: 20, marginRight: 10 },
  planButtonActive: { backgroundColor: '#3b82f6' },
  planText: { color: '#94a3b8', fontWeight: '600' },
  planTextActive: { color: '#fff' },
  paramsRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 16 },
  paramBox: { backgroundColor: '#1e293b', borderWidth: 1, borderColor: '#334155', borderRadius: 10, padding: 14, width: '30%', alignItems: 'center' },
  paramLabel: { color: '#64748b', fontSize: 12, marginBottom: 4 },
  paramValue: { color: '#fff', fontSize: 22, fontWeight: 'bold' },
  xrayArea: { height: 180, backgroundColor: '#111', borderRadius: 10, justifyContent: 'center', alignItems: 'center', marginBottom: 20, borderWidth: 1, borderColor: '#334155' },
  xrayIcon: { fontSize: 48, color: '#334155' },
  footer: { flexDirection: 'row', gap: 12 },
  exposeButton: { flex: 1, backgroundColor: '#3b82f6', paddingVertical: 14, borderRadius: 10, alignItems: 'center' },
  finishButton: { flex: 1, backgroundColor: '#0ea5e9', paddingVertical: 14, borderRadius: 10, alignItems: 'center' },
  disabled: { opacity: 0.7 },
  actionButtonText: { color: '#fff', fontWeight: 'bold', fontSize: 14 },
});
