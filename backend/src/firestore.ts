import { Env, UserTokenProfile } from './types';
import { TOKEN_CONFIG } from './config';

/**
 * Convierte un objeto JavaScript simple a formato Value Document de Firestore REST API.
 */
function toFirestoreFields(obj: Record<string, any>): Record<string, any> {
  const fields: Record<string, any> = {};
  for (const [key, val] of Object.entries(obj)) {
    if (val === null || val === undefined) {
      fields[key] = { nullValue: null };
    } else if (typeof val === 'boolean') {
      fields[key] = { booleanValue: val };
    } else if (typeof val === 'number') {
      if (Number.isInteger(val)) {
        fields[key] = { integerValue: val.toString() };
      } else {
        fields[key] = { doubleValue: val };
      }
    } else if (typeof val === 'string') {
      fields[key] = { stringValue: val };
    } else if (val instanceof Date) {
      fields[key] = { timestampValue: val.toISOString() };
    } else if (typeof val === 'object') {
      fields[key] = { mapValue: { fields: toFirestoreFields(val) } };
    }
  }
  return fields;
}

/**
 * Convierte un objeto Value Document de Firestore REST API a un objeto JavaScript simple.
 */
function fromFirestoreFields(fields: Record<string, any> = {}): Record<string, any> {
  const obj: Record<string, any> = {};
  for (const [key, val] of Object.entries(fields)) {
    if ('stringValue' in val) obj[key] = val.stringValue;
    else if ('integerValue' in val) obj[key] = parseInt(val.integerValue, 10);
    else if ('doubleValue' in val) obj[key] = parseFloat(val.doubleValue);
    else if ('booleanValue' in val) obj[key] = val.booleanValue;
    else if ('timestampValue' in val) obj[key] = val.timestampValue;
    else if ('nullValue' in val) obj[key] = null;
    else if ('mapValue' in val) obj[key] = fromFirestoreFields(val.mapValue.fields || {});
  }
  return obj;
}

/**
 * Cliente REST de Cloud Firestore optimizado para Cloudflare Workers
 */
export class FirestoreClient {
  private projectId: string;
  private baseUrl: string;

  constructor(env: Env) {
    this.projectId = env.FIREBASE_PROJECT_ID || 'x-medic';
    this.baseUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents`;
  }

  /**
   * Obtiene un documento de Firestore por su ruta
   */
  async getDocument(path: string): Promise<Record<string, any> | null> {
    const url = `${this.baseUrl}/${path}`;
    const res = await fetch(url);
    if (res.status === 404) {
      return null;
    }
    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`Firestore REST GET error (${res.status}): ${errText}`);
    }
    const data = await res.json() as any;
    return fromFirestoreFields(data.fields || {});
  }

  /**
   * Inicializa de forma idempotente y atómica el bono de 120 tokens para un UID
   */
  async initializeTokens(uid: string, initialProfile?: Partial<UserTokenProfile>): Promise<{ initialized: boolean; balance: number }> {
    const userDocPath = `users/${uid}`;
    const existing = await this.getDocument(userDocPath);

    if (existing && existing.tokensInitialized === true && typeof existing.tokens === 'number') {
      return { initialized: false, balance: existing.tokens };
    }

    const now = new Date().toISOString();
    const initialTokens = TOKEN_CONFIG.INITIAL_TOKENS;

    const updatedProfile = {
      uid,
      displayName: existing?.displayName || initialProfile?.displayName || 'Usuario XMedic',
      email: existing?.email || initialProfile?.email || '',
      role: existing?.role || 'PACIENTE',
      onboardingCompleted: existing?.onboardingCompleted ?? true,
      tokens: initialTokens,
      tokensInitialized: true,
      tokensInitializedAt: existing?.tokensInitializedAt || now,
      tokensUpdatedAt: now
    };

    const commitUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents:commit`;
    const txId = `tx_init_${uid}_${Date.now()}`;

    const commitPayload = {
      writes: [
        {
          update: {
            name: `projects/${this.projectId}/databases/(default)/documents/users/${uid}`,
            fields: toFirestoreFields(updatedProfile)
          }
        },
        {
          update: {
            name: `projects/${this.projectId}/databases/(default)/documents/token_transactions/${txId}`,
            fields: toFirestoreFields({
              transactionId: txId,
              uid,
              type: 'INITIAL_BONUS',
              amount: initialTokens,
              timestamp: now
            })
          }
        }
      ]
    };

    const res = await fetch(commitUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(commitPayload)
    });

    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Error en transacción de inicialización en Firestore: ${err}`);
    }

    return { initialized: true, balance: initialTokens };
  }

  /**
   * Obtiene el saldo oficial de tokens del usuario desde Firestore
   */
  async getBalance(uid: string): Promise<number> {
    const userDoc = await this.getDocument(`users/${uid}`);
    if (!userDoc || userDoc.tokensInitialized !== true) {
      const initResult = await this.initializeTokens(uid);
      return initResult.balance;
    }
    return typeof userDoc.tokens === 'number' ? userDoc.tokens : TOKEN_CONFIG.INITIAL_TOKENS;
  }

  /**
   * Reserva y descuenta transaccionalmente 5 tokens para un análisis médico (idempotente por operationId)
   */
  async reserveAnalysisTokens(uid: string, operationId: string): Promise<{ status: 'RESERVED' | 'ALREADY_RESERVED'; cost: number; balance: number }> {
    const cost = TOKEN_CONFIG.ANALYSIS_COST;
    const opPath = `analysis_operations/${operationId}`;
    const existingOp = await this.getDocument(opPath);

    if (existingOp && existingOp.status === 'RESERVED') {
      const currentBalance = await this.getBalance(uid);
      return { status: 'ALREADY_RESERVED', cost, balance: currentBalance };
    }

    const userDocPath = `users/${uid}`;
    const userDoc = await this.getDocument(userDocPath);
    const currentTokens = userDoc && typeof userDoc.tokens === 'number' ? userDoc.tokens : await this.getBalance(uid);

    if (currentTokens < cost) {
      throw new Error(`INSUFFICIENT_TOKENS: Saldo actual (${currentTokens}) inferior al costo requerido (${cost})`);
    }

    const newTokens = currentTokens - cost;
    const now = new Date().toISOString();
    const txId = `tx_reserve_${operationId}`;

    const commitUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents:commit`;
    const commitPayload = {
      writes: [
        {
          update: {
            name: `projects/${this.projectId}/databases/(default)/documents/users/${uid}`,
            fields: toFirestoreFields({
              ...userDoc,
              tokens: newTokens,
              tokensUpdatedAt: now
            })
          }
        },
        {
          update: {
            name: `projects/${this.projectId}/databases/(default)/documents/analysis_operations/${operationId}`,
            fields: toFirestoreFields({
              operationId,
              uid,
              status: 'RESERVED',
              cost,
              createdAt: now
            })
          }
        },
        {
          update: {
            name: `projects/${this.projectId}/databases/(default)/documents/token_transactions/${txId}`,
            fields: toFirestoreFields({
              transactionId: txId,
              uid,
              type: 'ANALYSIS_RESERVE',
              amount: -cost,
              operationId,
              timestamp: now
            })
          }
        }
      ]
    };

