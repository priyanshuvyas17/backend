import React, { useEffect } from 'react';
import { StatusBar } from 'expo-status-bar';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { WorkflowProvider } from './src/context/WorkflowContext';
import { ErrorBoundary } from './src/components/ErrorBoundary';
import { setupGlobalErrorHandler } from './src/utils/globalErrorHandler';
import PatientRegistrationScreen from './src/screens/PatientRegistrationScreen';
import StudyCreationScreen from './src/screens/StudyCreationScreen';
import AcquisitionConsoleScreen from './src/screens/AcquisitionConsoleScreen';
import ImagePreviewScreen from './src/screens/ImagePreviewScreen';
import FinishStudyScreen from './src/screens/FinishStudyScreen';
import PatientRecordsScreen from './src/screens/PatientRecordsScreen';
import PacsViewerScreen from './src/screens/PacsViewerScreen';
import ConnectionTestScreen from './src/screens/ConnectionTestScreen';
import type { RootStackParamList } from './src/navigation/types';

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  useEffect(() => {
    setupGlobalErrorHandler();
  }, []);

  return (
    <ErrorBoundary>
    <WorkflowProvider>
      <NavigationContainer>
        <StatusBar style="light" />
        <Stack.Navigator
          initialRouteName="PatientRegistration"
          screenOptions={{
            headerStyle: { backgroundColor: '#0f172a' },
            headerTintColor: '#fff',
            headerTitleStyle: { fontWeight: 'bold' },
          }}
        >
          <Stack.Screen name="PatientRegistration" component={PatientRegistrationScreen} options={{ title: 'X-Ray Pro 5000' }} />
          <Stack.Screen name="StudyCreation" component={StudyCreationScreen} options={{ title: 'Create Study' }} />
          <Stack.Screen name="AcquisitionConsole" component={AcquisitionConsoleScreen} options={{ title: 'Acquisition Console' }} />
          <Stack.Screen name="ImagePreview" component={ImagePreviewScreen} options={{ title: 'Image Preview' }} />
          <Stack.Screen name="FinishStudy" component={FinishStudyScreen} options={{ title: 'Finish Study', headerBackVisible: false }} />
          <Stack.Screen name="PatientRecords" component={PatientRecordsScreen} options={{ title: 'Patient Records' }} />
          <Stack.Screen name="PacsViewer" component={PacsViewerScreen} options={{ title: 'DICOM Viewer' }} />
          <Stack.Screen name="ConnectionTest" component={ConnectionTestScreen} options={{ title: 'Connection Test' }} />
        </Stack.Navigator>
      </NavigationContainer>
    </WorkflowProvider>
    </ErrorBoundary>
  );
}
