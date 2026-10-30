import { DecodedToken } from './types';

interface JwkKey {
  kty: string;
  alg: string;
  use: string;
  kid: string;
  n: string;
  e: string;
}

interface JwksResponse {
  keys: JwkKey[];
}

let cachedJwksKeys: Record<string, JwkKey> | null = null;
let jwksCacheExpiry = 0;

/**
 * Obtiene y almacena en caché las claves públicas JWK de Firebase Auth desde Google.
 */
async function getFirebaseJwksKeys(): Promise<Record<string, JwkKey>> {
  const now = Date.now();
  if (cachedJwksKeys && now < jwksCacheExpiry) {
    return cachedJwksKeys;
  }

  const res = await fetch('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com');
  if (!res.ok) {
    throw new Error(`Error obteniendo claves JWK de Google: ${res.statusText}`);
  }

  const cacheControl = res.headers.get('cache-control');
  let maxAgeSeconds = 3600;
  if (cacheControl) {
    const match = cacheControl.match(/max-age=(\d+)/);
    if (match) {
      maxAgeSeconds = parseInt(match[1], 10);
    }
  }

  const data = await res.json() as JwksResponse;
  const keysMap: Record<string, JwkKey> = {};

  if (Array.isArray(data.keys)) {
    for (const key of data.keys) {
      if (key.kid) {
        keysMap[key.kid] = key;
      }
    }
  }

  cachedJwksKeys = keysMap;
  jwksCacheExpiry = now + maxAgeSeconds * 1000;
  return cachedJwksKeys;
}

/**
 * Decodifica Base64URL a Uint8Array
 */
export function base64UrlToUint8Array(base64Url: string): Uint8Array {
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const padLen = (4 - (base64.length % 4)) % 4;
  const padded = base64 + '='.repeat(padLen);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

/**
 * Importa una clave JWK RSA usando Web Crypto API de Cloudflare Workers
 */
async function importJwkToCryptoKey(jwk: JwkKey): Promise<CryptoKey> {
  return await crypto.subtle.importKey(
    'jwk',
    jwk,
    {
      name: 'RSASSA-PKCS1-v1_5',
      hash: 'SHA-256'
    },
    false,
    ['verify']
  );
}

/**
 * Verifica criptográficamente el ID Token de Firebase Authentication.
 */
export async function verifyFirebaseIdToken(
  authHeader: string | null,
  projectId: string
): Promise<DecodedToken> {
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    throw new Error('Encabezado Authorization faltante o con formato inválido (Esperado: Bearer <token>)');
  }

  const idToken = authHeader.substring(7).trim();
  const parts = idToken.split('.');
  if (parts.length !== 3) {
    throw new Error('El ID Token debe constar de 3 partes separadas por puntos');
  }

  const [headerB64, payloadB64, signatureB64] = parts;

  const header = JSON.parse(new TextDecoder().decode(base64UrlToUint8Array(headerB64)));
  const payload = JSON.parse(new TextDecoder().decode(base64UrlToUint8Array(payloadB64)));

  if (header.alg !== 'RS256') {
    throw new Error(`Algoritmo no soportado: ${header.alg}. Se requiere RS256.`);
  }

  const kid = header.kid;
  if (!kid) {
    throw new Error('El encabezado del JWT no contiene la propiedad kid');
  }

  // 1. Verificar afirmaciones temporales obligatorias (Claims)
  const nowInSec = Math.floor(Date.now() / 1000);
  const clockSkewTolerance = 300; // 5 minutos de tolerancia para diferencias de reloj

  if (payload.aud !== projectId) {
    throw new Error(`Audience inválido: ${payload.aud}. Se esperaba: ${projectId}`);
  }

  const expectedIss = `https://securetoken.google.com/${projectId}`;
  if (payload.iss !== expectedIss) {
    throw new Error(`Issuer inválido: ${payload.iss}. Se esperaba: ${expectedIss}`);
  }

  if (payload.exp && payload.exp <= nowInSec) {
    throw new Error('El ID Token de Firebase ha expirado');
  }

  if (payload.iat && payload.iat > nowInSec + clockSkewTolerance) {
    throw new Error('El claim iat del ID Token se encuentra en el futuro');
  }

  if (payload.auth_time && payload.auth_time > nowInSec + clockSkewTolerance) {
    throw new Error('El claim auth_time del ID Token se encuentra en el futuro');
  }

  if (!payload.sub || typeof payload.sub !== 'string') {
    throw new Error('El Subject (sub/UID) del ID Token es inválido o está vacío');
  }

  // 2. Verificar firma criptográfica RS256 usando la clave JWK correspondiente al kid
  const jwksMap = await getFirebaseJwksKeys();
  const jwk = jwksMap[kid];
  if (!jwk) {
    throw new Error(`No se encontró clave pública JWK para el kid: ${kid}`);
  }

  const signatureBytes = base64UrlToUint8Array(signatureB64);
  const dataBytes = new TextEncoder().encode(`${headerB64}.${payloadB64}`);

  const cryptoKey = await importJwkToCryptoKey(jwk);
  const isValid = await crypto.subtle.verify(
    'RSASSA-PKCS1-v1_5',
    cryptoKey,
    signatureBytes.buffer as ArrayBuffer,
    dataBytes.buffer as ArrayBuffer
  );

  if (!isValid) {
    throw new Error('Firma criptográfica del ID Token inválida');
  }

  return {
    uid: payload.sub,
    email: payload.email,
    name: payload.name,
    iss: payload.iss,
    aud: payload.aud,
    exp: payload.exp,
    iat: payload.iat
  };
}
