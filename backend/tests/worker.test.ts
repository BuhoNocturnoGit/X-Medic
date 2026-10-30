import { TOKEN_CONFIG } from '../src/config';
import { verifyFirebaseIdToken } from '../src/auth';
import { verifyAdMobSsvSignature, derToP1363Signature } from '../src/admob';
import { Env } from '../src/types';

describe('XMedic Token Backend Business Rules & Security Tests (v1.0.9)', () => {

  test('Initial token grant constant equals 120', () => {
    expect(TOKEN_CONFIG.INITIAL_TOKENS).toBe(120);
  });

  test('Analysis cost constant equals 5 tokens', () => {
    expect(TOKEN_CONFIG.ANALYSIS_COST).toBe(5);
  });

  test('Rewarded ad token constant equals 2 tokens', () => {
    expect(TOKEN_CONFIG.REWARDED_AD_TOKENS).toBe(2);
  });

  test('User with 120 tokens can afford 5-token analysis leaving 115 tokens', () => {
    const initial = TOKEN_CONFIG.INITIAL_TOKENS;
    const cost = TOKEN_CONFIG.ANALYSIS_COST;
    expect(initial - cost).toBe(115);
    expect(initial >= cost).toBe(true);
  });

  test('Three 5-token analysis operations leave 105 tokens', () => {
    const initial = TOKEN_CONFIG.INITIAL_TOKENS; // 120
    const threeAnalysesCost = TOKEN_CONFIG.ANALYSIS_COST * 3; // 15
    const remaining = initial - threeAnalysesCost;
    expect(remaining).toBe(105);
  });

  test('User with 4 tokens cannot afford 5-token analysis', () => {
    const userTokens = 4;
    expect(userTokens >= TOKEN_CONFIG.ANALYSIS_COST).toBe(false);
  });

  test('Rewarded ad increases balance by 2 tokens', () => {
    const current = 10;
    const reward = TOKEN_CONFIG.REWARDED_AD_TOKENS;
    expect(current + reward).toBe(12);
  });

  test('derToP1363Signature converts ASN.1 DER signature bytes to 64-byte P1363 array', () => {
    // DER sequence [0x30, len, 0x02, len_r, r, 0x02, len_s, s]
    const dummyDer = new Uint8Array([
      0x30, 0x44,
      0x02, 0x20,
      0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10,
      0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20,
      0x02, 0x20,
      0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30,
      0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, 0x40
    ]);

    const p1363 = derToP1363Signature(dummyDer);
    expect(p1363.length).toBe(64);
  });

  test('verifyFirebaseIdToken rejects missing Authorization header', async () => {
    await expect(verifyFirebaseIdToken(null, 'x-medic')).rejects.toThrow(
      'Encabezado Authorization faltante o con formato inválido'
    );
  });

  test('verifyFirebaseIdToken rejects invalid Bearer token string', async () => {
    await expect(verifyFirebaseIdToken('Bearer invalid.jwt.token', 'x-medic')).rejects.toThrow();
  });

  test('verifyAdMobSsvSignature rejects request missing signature', async () => {
    const mockEnv: Env = {
      FIREBASE_PROJECT_ID: 'x-medic',
      ADMOB_REWARDED_AD_UNIT_ID: 'ca-app-pub-3940256099942544/5224354917'
    };
    const params = {
      ad_unit: 'ca-app-pub-3940256099942544/5224354917'
    };
    const isValid = await verifyAdMobSsvSignature(params, 'ad_unit=ca-app-pub-3940256099942544/5224354917', mockEnv);
    expect(isValid).toBe(false);
  });

  test('verifyAdMobSsvSignature rejects ad_unit mismatch', async () => {
    const mockEnv: Env = {
      FIREBASE_PROJECT_ID: 'x-medic',
      ADMOB_REWARDED_AD_UNIT_ID: 'ca-app-pub-3940256099942544/5224354917'
    };
    const params = {
      key_id: '1234',
      signature: 'invalid_sig',
      ad_unit: 'ca-app-pub-999999999/fake_unit'
    };
    const isValid = await verifyAdMobSsvSignature(params, 'ad_unit=ca-app-pub-999999999/fake_unit&key_id=1234&signature=invalid_sig', mockEnv);
    expect(isValid).toBe(false);
  });
});
