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
 * Convierte una firma ECDSA en formato ASN.1 DER (enviada por AdMob SSV) a formato IEEE P1363 de 64 bytes
 * requerido por Web Crypto API (crypto.subtle.verify)
 */
export function derToP1363Signature(derBytes: Uint8Array): Uint8Array {
  if (derBytes.length === 64) {
    return derBytes; // Ya en formato P1363
  }
  if (derBytes[0] !== 0x30) {
    return derBytes;
  }

  let offset = 2;
  if (derBytes[1] & 0x80) {
    const lenBytes = derBytes[1] & 0x7f;
    offset += lenBytes;
  }

  // Leer entero r
  if (derBytes[offset] !== 0x02) {
    throw new Error('Firma DER inválida: tag r esperado');
  }
  const rLen = derBytes[offset + 1];
  const rOffset = offset + 2;
  offset = rOffset + rLen;

  // Leer entero s
  if (derBytes[offset] !== 0x02) {
    throw new Error('Firma DER inválida: tag s esperado');
  }
  const sLen = derBytes[offset + 1];
  const sOffset = offset + 2;

  let rBytes = derBytes.subarray(rOffset, rOffset + rLen);
  let sBytes = derBytes.subarray(sOffset, sOffset + sLen);

  // Recortar ceros de relleno si es necesario
  if (rBytes.length > 32 && rBytes[0] === 0x00) {
    rBytes = rBytes.subarray(rBytes.length - 32);
  }
  if (sBytes.length > 32 && sBytes[0] === 0x00) {
    sBytes = sBytes.subarray(sBytes.length - 32);
  }

  const p1363 = new Uint8Array(64);
  p1363.set(rBytes, 32 - rBytes.length);
  p1363.set(sBytes, 64 - sBytes.length);
  return p1363;
}

/**
 * Extrae la subcadena sin alterar la representación original de bytes y orden enviada por Google AdMob
 */
function extractOriginalSignedQueryString(rawQueryString: string): string {
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

  // Validar ad_unit configurado estrictamente
  if (params.ad_unit && env.ADMOB_REWARDED_AD_UNIT_ID && params.ad_unit !== env.ADMOB_REWARDED_AD_UNIT_ID) {
    return false;
  }

  const keyId = parseInt(keyIdStr, 10);
  const keysMap = await getAdMobPublicKeys();
  const pem = keysMap[keyId];

  if (!pem) {
    return false;
  }

  const signedQueryString = extractOriginalSignedQueryString(rawQueryString);
  const messageBytes = new TextEncoder().encode(signedQueryString);
  const rawSignatureBytes = base64UrlToUint8Array(signatureStr);
  const p1363SignatureBytes = derToP1363Signature(rawSignatureBytes);

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
      p1363SignatureBytes.buffer as ArrayBuffer,
      messageBytes.buffer as ArrayBuffer
    );
  } catch {
    return false;
  }
}
