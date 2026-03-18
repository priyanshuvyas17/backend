import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

// Configuration
const WS_URL = 'http://10.171.43.31:8080/ws/device-status'; // Use correct IP
const REST_URL = 'http://10.171.43.31:8080/api/device/battery';

const BatteryIndicator = ({ onExposureLockChange }) => {
    const [battery, setBattery] = useState({
        percentage: 100,
        status: 'FULL',
        plugged: true
    });
    const [connected, setConnected] = useState(false);

    useEffect(() => {
        // 1. Initial Fetch via REST
        fetch(REST_URL)
            .then(res => res.json())
            .then(data => updateBatteryState(data))
            .catch(err => console.error('Initial battery fetch failed', err));

        // 2. WebSocket Setup
        const client = new Client({
            brokerURL: undefined, // Using SockJS, so brokerURL is not used directly
            webSocketFactory: () => new SockJS(WS_URL),
            onConnect: () => {
                setConnected(true);
                client.subscribe('/topic/battery', message => {
                    const data = JSON.parse(message.body);
                    updateBatteryState(data);
                });
            },
            onDisconnect: () => {
                setConnected(false);
            },
            onStompError: (frame) => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
            },
        });

        client.activate();

        return () => {
            client.deactivate();
        };
    }, []);

    const updateBatteryState = (data) => {
        setBattery(data);
        
        // Business Rule: Disable Exposure if < 15%
        if (onExposureLockChange) {
            const shouldLock = data.percentage < 15;
            onExposureLockChange(shouldLock);
        }
    };

    // UI Helpers
    const getBatteryColor = (pct) => {
        if (pct < 20) return '#FF3B30'; // Red
        if (pct < 50) return '#FFCC00'; // Yellow
        return '#34C759'; // Green
    };

    return (
        <View style={styles.container}>
            <View style={[styles.indicator, { backgroundColor: getBatteryColor(battery.percentage) }]}>
                <Text style={styles.text}>{battery.percentage}%</Text>
            </View>
            <Text style={styles.statusText}>
                {battery.status} {battery.plugged ? '⚡' : ''}
            </Text>
            {!connected && <Text style={styles.offlineText}>⚠️ Reconnecting...</Text>}
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 10,
        backgroundColor: '#1C1C1E',
        borderRadius: 8,
    },
    indicator: {
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 4,
        marginRight: 8,
    },
    text: {
        color: '#000',
        fontWeight: 'bold',
        fontSize: 14,
    },
    statusText: {
        color: '#FFF',
        fontSize: 14,
    },
    offlineText: {
        color: 'orange',
        fontSize: 12,
        marginLeft: 10,
    }
});

export default BatteryIndicator;
