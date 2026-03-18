import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useWorkflow } from '../context/WorkflowContext';
import { registerPatient } from '../api/pacsApi';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'PatientRegistration'>;

export default function PatientRegistrationScreen({ navigation }: Props) {
  const { setPatient } = useWorkflow();
  const [patientName, setPatientName] = useState('');
  const [patientId, setPatientId] = useState('');
  const [age, setAge] = useState('');
  const [gender, setGender] = useState('');
  const [bodyPartExamined, setBodyPartExamined] = useState('');
  const [studyType, setStudyType] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!patientName.trim() || !patientId.trim() || !age.trim() || !gender.trim() || !bodyPartExamined.trim() || !studyType.trim()) {
      Alert.alert('Error', 'Please fill all fields');
      return;
    }
    const ageNum = parseInt(age, 10);
    if (isNaN(ageNum) || ageNum < 0 || ageNum > 150) {
      Alert.alert('Error', 'Please enter a valid age');
      return;
    }

    setLoading(true);
    try {
      const res = await registerPatient({
        patientName: patientName.trim(),
        patientId: patientId.trim(),
        age: ageNum,
        gender: gender.trim(),
        bodyPartExamined: bodyPartExamined.trim(),
        studyType: studyType.trim(),
      });
      setPatient({
        patientUid: res.patientUid,
        patientName: res.patientName,
        patientId: res.patientIdExternal || res.patientUid,
        bodyPartExamined: bodyPartExamined.trim(),
      });
      Alert.alert('Success', `Patient registered. UID: ${res.patientUid}`, [
        { text: 'OK', onPress: () => navigation.navigate('StudyCreation') },
      ]);
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>X-Ray Pro 5000</Text>
        <Text style={styles.subtitle}>Patient Registration</Text>

        <Text style={styles.label}>Patient Name</Text>
        <TextInput style={styles.input} value={patientName} onChangeText={setPatientName} placeholder="Full name" />

        <Text style={styles.label}>Patient ID</Text>
        <TextInput style={styles.input} value={patientId} onChangeText={setPatientId} placeholder="e.g. P001" />

        <Text style={styles.label}>Age</Text>
        <TextInput style={styles.input} value={age} onChangeText={setAge} placeholder="Age" keyboardType="number-pad" />

        <Text style={styles.label}>Gender</Text>
        <TextInput style={styles.input} value={gender} onChangeText={setGender} placeholder="M / F / Other" />

        <Text style={styles.label}>Body Part Examined</Text>
        <TextInput style={styles.input} value={bodyPartExamined} onChangeText={setBodyPartExamined} placeholder="e.g. Chest, Skull" />

        <Text style={styles.label}>Study Type</Text>
        <TextInput style={styles.input} value={studyType} onChangeText={setStudyType} placeholder="e.g. PA, LAT" />

        <TouchableOpacity style={styles.button} onPress={handleSubmit} disabled={loading}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Register Patient</Text>}
        </TouchableOpacity>

        <TouchableOpacity style={styles.link} onPress={() => navigation.navigate('PatientRecords')}>
          <Text style={styles.linkText}>View Patient Records</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.link} onPress={() => navigation.navigate('ConnectionTest')}>
          <Text style={styles.linkText}>Connection Test</Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a' },
  scroll: { padding: 24, paddingBottom: 48 },
  title: { color: '#fff', fontSize: 24, fontWeight: 'bold', marginBottom: 4 },
  subtitle: { color: '#94a3b8', fontSize: 16, marginBottom: 24 },
  label: { color: '#cbd5e1', fontSize: 14, marginBottom: 6, marginTop: 12 },
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
  link: { marginTop: 16, alignItems: 'center' },
  linkText: { color: '#60a5fa', fontSize: 14 },
});
