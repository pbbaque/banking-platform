# Banking Platform — Manual Tests

Este documento recoge las pruebas manuales end-to-end realizadas sobre Banking Platform.

Su objetivo es conservar evidencia reproducible de los flujos principales del sistema y de los escenarios de consistencia distribuida que no quedan completamente representados por los tests unitarios e integración automatizados.

## Entorno local

Servicios:

| Servicio         | Puerto |
| ---------------- | -----: |
| customer-service |   8081 |
| identity-service |   8082 |
| account-service  |   8083 |
| transfer-service |   8084 |
| audit-service    |   8085 |
| Kafka            |   9092 |

Token interno local:

```text
banking-platform-local-internal-token
```

Usuario empleado en las pruebas:

```text
username: jwt-test-user
password: SecurePassword123!
```

Identity user:

```text
101b897b-2382-43d7-a7ac-2d1a6969f0a6
```

Customer:

```text
4b79210c-1cf3-4a37-9180-d7185eea3301
```

Cuenta principal de Alice:

```text
cb5e47a2-47ae-49b5-9b34-5da5433786b5
```

Cuenta de Bob:

```text
0f1ffb06-3b9b-4aae-9c53-ffbe60fcc5f1
```

---

# 1. Authentication

## Objetivo

Validar el flujo de autenticación mediante `identity-service` y la emisión de un JWT válido para acceder al resto de servicios protegidos.

## Request

```http
POST http://localhost:8082/api/v1/auth/login
Content-Type: application/json
```

Body:

```json
{
  "username": "jwt-test-user",
  "password": "SecurePassword123!"
}
```

## Resultado esperado

```text
HTTP 200
```

La respuesta debe incluir:

```text
userId
username
status
lastLoginAt
accessToken
```

Resultado observado:

```text
userId:
101b897b-2382-43d7-a7ac-2d1a6969f0a6

username:
jwt-test-user

status:
ACTIVE

accessToken:
JWT firmado por identity-service
```

El token incluye:

```text
iss = banking-platform
sub = 101b897b-2382-43d7-a7ac-2d1a6969f0a6
username = jwt-test-user
```

## Resultado

```text
PASS
```

---

# 2. Internal Customer Authentication

## Objetivo

Comprobar que los endpoints internos del `customer-service` requieren el token interno de servicios.

Header esperado:

```http
X-Internal-Service-Token: banking-platform-local-internal-token
```

## Casos

### Sin token interno

Request a endpoint `/api/internal/**` sin header.

Resultado esperado:

```text
401 / 403
```

según el punto exacto de rechazo de Spring Security.

### Token incorrecto

```http
X-Internal-Service-Token: invalid-token
```

Resultado esperado:

```text
request rechazada
```

### Token correcto

```http
X-Internal-Service-Token: banking-platform-local-internal-token
```

Resultado esperado:

```text
request permitida
```

## Resultado

```text
PASS
```

---

# 3. Transfer Recovery — Overview

`transfer-service` implementa recuperación explícita de transferencias mediante etapas persistidas.

Estados de procesamiento:

```text
DEBIT_PENDING
CREDIT_PENDING
COMPENSATION_PENDING
TERMINAL
RECOVERY_REQUIRED
```

Flujo normal:

```text
DEBIT_PENDING
      ↓
debit source
      ↓
CREDIT_PENDING
      ↓
credit destination
      ↓
COMPLETED
      ↓
TERMINAL
```

Si falla el crédito al destino:

```text
CREDIT_PENDING
      ↓
credit destination FAIL
      ↓
COMPENSATION_PENDING
      ↓
credit source
      ↓
FAILED
      ↓
TERMINAL
```

Las operaciones sobre `account-service` utilizan UUID deterministas para garantizar idempotencia:

```text
DEBIT
CREDIT
COMPENSATION
```

Esto permite reanudar transferencias sin ejecutar dos veces una operación de saldo ya aplicada.

---

# 4. Recovery from DEBIT_PENDING

## Objetivo

