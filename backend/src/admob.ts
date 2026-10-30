import { AdMobSsvQueryParams, Env } from './types';
import { base64UrlToUint8Array } from './auth';

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
 * Extrae la subcadena sin alterar la representación original de bytes y orden enviada por Google AdMob
 */
function extractOriginalSignedQueryString(rawQueryString: string): string {
  // Omitir signature y key_id preservando exactamente el orden y codificación original enviada por Google
  return rawQueryString
    .replace(/&signature=[^&]*/, '')
    .replace(/signature=[^&]*&?/, '')
    .replace(/&key_id=[^&]*/, '')
    .replace(/key_id=[^&]*&?/, '')
    .replace(/&$/, '')
    .replace(/^&/, '');
}

/**
 * Verifica la firma ECDSA del callback Server-Side Verification (SSV) de Google AdMob
 */
export async function verifyAdMobSsvSignature(
  params: AdMobSsvQueryParams,
  rawQueryString: string,
  env: Env
): Promise<boolean> {
  const keyIdStr = params.key_id;
  const signatureStr = params.signature;

  if (!keyIdStr || !signatureStr) {
    return false;
  }

  // Validar ad_unit configurado
  if (params.ad_unit && env.ADMOB_REWARDED_AD_UNIT_ID && params.ad_unit !== env.ADMOB_REWARDED_AD_UNIT_ID) {
    // Si no coincide con el bloque de anuncios configurado, rechazar
    if (!env.ADMOB_REWARDED_AD_UNIT_ID.includes('3940256099942544')) {
      return false;
    }
  }

  const keyId = parseInt(keyIdStr, 10);
  const keysMap = await getAdMobPublicKeys();
  const pem = keysMap[keyId];

  if (!pem) {
    return false;
  }

  const signedQueryString = extractOriginalSignedQueryString(rawQueryString);
  const messageBytes = new TextEncoder().encode(signedQueryString);
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
