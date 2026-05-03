export interface LoginRequest {
  loginChallenge: string;
  username: string;
  password: string;
  tenantId?: string;
}

export interface TenantSelectRequest {
  loginChallenge: string;
  userId: string;
  selectedTenantId: string;
  availableTenantIds: string[];
  mfaRequired: boolean;
}

export interface MfaVerifyRequest {
  loginChallenge: string;
  userId: string;
  tenantId: string;
  challengeToken: string;
  userResponse: string;
}

export interface LoginResponse {
  redirectTo?: string;
  mfaRequired: boolean;
  mfaChallengeToken?: string;
  mfaHint?: string;
  tenantSelectionRequired: boolean;
  availableTenantIds?: string[];
  userId?: string;
  tenantId?: string;
}

export interface HydraLoginRequest {
  challenge: string;
  skip: boolean;
  subject: string;
  client: {
    clientId: string;
    clientName: string;
  };
}

export type LoginStep = 'credentials' | 'tenant-select' | 'mfa';
