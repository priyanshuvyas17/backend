/**
 * Example screen using safe API - tests backend connectivity.
 * Add to navigation to verify API works from phone.
 */
import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import {
  checkHealth,
  checkTest,
  fetchBackendConnection,
  type ApiResult,
  type BackendConnectionInfo,
} from '../api/safeApi';
import { api } from '../config/api';
import { parseBackendUrl } from '../utils/parseBackendUrl';

export default function ConnectionTestScreen() {
  const [health, setHealth] = useState<ApiResult<{ status: string }> | null>(null);
  const [test, setTest] = useState<ApiResult<Record<string, unknown>> | null>(null);
  const [conn, setConn] = useState<ApiResult<BackendConnectionInfo> | null>(null);

  const parsed = parseBackendUrl(api.base);

  const runTests = async () => {
    setHealth(null);
    setTest(null);
    setConn(null);
    const [h, t, c] = await Promise.all([checkHealth(), checkTest(), fetchBackendConnection()]);
    setHealth(h);
    setTest(t);
    setConn(c);
  };

  useEffect(() => {
    runTests();
  }, []);

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Connection Test</Text>

      <View style={styles.hostBlock}>
        <Text style={styles.kicker}>BACKEND HOST (from config)</Text>
        <Text style={styles.hostFull} selectable>
          {parsed.host}
        </Text>
        <Text style={styles.kicker}>PORT</Text>
        <Text style={styles.portLine} selectable>
          {parsed.portLabel}
        </Text>
        <Text style={styles.hint}>
          Public HTTPS on Render uses port 443 by default (no :443 in the URL). Internal container port
          (e.g. 10000) is not what the phone connects to.
        </Text>
      </View>

      {conn && conn.ok ? (
        <View style={styles.hostBlock}>
          <Text style={styles.kicker}>BACKEND HOST (from server)</Text>
          <Text style={styles.hostFull} selectable>
            {conn.data.host}
          </Text>
          <Text style={styles.kicker}>PORT</Text>
          <Text style={styles.portLine} selectable>
            {conn.data.port} — {conn.data.portDescription}
          </Text>
          <Text style={styles.monoSmall} selectable>
            {conn.data.baseUrl}
          </Text>
        </View>
      ) : conn && !conn.ok ? (
        <Text style={styles.warn}>Could not load /api/public/backend-connection: {conn.error}</Text>
      ) : null}

      <Text style={styles.url} selectable>
        API base: {api.base}
      </Text>

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
  hostBlock: {
    backgroundColor: '#1e293b',
    borderRadius: 10,
    padding: 14,
    marginBottom: 16,
  },
  kicker: { color: '#64748b', fontSize: 11, fontWeight: '600', marginBottom: 4 },
  hostFull: { color: '#f8fafc', fontSize: 15, marginBottom: 12 },
  portLine: { color: '#e2e8f0', fontSize: 15, marginBottom: 8 },
  hint: { color: '#94a3b8', fontSize: 12, lineHeight: 18 },
  monoSmall: { color: '#94a3b8', fontSize: 12, marginTop: 8 },
  warn: { color: '#fbbf24', fontSize: 13, marginBottom: 12 },
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
