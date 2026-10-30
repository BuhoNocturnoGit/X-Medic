import { AdMobSsvQueryParams } from './types';

let cachedAdMobKeys: Record<number, string> | null = null;
let admobKeysCacheExpiry = 0;

/**
 * Obtiene y almacena en caché las claves públicas ECDSA oficiales de Google AdMob SSV
 */
async function getAdMobPublicKeys(): Promise<Record<number, string>> {
  const now = Date.now();
  if (cachedAdMobKeys && now < admobKeysCacheExpiry) {
    return cachedAdMobKeys;
  }

  const res = await fetch('https://admob.googleapis.com/v1/pubkeys');
  if (!res.ok) {
    throw new Error(`Error obteniendo claves públicas AdMob: ${res.statusText}`);
  }

  const data = await res.json() as { keys: Array<{ keyId: number; pem: string }> };
  const keysMap: Record<number, string> = {};

  if (Array.isArray(data.keys)) {
    for (const keyObj of data.keys) {
      if (keyObj.keyId && keyObj.pem) {
        keysMap[keyObj.keyId] = keyObj.pem;
      }
    }
  }

  cachedAdMobKeys = keysMap;
  admobKeysCacheExpiry = now + 3600 * 1000 * 24; // Caché por 24 horas
  return cachedAdMobKeys;
}

/**
 * Decodifica Base64URL
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
 * Verifica la firma ECDSA del callback Server-Side Verification (SSV) de Google AdMob
 */
export async function verifyAdMobSsvSignature(params: AdMobSsvQueryParams, rawQueryString: string): Promise<boolean> {
  const keyIdStr = params.key_id;
  const signatureStr = params.signature;

  if (!keyIdStr || !signatureStr) {
    return false;
  }

  const keyId = parseInt(keyIdStr, 10);
  const keysMap = await getAdMobPublicKeys();
  const pem = keysMap[keyId];

  if (!pem) {
    return false;
  }

  // Reconstruir la cadena firmada omitiendo signature y key_id según la especificación de Google AdMob SSV
  const urlParams = new URLSearchParams(rawQueryString);
  urlParams.delete('signature');
  urlParams.delete('key_id');

  // Ordenar alfabéticamente los parámetros restantes
  const sortedKeys = Array.from(urlParams.keys()).sort();
  const messageParts: string[] = [];
  for (const k of sortedKeys) {
    messageParts.push(`${k}=${urlParams.get(k)}`);
  }
  const messageToVerify = messageParts.join('&');
  const messageBytes = new TextEncoder().encode(messageToVerify);
  const signatureBytes = base64UrlToUint8Array(signatureStr);

  try {
    const pemContents = pem
      .replace(/-----BEGIN PUBLIC KEY-----/, '')
      .replace(/-----END PUBLIC KEY-----/, '')
      .replace(/\s+/g, '');
    const derKey = base64UrlToUint8Array(pemContents);

    const cryptoKey = await crypto.subtle.importKey(
      'spki',
      derKey.buffer as ArrayBuffer,
      {
        name: 'ECDSA',
        namedCurve: 'P-256'
      },
      false,
      ['verify']
    );

    return await crypto.subtle.verify(
      {
        name: 'ECDSA',
        hash: { name: 'SHA-256' }
      },
      cryptoKey,
      signatureBytes.buffer as ArrayBuffer,
      messageBytes.buffer as ArrayBuffer
    );
  } catch {
    return false;
  }
}
