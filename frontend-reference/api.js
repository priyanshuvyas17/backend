import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

// Default config (fallback)
let BASE_URL = 'http://10.171.43.31:8080';

const api = axios.create({
  baseURL: BASE_URL + '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 5000,
});

// Dynamic Base URL Updater
export const setApiBaseUrl = (ip, port) => {
  if (ip && port) {
    BASE_URL = `http://${ip}:${port}`;
    api.defaults.baseURL = BASE_URL + '/api';
    console.log('[API] Updated Base URL:', api.defaults.baseURL);
  }
};

api.interceptors.request.use(async (config) => {
  const token = await AsyncStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const login = async (email, password) => {
  const response = await api.post('/auth/login', { email, password });
  
  // Save credentials and config
  await AsyncStorage.setItem('token', response.data.token);
  await AsyncStorage.setItem('userId', response.data.userId.toString());
  
  if (response.data.machineIp) {
    await AsyncStorage.setItem('lastServerIp', response.data.machineIp);
    await AsyncStorage.setItem('lastServerPort', response.data.machinePort.toString());
    setApiBaseUrl(response.data.machineIp, 8080); // Assuming backend port is 8080
  }
  
  return response.data;
};

export const register = async (name, email, password) => {
  const response = await api.post('/auth/register', { name, email, password });
  
  // Auto-login logic (same as login)
  if (response.data.token) {
      await AsyncStorage.setItem('token', response.data.token);
      await AsyncStorage.setItem('userId', response.data.userId.toString());
      
      if (response.data.machineIp) {
        await AsyncStorage.setItem('lastServerIp', response.data.machineIp);
        await AsyncStorage.setItem('lastServerPort', response.data.machinePort.toString());
        setApiBaseUrl(response.data.machineIp, 8080);
      }
  }
  
  return response.data;
};

export const initConfig = async (userId) => {
    try {
        const response = await api.post('/config/init', { userId });
        return response.data;
    } catch (error) {
        console.warn('[API] initConfig failed:', error);
        throw error;
    }
};

export const loadStoredConfig = async () => {
  const ip = await AsyncStorage.getItem('lastServerIp');
  const port = await AsyncStorage.getItem('lastServerPort'); // Not used for backend port if fixed to 8080, but useful if flexible
  if (ip) {
    setApiBaseUrl(ip, 8080);
    return { ip, port: 8080 };
  }
  return null;
};

export const checkHealth = async () => {
  try {
    const response = await api.get('/health'); // Use the new public endpoint
    // Return specific state if available, otherwise ONLINE if 200 OK
    return response.data?.state || (response.status === 200 ? 'ONLINE' : 'OFFLINE');
  } catch (e) {
    // Distinguish errors if possible, but for simple health check, throw or return OFFLINE
    if (e.code === 'ECONNABORTED') return 'TIMEOUT';
    if (e.message.includes('Network Error')) return 'NETWORK_ERROR';
    return 'OFFLINE';
  }
};

export const connectXRay = async (userId) => {
    // 1. Try to load local config first
    const stored = await loadStoredConfig();
    if (stored) {
        const healthState = await checkHealth();
        if (healthState === 'ONLINE' || healthState === 'BUSY') {
            return { data: { status: 'CONNECTED', state: healthState, ip: stored.ip, port: stored.port } };
        }
    }
    
    // 2. If no local config or unhealthy, try fetching from backend (if we can reach it)
    try {
        const response = await api.get(`/config/${userId}`);
        const { machineIp, machinePort } = response.data;
        
        if (machineIp) {
            setApiBaseUrl(machineIp, machinePort || 8080);
            await AsyncStorage.setItem('lastServerIp', machineIp);
            await AsyncStorage.setItem('lastServerPort', (machinePort || 8080).toString());
            return { data: { status: 'CONNECTED', state: 'ONLINE', ip: machineIp, port: machinePort } };
        }
    } catch (e) {
        console.log("Failed to fetch config from backend", e);
        throw e; // Throw to let caller handle error reason
    }
    
    throw new Error("Could not connect to X-Ray Machine");
};

export default api;
