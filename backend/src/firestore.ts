import { Env, UserTokenProfile } from './types';
import { TOKEN_CONFIG } from './config';
import { base64UrlToUint8Array } from './auth';

let cachedAccessToken: { token: string; expiry: number } | null = null;

/**
 * Genera y renueva tokens de acceso OAuth 2.0 para la cuenta de servicio de Google en Cloudflare Workers
 */
async function getOAuth2AccessToken(env: Env): Promise<string | null> {
  const email = env.FIREBASE_SERVICE_ACCOUNT_EMAIL;
  const privateKeyPem = env.FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY;

  if (!email || !privateKeyPem) {
    return null; // Fallback para desarrollo local o entornos sin credenciales
  }

  const now = Math.floor(Date.now() / 1000);
  if (cachedAccessToken && now < cachedAccessToken.expiry - 60) {
    return cachedAccessToken.token;
  }

  try {
    const header = { alg: 'RS256', typ: 'JWT' };
    const payload = {
      iss: email,
      scope: 'https://www.googleapis.com/auth/datastore',
      aud: 'https://oauth2.googleapis.com/token',
      exp: now + 3600,
      iat: now
    };

    const headerB64 = btoa(JSON.stringify(header)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    const payloadB64 = btoa(JSON.stringify(payload)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    const dataToSign = `${headerB64}.${payloadB64}`;

    const pemContents = privateKeyPem
      .replace(/-----BEGIN PRIVATE KEY-----/g, '')
      .replace(/-----END PRIVATE KEY-----/g, '')
      .replace(/\s+/g, '');
    const keyDer = base64UrlToUint8Array(pemContents);

    const cryptoKey = await crypto.subtle.importKey(
      'pkcs8',
      keyDer.buffer as ArrayBuffer,
      { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
      false,
      ['sign']
    );

    const signature = await crypto.subtle.sign(
      'RSASSA-PKCS1-v1_5',
      cryptoKey,
      new TextEncoder().encode(dataToSign)
    );

    const signatureB64 = btoa(String.fromCharCode(...new Uint8Array(signature)))
      .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');

    const assertionJwt = `${dataToSign}.${signatureB64}`;

    const res = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion: assertionJwt
      })
    });

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`OAuth2 Token error (${res.status}): ${errText}`);
    }

    const tokenData = await res.json() as { access_token: string; expires_in: number };
    cachedAccessToken = {
      token: tokenData.access_token,
      expiry: now + (tokenData.expires_in || 3600)
    };

    return cachedAccessToken.token;
  } catch (e: any) {
    console.error('Error generando OAuth2 access token de cuenta de servicio:', e);
    return null;
  }
}

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
 * Cliente REST de Cloud Firestore con transacciones atómicas de lectura-escritura y reintentos ante conflictos
 */
export class FirestoreClient {
  private projectId: string;
  private baseUrl: string;
  private env: Env;

