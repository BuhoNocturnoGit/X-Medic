import { TOKEN_CONFIG } from '../src/config';

describe('XMedic Token Backend Business Rules', () => {

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
    val userTokens = 4;
    expect(userTokens >= TOKEN_CONFIG.ANALYSIS_COST).toBe(false);
  });

  test('Rewarded ad increases balance by 2', () => {
    const current = 10;
    const reward = TOKEN_CONFIG.REWARDED_AD_TOKENS;
    expect(current + reward).toBe(12);
  });
});
