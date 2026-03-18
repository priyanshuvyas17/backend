/**
 * Example screen using safe API - tests backend connectivity.
 * Add to navigation to verify API works from phone.
 */
import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { checkHealth, checkTest, type ApiResult } from '../api/safeApi';
import { api } from '../config/api';

export default function ConnectionTestScreen() {
  const [health, setHealth] = useState<ApiResult<{ status: string }> | null>(null);
  const [test, setTest] = useState<ApiResult<Record<string, unknown>> | null>(null);

  const runTests = async () => {
    setHealth(null);
    setTest(null);
    const [h, t] = await Promise.all([checkHealth(), checkTest()]);
    setHealth(h);
    setTest(t);
  };

  useEffect(() => {
    runTests();
  }, []);

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Connection Test</Text>
      <Text style={styles.url}>Backend: {api.base}</Text>

      <View style={styles.section}>
        <Text style={styles.label}>GET /health</Text>
        {health === null ? (
          <Text style={styles.pending}>Loading...</Text>
        ) : health.ok ? (
          <Text style={styles.success}>✓ {JSON.stringify(health.data)}</Text>
        ) : (
          <Text style={styles.error}>✗ {health.error}</Text>
        )}
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>GET /test</Text>
        {test === null ? (
          <Text style={styles.pending}>Loading...</Text>
        ) : test.ok ? (
          <Text style={styles.success}>✓ {JSON.stringify(test.data)}</Text>
        ) : (
          <Text style={styles.error}>✗ {test.error}</Text>
        )}
      </View>

      <TouchableOpacity style={styles.button} onPress={runTests}>
        <Text style={styles.buttonText}>Retry</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a' },
  content: { padding: 24 },
  title: { color: '#fff', fontSize: 22, fontWeight: 'bold', marginBottom: 8 },
  url: { color: '#94a3b8', fontSize: 14, marginBottom: 24 },
  section: { marginBottom: 16 },
  label: { color: '#cbd5e1', fontSize: 14, marginBottom: 4 },
  pending: { color: '#64748b', fontSize: 14 },
  success: { color: '#10b981', fontSize: 14 },
  error: { color: '#ef4444', fontSize: 14 },
  button: {
    backgroundColor: '#3b82f6',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 16,
  },
  buttonText: { color: '#fff', fontWeight: 'bold' },
});