    const res = await fetch(commitUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(commitPayload)
    });

    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Error reservando tokens de análisis en Firestore: ${err}`);
    }

    return { status: 'RESERVED', cost, balance: newTokens };
  }

  /**
   * Registra y valida una sesión de anuncio recompensado AdMob
   */
  async createAdSession(uid: string): Promise<string> {
    const sessionId = `ad_sess_${crypto.randomUUID()}`;
    const now = new Date().toISOString();

    const commitUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents:commit`;
    const commitPayload = {
      writes: [
        {
          update: {
            name: `projects/${this.projectId}/databases/(default)/documents/ad_sessions/${sessionId}`,
            fields: toFirestoreFields({
              sessionId,
              uid,
              status: 'PENDING',
              createdAt: now
            })
          }
        }
      ]
    };

    const res = await fetch(commitUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(commitPayload)
    });

    if (!res.ok) {
      throw new Error('Error al registrar sesión de anuncio AdMob');
    }

    return sessionId;
  }

  /**
   * Procesa de forma atómica e idempotente la recompensa de +2 tokens desde AdMob SSV
   */
  async processAdReward(uid: string, sessionId: string, transactionId: string, rewardAmount: number = TOKEN_CONFIG.REWARDED_AD_TOKENS): Promise<{ rewardAmount: number; newBalance: number }> {
    const rewardPath = `ad_rewards/${transactionId}`;
    const existingReward = await this.getDocument(rewardPath);

    if (existingReward) {
      const balance = await this.getBalance(uid);
      return { rewardAmount, newBalance: balance };
    }

    const userDoc = await this.getDocument(`users/${uid}`);
    const currentTokens = userDoc && typeof userDoc.tokens === 'number' ? userDoc.tokens : await this.getBalance(uid);
    const newTokens = currentTokens + rewardAmount;
    const now = new Date().toISOString();

    const commitUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents:commit`;
    const commitPayload = {
      writes: [
        {
          update: {
            name: `projects/${this.projectId}/databases/(default)/documents/users/${uid}`,
            fields: toFirestoreFields({
              ...userDoc,
              tokens: newTokens,
              tokensUpdatedAt: now
            })
          }
        },
        {
          update: {
            name: `projects/${this.projectId}/databases/(default)/documents/ad_rewards/${transactionId}`,
            fields: toFirestoreFields({
              transactionId,
              uid,
              sessionId,
              rewardAmount,
              timestamp: now
            })
          }
        },
        {
          update: {
            name: `projects/${this.projectId}/databases/(default)/documents/token_transactions/tx_ad_${transactionId}`,
            fields: toFirestoreFields({
              transactionId: `tx_ad_${transactionId}`,
              uid,
              type: 'AD_REWARD',
              amount: rewardAmount,
              transactionIdRef: transactionId,
              timestamp: now
            })
          }
        }
      ]
    };

    const res = await fetch(commitUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(commitPayload)
    });

    if (!res.ok) {
      throw new Error('Error al procesar la transacción de recompensa de anuncio');
    }

    return { rewardAmount, newBalance: newTokens };
  }
}