Validar que una transferencia persistida en:

```text
PENDING
DEBIT_PENDING
```

pueda reanudarse correctamente.

## Transfer

```text
id:
aaaaaaaa-1111-4111-8111-aaaaaaaaaaaa

idempotencyKey:
e2e-recovery-debit-pending-001

amount:
10 EUR
```

Balances antes de la recuperación:

```text
Alice: 720 EUR
Bob:   471 EUR
```

## Acción

Repetir la creación de la transferencia usando la misma `idempotencyKey`.

## Resultado esperado

El sistema debe:

```text
ejecutar debit Alice
ejecutar credit Bob
marcar transferencia COMPLETED
marcar processingStage TERMINAL
```

Balances resultantes:

```text
Alice: 710 EUR
Bob:   481 EUR
```

## Validación

La transferencia terminó:

```text
status = COMPLETED
processingStage = TERMINAL
```

El importe se movió exactamente una vez.

## Resultado

```text
PASS
```

---

# 5. Recovery from CREDIT_PENDING

## Objetivo

Comprobar que una transferencia cuyo débito ya ocurrió no vuelva a debitar la cuenta origen al recuperarse.

## Transfer

```text
id:
bbbbbbbb-2222-4222-8222-bbbbbbbbbbbb

idempotencyKey:
e2e-recovery-credit-pending-001

amount:
10 EUR
```

Antes del replay:

```text
Alice: 700 EUR
Bob:   481 EUR
```

El débito de Alice ya había ocurrido.

## Acción

Reprocesar la transferencia desde:

```text
PENDING
CREDIT_PENDING
```

## Resultado esperado

Debe ejecutarse únicamente:

```text
credit Bob
```

No debe repetirse:

```text
debit Alice
```

Balances después:

```text
Alice: 700 EUR
Bob:   491 EUR
```

Transfer:

```text
status = COMPLETED
processingStage = TERMINAL
```

El hecho de que Alice permanezca en `700 EUR` demuestra que el débito no se ejecutó nuevamente.

## Resultado

```text
PASS
```

---

# 6. Recovery from COMPENSATION_PENDING

## Objetivo

Validar la recuperación cuando:

```text
debit source = OK
credit destination = FAIL
```

y queda pendiente devolver el dinero a la cuenta origen.

## Transfer

```text
id:
dddddddd-4444-4444-8444-dddddddddddd

idempotencyKey:
e2e-recovery-compensation-pending-002

amount:
10 EUR
```

Estado previo:

```text
status = PENDING
processingStage = COMPENSATION_PENDING
```

## Acción

Reprocesar transferencia.

## Resultado esperado

Debe ejecutarse:

```text
credit source
```

como compensación.

UUID determinista de la operación:

```text
9c8745aa-0d37-3cdb-9f1f-10bb1d24c4ef
```

Operación registrada:

```text
operationType = CREDIT
amount = 10
resultingBalance = 690
```

Transfer final:

```text
status = FAILED
processingStage = TERMINAL
```

Failure reason:

```text
Destination account could not be credited; source debit was compensated
```

Esto representa una transferencia fallida funcionalmente, pero cuyo débito fue correctamente revertido.

## Resultado

```text
PASS
```

---

# 7. Audit E2E — Outbox + Kafka

## Objetivo

Validar el flujo completo:

```text
Transfer
   ↓
Transactional Outbox
   ↓
Kafka
   ↓
Audit
```

## Transfer utilizada

```text
id:
ec4defd2-7d66-4e75-af32-668c5ad6b091

idempotencyKey:
e2e-audit-transfer-001

amount:
5 EUR
```

Resultado de transferencia:

```text
COMPLETED
```

## Outbox

Evento generado:

```text
ID:
254FF386D50048C7B3D54B76EA5D7C86

EVENT_TYPE:
TRANSFER_COMPLETED

STATUS:
PUBLISHED

ATTEMPTS:
0

PUBLISHED_AT:
not null
```

## Audit

El mismo `eventId` apareció en:

