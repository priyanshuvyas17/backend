import React, { useState } from 'react';
import { View, Text, TextInput, Button, StyleSheet, Alert } from 'react-native';
import { login, initConfig } from './api';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function LoginScreen({ navigation }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!email || !password) {
      Alert.alert('Error', 'Please enter both email and password');
      return;
    }

    setLoading(true);
    try {
      console.log('[Login] Attempting login for:', email);
      const data = await login(email, password);
      console.log('[Login] Success, received data:', data);
      
      // Explicitly show success alert and wait for user interaction
      Alert.alert(
        'Success',
        'Login successful',
        [
          { 
            text: 'OK', 
            onPress: async () => {
              console.log('[Login] OK pressed, navigating to Dashboard');
              try {
                // Try to initialize config, but don't block navigation if it fails
                if (data.userId) {
                    await initConfig(data.userId);
                }
              } catch (e) {
                console.log("[Login] Config init warning (non-fatal):", e);
              }
              
              // Use replace to prevent going back to login
              navigation.replace('Dashboard');
            }
          }
        ],
        { cancelable: false }
      );
      
    } catch (error) {
      console.error('[Login] Error:', error);
      Alert.alert('Error', 'Login failed: ' + (error.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>X-Ray Control Login</Text>
      <TextInput
        style={styles.input}
        placeholder="Email"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
      />
      <TextInput
        style={styles.input}
        placeholder="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
      />
      <View style={styles.buttonContainer}>
        <Button 
            title={loading ? "Logging in..." : "Login"} 
            onPress={handleLogin} 
            disabled={loading}
        />
      </View>
      <View style={styles.buttonContainer}>
        <Button 
            title="Register" 
            onPress={() => navigation.navigate('Register')} 
            color="gray"
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 20 },
  title: { fontSize: 24, marginBottom: 20, textAlign: 'center' },
  input: { borderWidth: 1, borderColor: '#ccc', padding: 10, marginBottom: 10, borderRadius: 5 },
  buttonContainer: { marginBottom: 10 }
});
