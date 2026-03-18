import { useState, useEffect } from 'react';
import * as Network from 'expo-network';
import { Platform } from 'react-native';

/**
 * Custom hook to get the device's local IP address.
 * Uses expo-network to fetch the IP asynchronously.
 * 
 * @returns {Object} { ipAddress, loading, error, refreshIp }
 */
export const useDeviceIp = () => {
  const [ipAddress, setIpAddress] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchIpAddress = async () => {
    setLoading(true);
    setError(null);
    try {
      // expo-network is the standard way to get IP in Expo apps
      const ip = await Network.getIpAddressAsync();
      setIpAddress(ip);
      console.log('[useDeviceIp] Detected IP:', ip);
    } catch (err) {
      console.error('[useDeviceIp] Error fetching IP:', err);
      setError('Failed to detect IP');
      // Fallback for development if needed, or handle UI accordingly
      setIpAddress('0.0.0.0'); 
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIpAddress();
  }, []);

  return {
    ipAddress,
    loading,
    error,
    refreshIp: fetchIpAddress
  };
};