```text
BANK_AUDIT.AUDIT_EVENTS
```

Datos relevantes:

```text
aggregateType = TRANSFER
outcome = SUCCESS
description = Transfer completed successfully
```

## Resultado

```text
PASS
```

Esto valida el flujo:

```text
Transfer DB
   +
Outbox DB
   ↓
Kafka
   ↓
Audit DB
```

---

# 8. Current Customer — `/customers/me`

## Objetivo

Permitir que el frontend resuelva el `Customer` correspondiente al usuario autenticado sin conocer previamente su `customerId`.

## Request

```http
GET http://localhost:8081/api/v1/customers/me
Authorization: Bearer <JWT>
```

`customer-service` utiliza:

```text
JWT.sub
```

como:

```text
identityUserId
```

y busca el `Customer` correspondiente.

## Resultado observado

```json
{
  "id": "4b79210c-1cf3-4a37-9180-d7185eea3301",
  "identityUserId": "101b897b-2382-43d7-a7ac-2d1a6969f0a6",
  "firstName": "Alice",
  "lastName": "Test",
  "documentType": "DNI",
  "documentNumber": "11111111A",
  "dateOfBirth": "1990-01-01",
  "phoneNumber": "+34600000001",
  "fiscalAddress": "Calle Test 1",
  "postalCode": "48001",
  "city": "Bilbao",
  "countryCode": "ES",
  "status": "PENDING_VERIFICATION"
}
```

## Resultado

```text
PASS
```

---

# 9. Customer Service Security

## Objetivo

Validar que los endpoints públicos autenticados de Customer requieren un JWT válido.

Endpoint probado:

```http
GET /api/v1/customers/me
```

## JWT válido

```http
Authorization: Bearer <valid-token>
```

Resultado:

```text
request permitida
```

## Sin JWT

Resultado:

```text
request rechazada
```

## JWT inválido

Resultado:

```text
request rechazada
```

## Resultado

```text
PASS
```

---

# 10. Frontend Authentication

## Objetivo

Validar la integración inicial de Angular con `identity-service`.

El frontend utiliza:

```text
POST /identity-api/api/v1/auth/login
```

mediante el proxy Angular.

JWT almacenado en:

```text
localStorage
banking_access_token
```

El frontend dispone de:

```text
AuthService
authGuard
authInterceptor
```

## Login

Credenciales:

```text
jwt-test-user
SecurePassword123!
```

Resultado:

```text
login correcto
token almacenado
navegación autenticada habilitada
```

## Logout

Resultado:

```text
token eliminado
estado de autenticación actualizado
usuario redirigido correctamente
```

## Resultado

```text
PASS
```

---

# 11. Account Transfer History

## Objetivo

Permitir consultar todas las transferencias relacionadas con una cuenta concreta.

Endpoint:

```http
GET /api/transfers?accountId={accountId}
Authorization: Bearer <JWT>
```

Cuenta probada:

```text
cb5e47a2-47ae-49b5-9b34-5da5433786b5
```

## Seguridad

Antes de devolver el historial:

```text
transfer-service
    ↓
account-service
    ↓
valida ownership mediante JWT
```

El usuario solo puede consultar el historial de cuentas que posee.

## Resultado observado

El endpoint devolvió transferencias donde la cuenta aparece como:

```text
sourceAccountId
```

o:

```text
destinationAccountId
```

El resultado está ordenado:

```text
createdAt DESC
```

Ejemplos recuperados:

```text
ec4defd2-7d66-4e75-af32-668c5ad6b091
COMPLETED
5 EUR

eeeeeeee-5555-4555-8555-eeeeeeeeeeee
FAILED
10 EUR

dddddddd-4444-4444-8444-dddddddddddd
FAILED
10 EUR

bbbbbbbb-2222-4222-8222-bbbbbbbbbbbb
COMPLETED
10 EUR

aaaaaaaa-1111-4111-8111-aaaaaaaaaaaa
COMPLETED
10 EUR

93be01d9-7f45-44d9-bab0-dd4e26108c9d
COMPLETED
10 EUR
```

