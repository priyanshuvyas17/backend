import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, RefreshControl, Alert } from 'react-native';
import { getPatientRecords } from '../api/pacsApi';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/types';
import type { PatientRecord } from '../api/pacsApi';

type Props = NativeStackScreenProps<RootStackParamList, 'PatientRecords'>;

export default function PatientRecordsScreen({ navigation }: Props) {
  const [records, setRecords] = useState<PatientRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [offline, setOffline] = useState(false);

  const loadRecords = async () => {
    setOffline(false);
    try {
      const data = await getPatientRecords();
      setRecords(data);
    } catch (e: any) {
      const isNetworkError = e?.message === 'Network request failed' || e?.message?.includes('Network');
      setOffline(isNetworkError);
      if (!isNetworkError) {
        Alert.alert('Error', e.message || 'Failed to load records');
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadRecords();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    loadRecords();
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Patient Records</Text>
        <TouchableOpacity onPress={() => navigation.navigate('PatientRegistration')}>
          <Text style={styles.link}>New Patient</Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        style={styles.scroll}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#3b82f6" />}
      >
        {loading ? (
          <Text style={styles.empty}>Loading...</Text>
        ) : offline ? (
          <Text style={styles.offline}>PACS Offline</Text>
        ) : records.length === 0 ? (
          <Text style={styles.empty}>No studies available.</Text>
        ) : (
          records.map((r, i) => (
            <View key={i} style={styles.card}>
              <Text style={styles.cardTitle}>{r.patientName}</Text>
              <Text style={styles.cardSubtitle}>Patient ID: {r.patientId}</Text>
              <Text style={styles.row}>Study Date: {r.studyDate}</Text>
              <Text style={styles.row}>Modality: {r.modality}</Text>
              <Text style={styles.row}>Body Part: {r.bodyPartExamined}</Text>
              <Text style={styles.row}>Image Count: {r.imageCount}</Text>
              {r.images?.map((img, j) => (
                <View key={j} style={styles.imageRow}>
                  <Text style={styles.row}>File: {img.fileName}</Text>
                  <Text style={styles.row}>Upload: {img.uploadDate}</Text>
                  <Text style={styles.row}>Size: {img.fileSize ? `${(img.fileSize / 1024).toFixed(1)} KB` : 'N/A'}</Text>
                </View>
              ))}
              <TouchableOpacity
                style={styles.viewButton}
                onPress={() => navigation.navigate('PacsViewer', {})}
              >
                <Text style={styles.viewButtonText}>View in PACS</Text>
              </TouchableOpacity>
            </View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, borderBottomWidth: 1, borderBottomColor: '#334155' },
  title: { color: '#fff', fontSize: 20, fontWeight: 'bold' },
  link: { color: '#60a5fa', fontSize: 14 },
  scroll: { flex: 1, padding: 16 },
  empty: { color: '#94a3b8', fontSize: 16, textAlign: 'center', marginTop: 48 },
  offline: { color: '#ef4444', fontSize: 16, textAlign: 'center', marginTop: 48, fontWeight: '600' },
  card: { backgroundColor: '#1e293b', borderRadius: 10, padding: 16, marginBottom: 12 },
  cardTitle: { color: '#fff', fontSize: 18, fontWeight: 'bold', marginBottom: 4 },
  cardSubtitle: { color: '#94a3b8', fontSize: 14, marginBottom: 8 },
  row: { color: '#cbd5e1', fontSize: 13, marginBottom: 2 },
  imageRow: { marginTop: 8, paddingTop: 8, borderTopWidth: 1, borderTopColor: '#334155' },
  viewButton: { backgroundColor: '#3b82f6', paddingVertical: 10, borderRadius: 8, alignItems: 'center', marginTop: 12 },
  viewButtonText: { color: '#fff', fontWeight: 'bold' },
});
