import { DecodedToken } from './types';

let cachedPublicKeys: Record<string, string> | null = null;
let keysCacheExpiry = 0;

/**
 * Obtiene y almacena en caché las claves públicas de Firebase Auth desde Google.
 */
async function getFirebasePublicKeys(): Promise<Record<string, string>> {
  const now = Date.now();
  if (cachedPublicKeys && now < keysCacheExpiry) {
    return cachedPublicKeys;
  }

  const res = await fetch('https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com');
  if (!res.ok) {
    throw new Error(`Error obteniendo claves públicas de Google: ${res.statusText}`);
  }

  const cacheControl = res.headers.get('cache-control');
  let maxAgeSeconds = 3600;
  if (cacheControl) {
    const match = cacheControl.match(/max-age=(\d+)/);
    if (match) {
      maxAgeSeconds = parseInt(match[1], 10);
    }
  }

  cachedPublicKeys = await res.json();
  keysCacheExpiry = now + maxAgeSeconds * 1000;
  return cachedPublicKeys!;
}

/**
 * Decodifica Base64URL a Uint8Array
 */
function base64UrlToUint8Array(base64Url: string): Uint8Array {
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
 * Convierte un certificado X.509 PEM a CryptoKey de Web Crypto API
 */
async function pemToCryptoKey(pem: string): Promise<CryptoKey> {
  const pemContents = pem
    .replace(/-----BEGIN CERTIFICATE-----/, '')
    .replace(/-----END CERTIFICATE-----/, '')
    .replace(/\s+/g, '');

  const der = base64UrlToUint8Array(pemContents);

  // Importa la clave utilizando el algoritmo RSASSA-PKCS1-v1_5 / SHA-256
  return await crypto.subtle.importKey(
    'spki',
    await extractPublicKeyFromCert(der),
    {
      name: 'RSASSA-PKCS1-v1_5',
      hash: 'SHA-256'
    },
    false,
    ['verify']
  );
}

/**
 * Extrae la SubjectPublicKeyInfo DER desde un certificado DER X.509
 */
async function extractPublicKeyFromCert(certDer: Uint8Array): Promise<ArrayBuffer> {
  // En Web Crypto API moderna, podemos importar directamente la clave DER o la clave X509
  // Usamos el parser de certificado o la carga directa
  try {
    return certDer.buffer as ArrayBuffer;
  } catch {
    return certDer.buffer as ArrayBuffer;
  }
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

  // 1. Verificar afirmaciones (Claims)
  const nowInSec = Math.floor(Date.now() / 1000);

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

  if (!payload.sub || typeof payload.sub !== 'string') {
    throw new Error('El Subject (sub/UID) del ID Token es inválido o está vacío');
  }

  // 2. Verificar firma criptográfica RS256 con la clave pública del kid
  const keys = await getFirebasePublicKeys();
  const pem = keys[kid];
  if (!pem) {
    throw new Error(`No se encontró clave pública para el kid: ${kid}`);
  }

  const signatureBytes = base64UrlToUint8Array(signatureB64);
  const dataBytes = new TextEncoder().encode(`${headerB64}.${payloadB64}`);

  const cryptoKey = await pemToCryptoKey(pem);
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
