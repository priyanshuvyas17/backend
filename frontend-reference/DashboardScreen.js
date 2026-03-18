import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, Button, ActivityIndicator, TouchableOpacity, Alert } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import api, { connectXRay, checkHealth } from './api';

export default function DashboardScreen() {
  const [status, setStatus] = useState('Disconnected');
  const [machineState, setMachineState] = useState('OFFLINE');
  const [errorReason, setErrorReason] = useState('');
  const [config, setConfig] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    checkConnection();
    
    // Real-time polling
    const interval = setInterval(async () => {
        const health = await checkHealth();
        if (health === 'ONLINE' || health === 'BUSY') {
             setStatus('CONNECTED');
             setMachineState(health);
             setErrorReason('');
        } else {
             setStatus('Disconnected');
             setMachineState('OFFLINE');
             setErrorReason(health === 'OFFLINE' ? 'Unreachable' : health);
        }
    }, 5000);
    
    return () => clearInterval(interval);
  }, []);

  const checkConnection = async () => {
    setLoading(true);
    try {
      const userId = await AsyncStorage.getItem('userId');
      // Attempt connection
      const response = await connectXRay(userId);
      setStatus(response.data.status);
      setMachineState(response.data.state || 'ONLINE');
      setConfig({ ip: response.data.ip, port: response.data.port });
      setErrorReason('');
    } catch (error) {
      console.log('Connection failed:', error);
      setStatus('Disconnected');
      setMachineState('OFFLINE');
      if (error.message.includes('Network Error')) {
          setErrorReason('Network Error - Check IP/Wifi');
      } else if (error.code === 'ECONNABORTED') {
          setErrorReason('Timeout - Device unresponsive');
      } else {
          setErrorReason(error.message || 'Unknown Error');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCapture = async () => {
    if (status !== 'CONNECTED') {
      Alert.alert('Error', 'Machine is offline. Please connect first.');
      return;
    }
    Alert.alert('Capture', 'Initiating X-Ray Capture...');
    setLoading(true);
    setMachineState('BUSY');
    try {
      const userId = await AsyncStorage.getItem('userId');
      const response = await api.post('/xray/capture', { userId });
      const message = response?.data?.message || response?.data?.status || 'Capture complete.';
      Alert.alert('Capture', message);
      setMachineState(response?.data?.state || 'ONLINE');
    } catch (error) {
      console.log('Capture failed:', error);
      const message =
        error?.response?.data?.error ||
        error?.response?.data?.message ||
        error?.message ||
        'Unknown Error';
      Alert.alert('Capture Failed', message);
      setMachineState('ONLINE');
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = () => {
    Alert.alert('Upload', 'Select image to upload...');
    // TODO: Implement upload logic
  };

  const isConnected = status === 'CONNECTED';

  return (
    <View style={styles.container}>
      <Text style={styles.header}>X-Ray Machine Dashboard</Text>
      
      <View style={styles.card}>
        <Text style={styles.label}>Connection Status:</Text>
        <Text style={[styles.status, { color: isConnected ? 'green' : 'red' }]}>
          {status} {isConnected ? `(${machineState})` : ''}
        </Text>
        {!isConnected && errorReason ? (
            <Text style={styles.errorText}>Reason: {errorReason}</Text>
        ) : null}
      </View>

      {config && (
        <View style={styles.card}>
          <Text style={styles.label}>Machine Configuration (Read-Only):</Text>
          <Text>IP: {config.ip}</Text>
          <Text>Port: {config.port}</Text>
        </View>
      )}

      <View style={styles.buttonContainer}>
        <TouchableOpacity 
            style={[styles.button, styles.captureButton, !isConnected && styles.disabledButton]} 
            onPress={handleCapture}
            disabled={!isConnected}
        >
          <Text style={styles.buttonText}>Capture X-Ray</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={[styles.button, styles.uploadButton]} onPress={handleUpload}>
          <Text style={styles.buttonText}>Upload Image</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <ActivityIndicator size="large" style={{ marginTop: 20 }} />
      ) : (
        <Button title="Retry Connection" onPress={checkConnection} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, paddingTop: 50, backgroundColor: '#fff' },
  header: { fontSize: 24, fontWeight: 'bold', marginBottom: 20, textAlign: 'center' },
  card: { padding: 15, backgroundColor: '#f0f0f0', borderRadius: 10, marginBottom: 20 },
  label: { fontWeight: 'bold', marginBottom: 5, color: '#555' },
  status: { fontSize: 18, fontWeight: 'bold' },
  errorText: { color: 'red', marginTop: 5, fontStyle: 'italic' },
  buttonContainer: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
  button: { flex: 1, padding: 15, borderRadius: 8, alignItems: 'center', marginHorizontal: 5 },
  captureButton: { backgroundColor: '#007AFF' },
  uploadButton: { backgroundColor: '#34C759' },
  disabledButton: { backgroundColor: '#ccc' },
  buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 16 }
});
