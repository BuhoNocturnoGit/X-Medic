import { TOKEN_CONFIG } from '../src/config';
import { verifyFirebaseIdToken } from '../src/auth';
import { verifyAdMobSsvSignature } from '../src/admob';
import { Env } from '../src/types';

describe('XMedic Token Backend Business Rules & Auth Tests', () => {

  test('Initial token grant constant equals 120', () => {
    expect(TOKEN_CONFIG.INITIAL_TOKENS).toBe(120);
  });

  test('Analysis cost constant equals 5 tokens', () => {
    expect(TOKEN_CONFIG.ANALYSIS_COST).toBe(5);
  });

  test('Rewarded ad token constant equals 2 tokens', () => {
    expect(TOKEN_CONFIG.REWARDED_AD_TOKENS).toBe(2);
  });

  test('User with 120 tokens can afford 5-token analysis', () => {
    const initial = TOKEN_CONFIG.INITIAL_TOKENS;
    const cost = TOKEN_CONFIG.ANALYSIS_COST;
    expect(initial - cost).toBe(115);
    expect(initial >= cost).toBe(true);
  });

  test('User with 4 tokens cannot afford 5-token analysis', () => {
    const userTokens = 4;
    expect(userTokens >= TOKEN_CONFIG.ANALYSIS_COST).toBe(false);
  });

  test('Rewarded ad increases balance by 2', () => {
    const current = 10;
    const reward = TOKEN_CONFIG.REWARDED_AD_TOKENS;
    expect(current + reward).toBe(12);
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
      ADMOB_REWARDED_AD_UNIT_ID: 'ca-app-pub-123456789/real_unit'
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
