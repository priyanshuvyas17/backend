export type RootStackParamList = {
  PatientRegistration: undefined;
  StudyCreation: undefined;
  AcquisitionConsole: undefined;
  ImagePreview: { imageUri: string };
  FinishStudy: undefined;
  PatientRecords: undefined;
  PacsViewer: { studyUid?: string; instanceId?: string };
  ConnectionTest: undefined;
};
