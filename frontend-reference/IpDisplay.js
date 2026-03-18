import React from 'react';
import { View, Text, ActivityIndicator, Button, StyleSheet } from 'react-native';
import { useDeviceIp } from './useDeviceIp';

/**
 * Component to display the current device IP address.
 * Useful for debugging connectivity issues on real devices.
 */
const IpDisplay = () => {
  const { ipAddress, loading, error, refreshIp } = useDeviceIp();

  if (loading) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="small" color="#0000ff" />
        <Text style={styles.text}>Detecting Device IP...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.label}>Device IP Address:</Text>
      <Text style={styles.ipText}>{ipAddress || 'Unknown'}</Text>
      {error && <Text style={styles.errorText}>{error}</Text>}
      <Button title="Refresh IP" onPress={refreshIp} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 20,
    backgroundColor: '#f5f5f5',
    borderRadius: 10,
    alignItems: 'center',
    margin: 10,
    borderWidth: 1,
    borderColor: '#ddd'
  },
  label: {
    fontSize: 14,
    color: '#666',
    marginBottom: 5
  },
  ipText: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10
  },
  text: {
    marginLeft: 10,
    color: '#666'
  },
  errorText: {
    color: 'red',
    fontSize: 12,
    marginBottom: 5
  }
});

export default IpDisplay;
