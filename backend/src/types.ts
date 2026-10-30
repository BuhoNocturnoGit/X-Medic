export interface Env {
  FIREBASE_PROJECT_ID: string;
  ADMOB_REWARDED_AD_UNIT_ID: string;
  FIREBASE_SERVICE_ACCOUNT_EMAIL?: string;
  FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY?: string;
}

export interface DecodedToken {
  uid: string;
  email?: string;
  name?: string;
  iss: string;
  aud: string;
  exp: number;
  iat: number;
}

export interface UserTokenProfile {
  uid: string;
  displayName?: string;
  email?: string;
  role?: string;
  onboardingCompleted?: boolean;
  tokens: number;
  tokensInitialized: boolean;
  tokensInitializedAt?: string;
  tokensUpdatedAt?: string;
}

export interface InitializeResponse {
  success: boolean;
  initialized: boolean;
  balance: number;
  message?: string;
}

export interface BalanceResponse {
  uid: string;
  balance: number;
  initialized: boolean;
}

export interface ReserveResponse {
  success: boolean;
  operationId: string;
  status: 'RESERVED' | 'ALREADY_RESERVED';
  cost: number;
  balance: number;
  error?: string;
}

export interface AdSessionResponse {
  sessionId: string;
  customData: string;
  createdAt: string;
}

export interface AdMobSsvQueryParams {
  ad_network?: string;
  ad_unit?: string;
  custom_data?: string;
  key_id?: string;
  reward_amount?: string;
  reward_item?: string;
  timestamp?: string;
  transaction_id?: string;
  signature?: string;
}