  constructor(env: Env) {
    this.env = env;
    this.projectId = env.FIREBASE_PROJECT_ID || 'x-medic';
    this.baseUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents`;
  }

  private async getAuthHeaders(): Promise<Record<string, string>> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    const accessToken = await getOAuth2AccessToken(this.env);
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`;
    }
    return headers;
  }

  /**
   * Ejecuta un bloque transaccional con reintentos exponenciales ante conflictos de concurrencia (ABORTED/409)
   */
  private async executeWithTransactionRetry<T>(block: () => Promise<T>, maxRetries = 3): Promise<T> {
    let attempt = 0;
    while (attempt < maxRetries) {
      try {
        return await block();
      } catch (e: any) {
        attempt++;
        const isConflict = e.message && (e.message.includes('409') || e.message.includes('ABORTED') || e.message.includes('transaction'));
        if (isConflict && attempt < maxRetries) {
          const backoffMs = Math.floor(Math.random() * 100) + 50 * attempt;
          await new Promise(res => setTimeout(res, backoffMs));
          continue;
        }
        throw e;
      }
    }
    throw new Error('Transacción de Firestore abortada tras superar el límite de reintentos por concurrencia');
  }

  /**
   * Inicia una transacción atómica de lectura-escritura en Firestore
   */
  async beginTransaction(): Promise<string> {
    const headers = await this.getAuthHeaders();
    const url = `${this.baseUrl}:beginTransaction`;
    const res = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify({ options: { readWrite: {} } })
    });
    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Firestore beginTransaction error (${res.status}): ${err}`);
    }
    const data = await res.json() as { transaction: string };
    return data.transaction;
  }

  /**
   * Obtiene un documento de Firestore dentro o fuera de una transacción
   */
  async getDocument(path: string, transactionId?: string): Promise<Record<string, any> | null> {
    const headers = await this.getAuthHeaders();
    const query = transactionId ? `?transaction=${encodeURIComponent(transactionId)}` : '';
    const url = `${this.baseUrl}/${path}${query}`;
    const res = await fetch(url, { headers });
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
   * Inicializa de forma atómica e idempotente el bono de 120 tokens para un UID.
   * IMPORTANTE: No inventa ni sobrescribe rol ni onboardingCompleted si el perfil no existe.
   */
  async initializeTokens(uid: string, initialProfile?: Partial<UserTokenProfile>): Promise<{ initialized: boolean; balance: number }> {
    return this.executeWithTransactionRetry(async () => {
      const txId = await this.beginTransaction();
      const userDocPath = `users/${uid}`;
      const existing = await this.getDocument(userDocPath, txId);

      if (existing && existing.tokensInitialized === true && typeof existing.tokens === 'number') {
        return { initialized: false, balance: existing.tokens };
      }

      const now = new Date().toISOString();
      const initialTokens = TOKEN_CONFIG.INITIAL_TOKENS;

      // Preservar exactamente los campos de perfil existentes si estaban presentes; NO inventar rol ni onboardingCompleted.
      const updatedProfile: Record<string, any> = {
        uid,
        tokens: initialTokens,
        tokensInitialized: true,
        tokensInitializedAt: existing?.tokensInitializedAt || now,
        tokensUpdatedAt: now
      };

      if (existing) {
        if (existing.displayName) updatedProfile.displayName = existing.displayName;
        if (existing.email) updatedProfile.email = existing.email;
        if (existing.role) updatedProfile.role = existing.role;
        if (existing.onboardingCompleted !== undefined) updatedProfile.onboardingCompleted = existing.onboardingCompleted;
      } else if (initialProfile) {
        if (initialProfile.displayName) updatedProfile.displayName = initialProfile.displayName;
        if (initialProfile.email) updatedProfile.email = initialProfile.email;
      }

      const transactionRecordId = `tx_init_${uid}_${Date.now()}`;
      const commitPayload = {
        transaction: txId,
        writes: [
          {
            update: {
              name: `projects/${this.projectId}/databases/(default)/documents/users/${uid}`,
              fields: toFirestoreFields(updatedProfile)
            }
          },
          {
            update: {
              name: `projects/${this.projectId}/databases/(default)/documents/token_transactions/${transactionRecordId}`,
              fields: toFirestoreFields({
                transactionId: transactionRecordId,
                uid,
                type: 'INITIAL_BONUS',
                amount: initialTokens,
                timestamp: now
              })
            }
          }
        ]
      };

      const headers = await this.getAuthHeaders();
      const commitUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents:commit`;
      const res = await fetch(commitUrl, {
        method: 'POST',
        headers,
        body: JSON.stringify(commitPayload)
      });

      if (!res.ok) {
        const err = await res.text();
        throw new Error(`Error en transacción de inicialización en Firestore: ${err}`);
      }

      return { initialized: true, balance: initialTokens };
    });
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
    return typeof userDoc.tokens === 'number' ? userDoc.tokens : 0;
  }

  /**
   * Reserva y descuenta transaccionalmente 5 tokens para un análisis médico (atómico con validación de pertenencia e idempotencia)
   */
  async reserveAnalysisTokens(uid: string, operationId: string): Promise<{ status: 'RESERVED' | 'ALREADY_RESERVED'; cost: number; balance: number }> {
    return this.executeWithTransactionRetry(async () => {
      const txId = await this.beginTransaction();
      const cost = TOKEN_CONFIG.ANALYSIS_COST;
      const opPath = `analysis_operations/${operationId}`;
      const existingOp = await this.getDocument(opPath, txId);

      if (existingOp) {
        if (existingOp.uid !== uid) {
          throw new Error('FOREIGN_OPERATION_ID: La operación especificada pertenece a otro usuario');
        }
        if (existingOp.status === 'RESERVED') {
          const currentBalance = await this.getBalance(uid);
          return { status: 'ALREADY_RESERVED', cost, balance: currentBalance };
        }
      }

      const userDocPath = `users/${uid}`;
      let userDoc = await this.getDocument(userDocPath, txId);

      if (!userDoc || userDoc.tokensInitialized !== true) {
        await this.initializeTokens(uid);
        userDoc = await this.getDocument(userDocPath, txId);
      }

      const currentTokens = userDoc && typeof userDoc.tokens === 'number' ? userDoc.tokens : 0;

      if (currentTokens < cost) {
        throw new Error(`INSUFFICIENT_TOKENS: Saldo actual (${currentTokens}) inferior al costo requerido (${cost})`);
      }

      const newTokens = currentTokens - cost;
      const now = new Date().toISOString();
      const txRecordId = `tx_reserve_${operationId}`;

      const commitPayload = {
        transaction: txId,
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
              name: `projects/${this.projectId}/databases/(default)/documents/token_transactions/${txRecordId}`,
              fields: toFirestoreFields({
                transactionId: txRecordId,
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

      const headers = await this.getAuthHeaders();
      const commitUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents:commit`;
      const res = await fetch(commitUrl, {
        method: 'POST',
        headers,
        body: JSON.stringify(commitPayload)
      });

      if (!res.ok) {
        const err = await res.text();
        throw new Error(`Error reservando tokens de análisis en Firestore: ${err}`);
      }

      return { status: 'RESERVED', cost, balance: newTokens };
    });
  }

  /**
   * Registra y valida una sesión de anuncio recompensado AdMob
   */
  async createAdSession(uid: string): Promise<string> {
    const sessionId = `ad_sess_${crypto.randomUUID()}`;
    const now = new Date().toISOString();

    const headers = await this.getAuthHeaders();
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
      headers,
      body: JSON.stringify(commitPayload)
    });

    if (!res.ok) {
      throw new Error('Error al registrar sesión de anuncio AdMob');
    }

    return sessionId;
  }

  /**
   * Procesa de forma atómica e idempotente la recompensa de +2 tokens desde AdMob SSV dentro de una transacción de lectura-escritura
   */
  async processAdReward(uid: string, sessionId: string, transactionId: string, rewardAmount: number = TOKEN_CONFIG.REWARDED_AD_TOKENS): Promise<{ rewardAmount: number; newBalance: number }> {
    return this.executeWithTransactionRetry(async () => {
      const txId = await this.beginTransaction();
      const rewardPath = `ad_rewards/${transactionId}`;
      const existingReward = await this.getDocument(rewardPath, txId);

      if (existingReward) {
        const balance = await this.getBalance(uid);
        return { rewardAmount, newBalance: balance };
      }

      const adSessionPath = `ad_sessions/${sessionId}`;
      const adSession = await this.getDocument(adSessionPath, txId);

      if (!adSession || adSession.uid !== uid) {
        throw new Error('INVALID_AD_SESSION: La sesión de anuncio no pertenece al usuario autenticado');
      }

      if (adSession.status === 'REWARDED') {
        const balance = await this.getBalance(uid);
        return { rewardAmount, newBalance: balance };
      }

      const userDoc = await this.getDocument(`users/${uid}`, txId);
      const currentTokens = userDoc && typeof userDoc.tokens === 'number' ? userDoc.tokens : TOKEN_CONFIG.INITIAL_TOKENS;
      const newTokens = currentTokens + rewardAmount;
      const now = new Date().toISOString();

      const commitPayload = {
        transaction: txId,
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
              name: `projects/${this.projectId}/databases/(default)/documents/ad_sessions/${sessionId}`,
              fields: toFirestoreFields({
                ...adSession,
                status: 'REWARDED',
                rewardedAt: now
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

      const headers = await this.getAuthHeaders();
      const commitUrl = `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents:commit`;
      const res = await fetch(commitUrl, {
        method: 'POST',
        headers,
        body: JSON.stringify(commitPayload)
      });

      if (!res.ok) {
        throw new Error('Error al procesar la transacción de recompensa de anuncio');
      }

      return { rewardAmount, newBalance: newTokens };
    });
  }
}
