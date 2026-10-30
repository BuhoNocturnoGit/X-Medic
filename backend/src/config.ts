/**
 * Constantes de reglas de negocio para el sistema de tokens de XMedic
 */
export const TOKEN_CONFIG = {
  INITIAL_TOKENS: 120,       // Bono inicial único por UID al registrarse/conectar
  ANALYSIS_COST: 5,          // Costo en tokens por cada análisis autorizado
  REWARDED_AD_TOKENS: 2,    // Recompensa acreditada por anuncio AdMob SSV verificado
  MIN_BALANCE: 0             // El saldo jamás puede ser negativo
} as const;
