import React, { createContext, useContext, useState, ReactNode } from 'react';

export type WorkflowState = {
  patientUid: string | null;
  patientName: string | null;
  patientId: string | null;
  bodyPartExamined: string | null;
  studyUid: string | null;
  studyDate: string | null;
  modality: string;
  operatorName: string;
  deviceIp: string;
};

const defaultState: WorkflowState = {
  patientUid: null,
  patientName: null,
  patientId: null,
  bodyPartExamined: null,
  studyUid: null,
  studyDate: null,
  modality: 'XRAY',
  operatorName: 'Operator',
  deviceIp: '192.168.1.1',
};

const WorkflowContext = createContext<{
  state: WorkflowState;
  setPatient: (p: Partial<Pick<WorkflowState, 'patientUid' | 'patientName' | 'patientId' | 'bodyPartExamined'>>) => void;
  setStudy: (s: Partial<Pick<WorkflowState, 'studyUid' | 'studyDate'>>) => void;
  setOperator: (name: string, ip: string) => void;
  reset: () => void;
} | null>(null);

export function WorkflowProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<WorkflowState>(defaultState);

  const setPatient = (p: Partial<WorkflowState>) => {
    setState((s) => ({ ...s, ...p }));
  };

  const setStudy = (s: Partial<WorkflowState>) => {
    setState((prev) => ({ ...prev, ...s }));
  };

  const setOperator = (name: string, ip: string) => {
    setState((s) => ({ ...s, operatorName: name, deviceIp: ip }));
  };

  const reset = () => setState(defaultState);

  return (
    <WorkflowContext.Provider value={{ state, setPatient, setStudy, setOperator, reset }}>
      {children}
    </WorkflowContext.Provider>
  );
}

export function useWorkflow() {
  const ctx = useContext(WorkflowContext);
  if (!ctx) throw new Error('useWorkflow must be used within WorkflowProvider');
  return ctx;
}
