# Authorizer Lambda — Pedidos360 backend-api

Lambda authorizer tipo `REQUEST` (payload 2.0, respuestas simples) para el
API Gateway `backend-api`. Valida firma, vigencia, `iss` y `aud` de los JWT
de **Azure Entra ID** (proveedor `cognito` comentado hasta crear el User Pool)
y responde `{"isAuthorized": true|false}`.

Basado en `j-camposar/validador-lambda` (configurado para este tenant).
Usa `NimbusJwtDecoder` de Spring Security: acepta llaves JWKS sin campo
`alg` (el mismo caso que el fix `1.3.0` de servicio-pedidos).

## Configuración

`src/main/resources/application.yaml` → `app.security.jwt.providers`.
Por entorno se sobreescribe con relaxed binding
(`APP_SECURITY_JWT_PROVIDERS_0_...`).

## Build (en backend-1 o backend-2, vía salto desde el frontend)

```bash
cd ~/Ev1CloudNative_Mountain_backend_parte2/authorizer-lambda
export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
./mvnw package -DskipTests   # genera target/function.jar
```

## Deploy (consola AWS, sin SAM CLI)

1. S3 → subir `function.jar` a un bucket.
2. Lambda → crear función `pedidos360-authorizer`: runtime Java 21, arm64,
   handler `com.lambda.validador.AuthorizerHandler::handleRequest`,
   memoria 1024, timeout 10s, código desde S3.
3. API Gateway → `backend-api` → Authorization → crear authorizer Lambda:
   payload `2.0`, respuestas simples ON, identity
   `$request.header.Authorization`, TTL 300.
4. En cada ruta protegida (`/orders`, `/cart`, `/shipments`,
   `/notifications` + bases) cambiar el authorizer JWT por el Lambda.
5. Probar: sin token → 401, con token Entra → pasa.

## Versionamiento

`X.Y.Z - descripción` (patch `+0.0.1`, minor `+0.1.0`, major `+1.0.0`).
