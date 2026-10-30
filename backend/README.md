# XMedic Token Backend — Cloudflare Workers & Firebase Spark

Backend seguro sin costos recurrentes desarrollado en **TypeScript** sobre **Cloudflare Workers (Plan Gratuito)** e integrado con **Cloud Firestore (Plan Spark)** y **Google AdMob SSV**.

---

## 🏗️ Arquitectura y Seguridad

- **Autenticación en Servidor:** Cada solicitud HTTPS en `/api/...` valida criptográficamente el `Firebase ID Token` de Android mediante la API **Web Crypto** y las claves públicas x509 de Google (`RS256`).
- **Control de Saldo Centralizado:** El cliente Android **ya no puede modificar directamente** su campo `tokens` en Cloud Firestore. Todas las operaciones (inicialización, reservas y recompensas publicitarias) ocurren exclusivamente mediante transacciones atómicas desde el Worker.
- **Transacciones Atómicas:** Las inicializaciones (120 tokens), reservas de análisis (5 tokens) y recompensas de anuncios AdMob (2 tokens) utilizan operaciones `:commit` de la API REST de Firestore, garantizando idempotencia y resistencia a condiciones de carrera.
- **Cero Secretos en Android:** Ninguna clave privada o credencial de servicio reside en la APK de Android. Los secretos se gestionan con `wrangler secret put`.

---

## ⚡ Reglas de Negocio de Tokens

| Regla | Valor | Descripción |
|---|---|---|
| **Bono Inicial Único** | `120 tokens` | Asignado una sola vez por UID al registrarse/iniciar sesión. No se vuelve a conceder al reinstalar o reiniciar sesión. |
| **Costo por Análisis** | `5 tokens` | Descontado de forma atómica antes de autorizar un análisis médico. Si el saldo es < 5, la operación se rechaza. |
| **Anuncio Recompensado** | `2 tokens` | Acreditado mediante verificación ECDSA en el callback SSV de AdMob. |
| **Saldo Mínimo** | `0 tokens` | El saldo nunca puede ser negativo. |

---

## 🛠️ Endpoints de la API REST

### 1. Inicializar Saldo de Usuario
- **Ruta:** `POST /api/tokens/initialize`
- **Encabezado:** `Authorization: Bearer <idToken>`
- **Respuesta (200 OK):**
  ```json
  {
    "success": true,
    "initialized": true,
    "balance": 120,
    "message": "Bono inicial de 120 tokens asignado con éxito"
  }
  ```

### 2. Consultar Saldo Oficial
- **Ruta:** `GET /api/tokens/balance`
- **Encabezado:** `Authorization: Bearer <idToken>`
- **Respuesta (200 OK):**
  ```json
  {
    "uid": "firebase_uid_123",
    "balance": 120,
    "initialized": true
  }
  ```

### 3. Reservar Tokens para Análisis Médico
- **Ruta:** `POST /api/analysis/reserve`
- **Encabezado:** `Authorization: Bearer <idToken>`
- **Cuerpo JSON:** `{ "operationId": "op_1710000000_a1b2c3d4" }`
- **Respuesta (200 OK):**
  ```json
  {
    "success": true,
    "operationId": "op_1710000000_a1b2c3d4",
    "status": "RESERVED",
    "cost": 5,
    "balance": 115
  }
  ```
- **Respuesta (402 Payment Required):**
  ```json
  {
    "success": false,
    "error": "INSUFFICIENT_TOKENS",
    "message": "No posees los 5 tokens requeridos para autorizar este análisis médico",
    "cost": 5,
    "balance": 3
  }
  ```

### 4. Consultar Estado de Operación
- **Ruta:** `GET /api/analysis/:operationId`
- **Encabezado:** `Authorization: Bearer <idToken>`

### 5. Crear Sesión para Anuncio Recompensado
- **Ruta:** `POST /api/ads/session`
- **Encabezado:** `Authorization: Bearer <idToken>`
- **Respuesta (200 OK):**
  ```json
  {
    "sessionId": "ad_sess_f47ac10b-58cc-4372-a567-0e02b2c3d4e5",
    "customData": "ad_sess_f47ac10b-58cc-4372-a567-0e02b2c3d4e5",
    "createdAt": "2025-03-01T12:00:00.000Z"
  }
  ```

### 6. Callback SSV de AdMob
- **Ruta:** `GET /api/ads/ssv?...`
- **Filtro de Seguridad:** Valida la firma ECDSA de Google AdMob (`https://admob.googleapis.com/v1/pubkeys`), exige `custom_data` y verifica que el `transaction_id` no haya sido procesado previamente.

---

## 🚀 Despliegue y Ejecución Local

### Prerrequisitos
1. Node.js (v18+) e npm instalado.
2. CLI de Wrangler (`npm install -g wrangler`).

### Ejecución Local de Desarrollo
```bash
cd backend
npm install
npm run dev
```

### Configuración de Secretos en Cloudflare Workers
```bash
wrangler secret put FIREBASE_SERVICE_ACCOUNT_EMAIL
wrangler secret put FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY
```

### Despliegue a Producción (Plan Gratuito)
```bash
npm run deploy
```

---

## ⚠️ Limitaciones del Plan Gratuito (Cloudflare Workers Free & Firebase Spark)

1. **Límite Diario de Cloudflare Workers Free:**
   - Hasta 100,000 solicitudes HTTPS/día en el plan gratuito de Cloudflare Workers.
2. **Límite Diario de Firestore Spark:**
   - Hasta 50,000 lecturas/día, 20,000 escrituras/día y 20,000 eliminaciones/día sin costo.
3. **Limitación de Seguridad de Inferencia Local en Android:**
   - La inferencia del modelo TFLite ocurre localmente en el dispositivo del usuario. Si bien el servidor autoriza y cobra los 5 tokens antes del análisis, un atacante con acceso root o APK modificada podría ejecutar inferencias locales directamente. Para protección clínica absoluta en producción, la inferencia médica sensible puede migrarse a un endpoint de servidor cuando se escala a un plan de producción.
