import { Env } from './types';
import { verifyFirebaseIdToken } from './auth';
import { FirestoreClient } from './firestore';
import { verifyAdMobSsvSignature } from './admob';
import { TOKEN_CONFIG } from './config';

function jsonResponse(data: any, status = 200, extraHeaders: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
      ...extraHeaders
    }
  });
}

function handleCorsOptions(): Response {
  return new Response(null, {
    status: 204,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
      'Access-Control-Max-Age': '86400'
    }
  });
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    if (request.method === 'OPTIONS') {
      return handleCorsOptions();
    }

    const url = new URL(request.url);
    const pathname = url.pathname;
    const firestore = new FirestoreClient(env);
    const projectId = env.FIREBASE_PROJECT_ID || 'x-medic';

    try {
      // 1. Endpoint: POST /api/tokens/initialize
      if (pathname === '/api/tokens/initialize' && request.method === 'POST') {
        const decodedToken = await verifyFirebaseIdToken(request.headers.get('Authorization'), projectId);
        const result = await firestore.initializeTokens(decodedToken.uid, {
          displayName: decodedToken.name,
          email: decodedToken.email
        });
        return jsonResponse({
          success: true,
          initialized: result.initialized,
          balance: result.balance,
          message: result.initialized ? 'Bono inicial de 120 tokens asignado con éxito' : 'Usuario ya inicializado previamente'
        });
      }

      // 2. Endpoint: GET /api/tokens/balance
      if (pathname === '/api/tokens/balance' && request.method === 'GET') {
        const decodedToken = await verifyFirebaseIdToken(request.headers.get('Authorization'), projectId);
        const balance = await firestore.getBalance(decodedToken.uid);
        return jsonResponse({
          uid: decodedToken.uid,
          balance,
          initialized: true
        });
      }

      // 3. Endpoint: POST /api/analysis/reserve
      if (pathname === '/api/analysis/reserve' && request.method === 'POST') {
        const decodedToken = await verifyFirebaseIdToken(request.headers.get('Authorization'), projectId);
        const body = await request.json() as { operationId?: string };

        const operationId = body.operationId;
        if (!operationId || typeof operationId !== 'string' || !/^[a-zA-Z0-9_-]{1,128}$/.test(operationId)) {
          return jsonResponse({ success: false, error: 'El parámetro operationId es obligatorio y debe tener formato alfanumérico válido (máx 128 caracteres)' }, 400);
        }

        try {
          const reserveResult = await firestore.reserveAnalysisTokens(decodedToken.uid, operationId);
          return jsonResponse({
            success: true,
            operationId,
            status: reserveResult.status,
            cost: reserveResult.cost,
            balance: reserveResult.balance
          });
        } catch (e: any) {
          if (e.message && e.message.includes('FOREIGN_OPERATION_ID')) {
            return jsonResponse({
              success: false,
              error: 'FOREIGN_OPERATION_ID',
              message: 'La operación especificada pertenece a otro usuario'
            }, 403);
          }
          if (e.message && e.message.includes('USER_NOT_INITIALIZED')) {
            return jsonResponse({
              success: false,
              error: 'USER_NOT_INITIALIZED',
              message: 'El usuario no posee tokens inicializados en el servidor'
            }, 400);
          }
          if (e.message && e.message.includes('INSUFFICIENT_TOKENS')) {
            const currentBalance = await firestore.getBalance(decodedToken.uid);
            return jsonResponse({
              success: false,
              error: 'INSUFFICIENT_TOKENS',
              message: 'No posees los 5 tokens requeridos para autorizar este análisis médico',
              cost: TOKEN_CONFIG.ANALYSIS_COST,
              balance: currentBalance
            }, 402);
          }
          throw e;
        }
      }

      // 4. Endpoint: POST /api/analysis/complete
      if (pathname === '/api/analysis/complete' && request.method === 'POST') {
        const decodedToken = await verifyFirebaseIdToken(request.headers.get('Authorization'), projectId);
        const body = await request.json() as { operationId?: string; status?: 'COMPLETED' | 'FAILED' };

        if (!body.operationId || typeof body.operationId !== 'string' || !body.status || !['COMPLETED', 'FAILED'].includes(body.status)) {
          return jsonResponse({ success: false, error: 'Parámetros operationId y status (COMPLETED|FAILED) son obligatorios' }, 400);
        }

        const res = await firestore.completeAnalysisOperation(decodedToken.uid, body.operationId, body.status);
        return jsonResponse({
          success: true,
          operationId: body.operationId,
          status: body.status,
          balance: res.newBalance
        });
      }

      // 5. Endpoint: GET /api/analysis/:operationId
      if (pathname.startsWith('/api/analysis/') && request.method === 'GET') {
        const decodedToken = await verifyFirebaseIdToken(request.headers.get('Authorization'), projectId);
        const operationId = pathname.substring('/api/analysis/'.length);

        if (!operationId || !/^[a-zA-Z0-9_-]{1,128}$/.test(operationId)) {
          return jsonResponse({ error: 'operationId faltante o con formato inválido' }, 400);
        }

        const opDoc = await firestore.getDocument(`analysis_operations/${operationId}`);
        if (!opDoc) {
          return jsonResponse({ error: 'Operación de análisis no encontrada' }, 404);
        }

        if (opDoc.uid !== decodedToken.uid) {
          return jsonResponse({ error: 'No autorizado para consultar operaciones de otro usuario' }, 403);
        }

        return jsonResponse(opDoc);
      }

      // 6. Endpoint: POST /api/ads/session
      if (pathname === '/api/ads/session' && request.method === 'POST') {
        const decodedToken = await verifyFirebaseIdToken(request.headers.get('Authorization'), projectId);
        const sessionId = await firestore.createAdSession(decodedToken.uid);
        return jsonResponse({
          sessionId,
          customData: sessionId,
          createdAt: new Date().toISOString()
        });
      }

      // 7. Endpoint: GET /api/ads/ssv (Callback de AdMob SSV)
      if (pathname === '/api/ads/ssv' && request.method === 'GET') {
        const queryParams: Record<string, string> = {};
        url.searchParams.forEach((val, key) => {
          queryParams[key] = val;
        });

        const rawQueryString = url.search.substring(1);
        const isSignatureValid = await verifyAdMobSsvSignature(queryParams, rawQueryString, env);

        if (!isSignatureValid) {
          return jsonResponse({ error: 'Firma ECDSA de AdMob SSV inválida' }, 400);
        }

        const sessionId = queryParams.custom_data;
        const transactionId = queryParams.transaction_id;
        const rewardAmount = TOKEN_CONFIG.REWARDED_AD_TOKENS;

        if (!sessionId || !transactionId) {
          return jsonResponse({ error: 'Parámetros custom_data o transaction_id faltantes en callback SSV' }, 400);
        }

        const adSession = await firestore.getDocument(`ad_sessions/${sessionId}`);
        if (!adSession) {
          return jsonResponse({ error: 'Sesión de anuncio no encontrada o no autorizada' }, 404);
        }

        const rewardResult = await firestore.processAdReward(adSession.uid, sessionId, transactionId, rewardAmount);

        return new Response('SSV_VERIFIED', {
          status: 200,
          headers: { 'Content-Type': 'text/plain' }
        });
      }

      return jsonResponse({ error: 'Ruta o método no encontrado' }, 404);

    } catch (e: any) {
      return jsonResponse({
        error: 'ERROR_INTERNO_WORKER',
        message: e.message || 'Ocurrió un error no controlado en el backend de Cloudflare'
      }, 500);
    }
  }
};