## Resultado

```text
PASS
```

---

# 12. Transfer Detail

## Objetivo

Permitir consultar una transferencia individual cuando el usuario autenticado posee cualquiera de las cuentas implicadas.

Endpoint:

```http
GET /api/transfers/{transferId}
Authorization: Bearer <JWT>
```

Antes de este cambio, la autorización solo comprobaba:

```text
sourceAccountId
```

Ahora se permite acceso si el usuario posee:

```text
sourceAccountId
OR
destinationAccountId
```

## Flujo

```text
comprobar source ownership
        ↓
si pertenece -> permitir

si no pertenece
        ↓
comprobar destination ownership
        ↓
si pertenece -> permitir
```

Los errores reales de comunicación con `account-service` no se interpretan como simples fallos de ownership.

## Resultado

La consulta de detalle con una transferencia válida y una cuenta perteneciente al usuario funcionó correctamente.

## Resultado

```text
PASS
```

---

# 13. Historical Balance per Transfer

## Objetivo

Mostrar en el historial el saldo real que tenía la cuenta inmediatamente después de cada movimiento, en lugar de mostrar el saldo actual repetido en todas las transferencias.

Ejemplo del problema anterior:

```text
Transfer -5 EUR     saldo 695 EUR
Transfer -10 EUR    saldo 695 EUR
Transfer -10 EUR    saldo 695 EUR
```

Ese resultado era incorrecto porque el frontend utilizaba:

```text
Account.balance
```

que representa el saldo actual, no el saldo histórico posterior a cada operación.

## Fuente de verdad

`account-service` ya persiste cada movimiento en:

```text
ACCOUNT_OPERATIONS
```

Entidad:

```text
AccountOperation
```

Campos relevantes:

```text
id
accountId
operationType
amount
resultingBalance
createdAt
```

`resultingBalance` representa:

```text
saldo de la cuenta inmediatamente después de ejecutar la operación
```

No se añadió ningún campo histórico nuevo a:

```text
BANK_TRANSFER.TRANSFERS
```

El saldo histórico sigue perteneciendo al dominio Account.

## Nuevo endpoint interno

Se añadió:

```http
GET /api/internal/accounts/operations/{operationId}
X-Internal-Service-Token: banking-platform-local-internal-token
```

Respuesta:

```json
{
  "id": "...",
  "accountId": "...",
  "operationType": "DEBIT",
  "amount": 5,
  "resultingBalance": 695,
  "createdAt": "..."
}
```

Se introdujo el DTO:

```text
AccountOperationResponse
```

en:

```text
account-service
transfer-service
```

El DTO de `transfer-service` representa exclusivamente el contrato HTTP recibido desde Account y no importa clases internas del otro microservicio.

## Correlación Transfer -> AccountOperation

`transfer-service` ya genera IDs deterministas para:

```text
DEBIT
CREDIT
COMPENSATION
```

a partir del `transferId`.

Conceptualmente:

```text
transferId
    │
    ├── debit(transferId)
    │      ↓
    │   AccountOperation
    │      ↓
    │   resultingBalance
    │
    ├── credit(transferId)
    │      ↓
    │   AccountOperation
    │      ↓
    │   resultingBalance
    │
    └── compensation(transferId)
           ↓
        AccountOperation
           ↓
        resultingBalance
```

## Resolución según perspectiva

### Transferencia enviada COMPLETED

Se consulta:

```text
DEBIT
```

El saldo mostrado corresponde al saldo del origen tras el débito.

### Transferencia recibida COMPLETED

Se consulta:

```text
CREDIT
```

El saldo mostrado corresponde al saldo del destino tras recibir el dinero.

### Transferencia FAILED compensada

Se intenta primero:

```text
COMPENSATION
```

porque representa el saldo después de devolver el dinero a la cuenta origen.

### Transferencia FAILED antes del débito

No existe operación de saldo.

Resultado:

```text
balanceAfter = null
```

