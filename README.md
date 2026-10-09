# Pedidos360 backend parte 2 (EC2 backend-2)

Microservicios que corren en el EC2 backend-2 (`10.0.146.240`):

| Servicio | Puerto | Descripción |
|---|---|---|
| servicio-notificaciones | 8084 | Email por pedido + `POST /notifications/enviar` directo (Postman) |
| servicio-carrito | 8085 | Carrito persistente por usuario (`/cart`) |
| servicio-envios | 8086 | Despachos, MS nuevo a elección (`/shipments`, consume `pedido.creado`) |
| authorizer-lambda/ | — | Lambda authorizer REQUEST del `backend-api` (serverless, no va en el compose) |

## Contratos MQ

Exchange duradero `pedidos.eventos`, routing key `pedido.creado`, cola `pedidos.creados`
(broker RabbitMQ en backend-1, `RABBITMQ_HOST=10.0.128.111`):

```json
{"orderId":"PEDIDO-100001","customerEmail":"a@b.cl","customerName":"Ana",
 "items":[{"productId":1,"quantity":2}],"total":1798}
```

## Seguridad

JWT de Azure Entra ID validado en cada MS (defensa en profundidad además del
JWT Authorizer del AWS API Gateway). Scopes: `orders.write`.

## Build y deploy (en backend-2, vía salto desde el frontend)

```bash
git clone <este-repo> ~/Ev1CloudNative_Mountain_backend_parte2
cp .env.example .env  # completar secretos, nunca commitear .env
export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
./mvnw package -DskipTests
docker compose up -d --build
```

## Versionamiento

`X.Y.Z - descripción`: patch `+0.0.1` cambios pequeños, minor `+0.1.0`
funcional, major `+1.0.0` grandes.
