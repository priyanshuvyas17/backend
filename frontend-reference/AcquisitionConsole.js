import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Alert, ActivityIndicator } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { loadStoredConfig } from './api';

// Configuration
const RECONNECT_BASE_DELAY = 1000;
const RECONNECT_MAX_DELAY = 30000;
const HEARTBEAT_TIMEOUT = 10000; // 10 seconds without heartbeat = offline

export default function AcquisitionConsole() {
  const [status, setStatus] = useState('DISCONNECTED'); // CONNECTED, RECONNECTING, DISCONNECTED
  const [deviceInfo, setDeviceInfo] = useState({ ip: '-', port: '-' });
  const [lastHeartbeat, setLastHeartbeat] = useState(null);
  const [selectedPlan, setSelectedPlan] = useState('Skull PA');
  
  // Reconnection refs
  const stompClient = useRef(null);
  const reconnectTimeout = useRef(null);
  const reconnectAttempt = useRef(0);
  const heartbeatWatchdog = useRef(null);
  const isMounted = useRef(true);

  useEffect(() => {
    isMounted.current = true;
    initializeConnection();

    return () => {
      isMounted.current = false;
      disconnect();
    };
  }, []);

  // Heartbeat Watchdog
  useEffect(() => {
    if (status === 'CONNECTED') {
      heartbeatWatchdog.current = setInterval(() => {
        const now = Date.now();
        if (lastHeartbeat && (now - lastHeartbeat > HEARTBEAT_TIMEOUT)) {
          console.warn('[Watchdog] No heartbeat received. Forcing reconnect.');
          handleDisconnect();
        }
      }, 2000);
    } else {
      if (heartbeatWatchdog.current) clearInterval(heartbeatWatchdog.current);
    }
    return () => {
        if (heartbeatWatchdog.current) clearInterval(heartbeatWatchdog.current);
    };
  }, [status, lastHeartbeat]);

  const initializeConnection = async () => {
    const config = await loadStoredConfig();
    if (config) {
      setDeviceInfo({ ip: config.ip, port: config.port });
      connectWebSocket(config.ip, config.port);
    } else {
      setStatus('DISCONNECTED');
      // Maybe prompt user to configure IP
    }
  };

  const connectWebSocket = (ip, port) => {
    if (stompClient.current && stompClient.current.active) return;

    const wsUrl = `http://${ip}:${port}/ws/device-status`;
    console.log('[WebSocket] Connecting to:', wsUrl);

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 0, // We handle reconnection manually for exponential backoff
      debug: (str) => console.log('[STOMP]', str),
      
      onConnect: () => {
        console.log('[WebSocket] Connected');
        if (!isMounted.current) return;
        
        setStatus('CONNECTED');
        reconnectAttempt.current = 0;
        setLastHeartbeat(Date.now());

        // Subscribe to Heartbeat
        client.subscribe('/topic/status', (message) => {
          if (!isMounted.current) return;
          try {
            const data = JSON.parse(message.body);
            console.log('[Heartbeat]', data);
            setLastHeartbeat(Date.now());
            if (data.status === 'ONLINE') {
               setStatus('CONNECTED');
            }
          } catch (e) {
            console.error('Failed to parse heartbeat', e);
          }
        });
      },

      onStompError: (frame) => {
        console.error('[WebSocket] Broker error:', frame.headers['message']);
        handleDisconnect();
      },

      onWebSocketClose: () => {
        console.log('[WebSocket] Connection closed');
        handleDisconnect();
      }
    });

    client.activate();
    stompClient.current = client;
  };

  const disconnect = () => {
    if (stompClient.current) {
      stompClient.current.deactivate();
      stompClient.current = null;
    }
    if (reconnectTimeout.current) clearTimeout(reconnectTimeout.current);
    if (heartbeatWatchdog.current) clearInterval(heartbeatWatchdog.current);
  };

  const handleDisconnect = () => {
    if (!isMounted.current) return;
    setStatus('RECONNECTING');
    
    // Exponential Backoff
    const delay = Math.min(
      RECONNECT_BASE_DELAY * Math.pow(2, reconnectAttempt.current),
      RECONNECT_MAX_DELAY
    );
    
    console.log(`[WebSocket] Reconnecting in ${delay}ms (Attempt ${reconnectAttempt.current + 1})`);
    
    if (reconnectTimeout.current) clearTimeout(reconnectTimeout.current);
    
    reconnectTimeout.current = setTimeout(() => {
      reconnectAttempt.current += 1;
      // Use current IP/Port from state or refetch
      if (deviceInfo.ip !== '-') {
          connectWebSocket(deviceInfo.ip, deviceInfo.port);
      }
    }, delay);
  };

  // UI Components matching the screenshot
  const PlanButton = ({ title }) => (
    <TouchableOpacity 
      style={[styles.planButton, selectedPlan === title && styles.planButtonActive]}
      onPress={() => setSelectedPlan(title)}
    >
      <Text style={[styles.planText, selectedPlan === title && styles.planTextActive]}>{title}</Text>
    </TouchableOpacity>
  );

  const ParamBox = ({ label, value }) => (
    <View style={styles.paramBox}>
      <Text style={styles.paramLabel}>{label}</Text>
      <Text style={styles.paramValue}>{value}</Text>
    </View>
  );

  return (
    <ScrollView style={styles.container}>
      {/* Header / Top Bar */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>X-Ray Acquisition Console</Text>
      </View>

      {/* Operator & Device Info Row */}
      <View style={styles.row}>
        {/* Operator Card */}
        <View style={styles.card}>
          <Text style={styles.cardLabel}>Operator</Text>
          <Text style={styles.cardValue}>Dr.Vyas</Text>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>ID</Text>
            <Text style={styles.infoValue}>D-10293</Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>DOB</Text>
            <Text style={styles.infoValue}>2003-05-14</Text>
          </View>
        </View>

        {/* Device Card */}
        <View style={styles.card}>
          <Text style={styles.cardLabel}>Device</Text>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>IP</Text>
            <Text style={styles.infoValue}>{deviceInfo.ip}</Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Port</Text>
            <Text style={styles.infoValue}>{deviceInfo.port}</Text>
          </View>
          <View style={styles.statusContainer}>
            <View style={[
              styles.statusDot, 
              status === 'CONNECTED' ? styles.statusOnline : 
              status === 'RECONNECTING' ? styles.statusReconnecting : styles.statusOffline
            ]} />
            <Text style={styles.statusText}>{status}</Text>
          </View>
        </View>
      </View>

      {/* Exposure Plan */}
      <View style={styles.sectionContainer}>
        <Text style={styles.sectionTitle}>Exposure Plan</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.planScroll}>
          <PlanButton title="Skull PA" />
          <PlanButton title="Skull LAT" />
          <PlanButton title="Nasal Bones" />
          <PlanButton title="Sinus" />
        </ScrollView>
      </View>

      {/* Parameters */}
      <View style={styles.sectionContainer}>
        <Text style={styles.sectionTitle}>Parameters</Text>
        <View style={styles.paramsRow}>
          <ParamBox label="kVp" value="80" />
          <ParamBox label="mAs" value="5" />
          <ParamBox label="DAP" value="0.5" />
        </View>
      </View>

      {/* X-Ray Area Placeholder */}
      <View style={styles.xrayArea}>
        <Text style={{color: '#333', fontSize: 50}}>☢️</Text>
      </View>

      {/* Footer Buttons */}
      <View style={styles.footer}>
        <TouchableOpacity style={[styles.actionButton, styles.exposeButton]} disabled={status !== 'CONNECTED'}>
          <Text style={styles.actionButtonText}>⚡ Expose</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.actionButton, styles.finishButton]}>
          <Text style={styles.actionButtonText}>✓ Finish Study</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.actionButton, styles.printButton]}>
          <Text style={styles.actionButtonText}>🖨 Print Study</Text>
        </TouchableOpacity>
      </View>

    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0f172a', padding: 10 },
  header: { marginBottom: 20, marginTop: 40 },
  headerTitle: { color: '#fff', fontSize: 20, fontWeight: 'bold' },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
  card: { backgroundColor: '#1e293b', borderRadius: 10, padding: 15, width: '48%' },
  cardLabel: { color: '#94a3b8', fontSize: 12, marginBottom: 5 },
  cardValue: { color: '#fff', fontSize: 16, fontWeight: 'bold', marginBottom: 10 },
  infoRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 5 },
  infoLabel: { color: '#64748b', fontSize: 12 },
  infoValue: { color: '#cbd5e1', fontSize: 12, fontWeight: 'bold' },
  statusContainer: { flexDirection: 'row', alignItems: 'center', marginTop: 10 },
  statusDot: { width: 8, height: 8, borderRadius: 4, marginRight: 8 },
  statusOnline: { backgroundColor: '#10b981' },
  statusReconnecting: { backgroundColor: '#f59e0b' },
  statusOffline: { backgroundColor: '#ef4444' },
  statusText: { color: '#fff', fontSize: 12, fontWeight: 'bold' },
  sectionContainer: { backgroundColor: '#fff', borderRadius: 10, padding: 15, marginBottom: 20 },
  sectionTitle: { color: '#0f172a', fontSize: 14, fontWeight: 'bold', marginBottom: 10 },
  planScroll: { flexDirection: 'row' },
  planButton: { backgroundColor: '#f1f5f9', paddingVertical: 8, paddingHorizontal: 16, borderRadius: 20, marginRight: 10 },
  planButtonActive: { backgroundColor: '#3b82f6' },
  planText: { color: '#64748b', fontWeight: '600' },
  planTextActive: { color: '#fff' },
  paramsRow: { flexDirection: 'row', justifyContent: 'space-between' },
  paramBox: { borderWidth: 1, borderColor: '#e2e8f0', borderRadius: 10, padding: 15, width: '30%', alignItems: 'center' },
  paramLabel: { color: '#64748b', fontSize: 12, marginBottom: 5 },
  paramValue: { color: '#0f172a', fontSize: 24, fontWeight: 'bold' },
  xrayArea: { height: 200, backgroundColor: '#111', borderRadius: 10, justifyContent: 'center', alignItems: 'center', marginBottom: 20, borderColor: '#333', borderWidth: 1 },
  footer: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
  actionButton: { paddingVertical: 12, paddingHorizontal: 15, borderRadius: 8, flex: 1, marginHorizontal: 2, alignItems: 'center' },
  exposeButton: { backgroundColor: '#3b82f6' },
  finishButton: { backgroundColor: '#0ea5e9' },
  printButton: { backgroundColor: '#0284c7' },
  actionButtonText: { color: '#fff', fontWeight: 'bold', fontSize: 12 },
});