### Transferencia PENDING con debit ya ejecutado

Se utiliza:

```text
DEBIT.resultingBalance
```

Esto permite representar posteriormente en frontend:

```text
Total        = resultingBalance + amount
Retenido     = amount
Disponible   = resultingBalance
```

## Build validation

Después de implementar los cambios se ejecutó:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"; mvn clean verify
```

en:

```text
account-service
transfer-service
```

Resultado:

```text
account-service  -> BUILD SUCCESS
transfer-service -> BUILD SUCCESS
```

## Prueba manual — AccountOperation DEBIT

Transfer:

```text
ec4defd2-7d66-4e75-af32-668c5ad6b091
```

Operation ID calculado:

```text
b2282b5e-cafb-355a-bf4b-3cc88395df03
```

Request:

```http
GET /api/internal/accounts/operations/b2282b5e-cafb-355a-bf4b-3cc88395df03
```

Resultado:

```text
id:
b2282b5e-cafb-355a-bf4b-3cc88395df03

accountId:
cb5e47a2-47ae-49b5-9b34-5da5433786b5

operationType:
DEBIT

amount:
5

resultingBalance:
695
```

Resultado:

```text
PASS
```

## Prueba manual — COMPENSATION

Transfer:

```text
dddddddd-4444-4444-8444-dddddddddddd
```

Operation ID:

```text
9c8745aa-0d37-3cdb-9f1f-10bb1d24c4ef
```

Resultado:

```text
operationType:
CREDIT

amount:
10

resultingBalance:
690
```

El `operationType` es `CREDIT` porque la compensación consiste precisamente en devolver fondos a la cuenta origen mediante un crédito.

Resultado:

```text
PASS
```

## `balanceAfter` en TransferResponse

`TransferResponse` expone ahora:

```text
balanceAfter
```

Este campo no se persiste en Transfer.

Se resuelve dinámicamente utilizando la `AccountOperation` correspondiente.

## Prueba manual — historial completo

Request:

```http
GET http://localhost:8084/api/transfers?accountId=cb5e47a2-47ae-49b5-9b34-5da5433786b5
Authorization: Bearer <JWT>
```

Resultados relevantes:

| Transfer                               | Status    | Amount | balanceAfter |
| -------------------------------------- | --------- | -----: | -----------: |
| `ec4defd2-7d66-4e75-af32-668c5ad6b091` | COMPLETED |      5 |          695 |
| `eeeeeeee-5555-4555-8555-eeeeeeeeeeee` | FAILED    |     10 |          700 |
| `dddddddd-4444-4444-8444-dddddddddddd` | FAILED    |     10 |          690 |
| `bbbbbbbb-2222-4222-8222-bbbbbbbbbbbb` | COMPLETED |     10 |         null |
| `aaaaaaaa-1111-4111-8111-aaaaaaaaaaaa` | COMPLETED |     10 |          710 |
| `93be01d9-7f45-44d9-bab0-dd4e26108c9d` | COMPLETED |     10 |          720 |
| `f253c9ca-c5a2-46dd-8554-1ca9d6083234` | FAILED    |     10 |          730 |
| `6db4510a-1693-42da-b12c-a985ca252c42` | FAILED    |     10 |          730 |
| `fccc4ab9-95c7-4495-abc7-91a976d55656` | PENDING   |     10 |          730 |
| `9914bd7b-f5dd-451c-bfb6-1cbb6f2f03c7` | COMPLETED |     25 |          740 |

Esto demuestra que el sistema devuelve saldos históricos diferentes correspondientes a operaciones diferentes.

Ejemplo:

```text
5 EUR  -> balanceAfter 695
10 EUR -> balanceAfter 710
10 EUR -> balanceAfter 720
25 EUR -> balanceAfter 740
```

Ya no se utiliza el saldo actual de la cuenta para representar movimientos históricos.

## Transferencias antiguas sin AccountOperation correlacionable

Algunas transferencias antiguas devuelven:

```text
balanceAfter = null
```

Ejemplos observados:

```text
bbbbbbbb-2222-4222-8222-bbbbbbbbbbbb

bb9c5282-c223-48ff-a141-2f9a3b743dc0

32f4145a-b7cd-4a57-b188-67ef2951af01

d365d07b-ba86-433f-874d-aaeb9d286f57

2c230536-bf13-421c-868d-8e9b9f87e5f9

bb683083-5777-452b-afc7-2c0c12f6fef8
```

Estas transferencias pertenecen a pruebas anteriores o estados creados antes de que todas las operaciones estuvieran correlacionadas mediante los UUID deterministas actuales.

No se reconstruyen artificialmente los saldos históricos.

Contrato:

```text
si existe AccountOperation correlacionable
    -> balanceAfter = resultingBalance

si no existe
    -> balanceAfter = null
```

El frontend debe ocultar el saldo cuando:

```text
balanceAfter == null
```

en lugar de inventar un valor.

## Pending validado

Transfer:

```text
fccc4ab9-95c7-4495-abc7-91a976d55656
```

Estado:

```text
PENDING
```

Amount:

```text
10 EUR
```

Balance after debit:

```text
730 EUR
```

Esto permite representar:

```text
Saldo total      740 EUR
Retenido          10 EUR
Disponible       730 EUR
```

sin almacenar datos adicionales en Transfer.

## Failed compensation validada

Ejemplos:

```text
eeeeeeee-5555-4555-8555-eeeeeeeeeeee
balanceAfter = 700

dddddddd-4444-4444-8444-dddddddddddd
balanceAfter = 690

f253c9ca-c5a2-46dd-8554-1ca9d6083234
balanceAfter = 730

6db4510a-1693-42da-b12c-a985ca252c42
balanceAfter = 730
```

Todos corresponden a transferencias:

```text
FAILED
```

con:

```text
Destination account could not be credited; source debit was compensated
```

Por tanto, el saldo mostrado es el posterior a la devolución del dinero, no el saldo temporal posterior al débito.

## Resultado

```text
PASS
```

---

# Summary

Pruebas manuales completadas:

|   # | Flujo                             | Resultado |
| --: | --------------------------------- | --------- |
|   1 | Authentication                    | PASS      |
|   2 | Internal Customer Authentication  | PASS      |
|   3 | Transfer Recovery Overview        | VALIDATED |
|   4 | Recovery DEBIT_PENDING            | PASS      |
|   5 | Recovery CREDIT_PENDING           | PASS      |
|   6 | Recovery COMPENSATION_PENDING     | PASS      |
|   7 | Transfer → Outbox → Kafka → Audit | PASS      |
|   8 | Current Customer `/customers/me`  | PASS      |
|   9 | Customer Service Security         | PASS      |
|  10 | Frontend Authentication           | PASS      |
|  11 | Account Transfer History          | PASS      |
|  12 | Transfer Detail                   | PASS      |
|  13 | Historical Balance per Transfer   | PASS      |

Estado actual:

```text
Identity authentication             PASS
Customer JWT integration            PASS
Customer /me                        PASS
Account ownership                   PASS
Account balance operations          PASS
Remote operation idempotency        PASS
Transfer processing                 PASS
Transfer recovery                   PASS
Transfer compensation               PASS
Transfer account history            PASS
Transfer detail                     PASS
Historical transfer balance         PASS
Transactional outbox                PASS
Kafka publishing                    PASS
Audit persistence                   PASS
Frontend authentication             PASS
```

El backend queda validado para continuar con la integración del historial de transferencias en Angular usando:

```text
TransferResponse.balanceAfter
```

Reglas previstas para frontend:

```text
COMPLETED
    -> mostrar balanceAfter como saldo

PENDING outgoing + balanceAfter
    -> total = balanceAfter + amount
    -> retenido = amount
    -> disponible = balanceAfter

FAILED compensada
    -> importe visualmente tachado
    -> mostrar balanceAfter restaurado

balanceAfter == null
    -> no mostrar saldo histórico
```
