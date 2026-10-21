# Banking Platform

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![Angular](https://img.shields.io/badge/Angular-22-red)
![TypeScript](https://img.shields.io/badge/TypeScript-blue)
![Oracle](https://img.shields.io/badge/Oracle-Database-red)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-black)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)

Plataforma bancaria full-stack desarrollada como proyecto personal de portfolio con arquitectura de microservicios.

El objetivo del proyecto es implementar un flujo bancario completo y realista, incluyendo autenticación, gestión de clientes y cuentas, transferencias, idempotencia, compensación de operaciones, recuperación ante fallos y auditoría mediante Kafka.

## Tecnologías

### Backend

- Java 17
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- Maven
- Oracle Database
- Flyway
- Apache Kafka
- JWT con claves RSA
- REST APIs
- Transactional Outbox Pattern

### Frontend

- Angular 22
- TypeScript
- SCSS
- RxJS
- Angular Signals
- Standalone Components
- Reactive Forms
- Diseño responsive y mobile-first

### Infraestructura y calidad

- Docker
- Docker Compose
- JUnit
- Mockito
- JaCoCo
- SpotBugs
- Checkstyle
- Spotless

## Arquitectura

El backend está dividido en cinco microservicios independientes:

| Servicio           | Puerto | Responsabilidad                      |
| ------------------ | -----: | ------------------------------------ |
| `customer-service` |   8081 | Gestión de clientes                  |
| `identity-service` |   8082 | Autenticación y emisión de JWT       |
| `account-service`  |   8083 | Cuentas, saldos y operaciones        |
| `transfer-service` |   8084 | Transferencias y compensaciones      |
| `audit-service`    |   8085 | Persistencia de eventos de auditoría |

Cada microservicio es propietario de su propio esquema Oracle:

```text
BANK_IDENTITY
BANK_CUSTOMER
BANK_ACCOUNT
BANK_TRANSFER
BANK_AUDIT
```

No existen foreign keys ni joins entre esquemas pertenecientes a distintos microservicios.

## Funcionalidades principales

- Autenticación mediante JWT firmado con RSA.
- Protección de endpoints públicos e internos.
- Validación de propiedad de cuentas.
- Operaciones de débito y crédito idempotentes.
- Transferencias entre cuentas.
- Compensación automática cuando falla el crédito de destino.
- Recuperación de transferencias interrumpidas.
- Persistencia del estado de procesamiento de las transferencias.
- Transactional Outbox.
- Publicación de eventos mediante Kafka.
- Auditoría persistente e idempotente.
- Migraciones de base de datos con Flyway.
- Tests unitarios e integración.

## Flujo de transferencias

Una transferencia normal sigue este proceso:

```text
DEBIT_PENDING
      ↓
Débito en cuenta origen
      ↓
CREDIT_PENDING
      ↓
Crédito en cuenta destino
      ↓
COMPLETED
```

Si el crédito de destino falla:

```text
CREDIT_PENDING
      ↓
COMPENSATION_PENDING
      ↓
Compensación en cuenta origen
      ↓
FAILED
```

El estado de procesamiento se guarda en base de datos, permitiendo reanudar transferencias interrumpidas sin repetir movimientos ya realizados.

## Auditoría

Los eventos terminales de las transferencias utilizan un transactional outbox:

```text
transfer-service
      ↓
OUTBOX_EVENTS
      ↓
Apache Kafka
      ↓
audit-service
      ↓
AUDIT_EVENTS
```

Este flujo ha sido validado de extremo a extremo.

## Estructura del repositorio

```text
banking-platform/
├── backend/
│   ├── identity-service/
│   ├── customer-service/
│   ├── account-service/
│   ├── transfer-service/
│   └── audit-service/
│
├── frontend/
├── infrastructure/
│   ├── oracle/
│   └── kafka/
│
├── docs/
├── start-backend.ps1
├── stop-backend.ps1
└── README.md
```

## Ejecución local

### Requisitos

- Java 17
- Maven
- Node.js
- npm
- Docker Desktop

### Oracle

```powershell
cd infrastructure/oracle
docker compose up -d
```

### Kafka

```powershell
cd ../kafka
docker compose up -d
```

### Backend

Desde la raíz del proyecto:

```powershell
.\start-backend.ps1
```

Para detener los cinco microservicios:

```powershell
.\stop-backend.ps1
```

### Frontend

```powershell
cd frontend
npm install
npm start
```

Aplicación disponible en:

```text
http://localhost:4200
```

## Estado del proyecto

### Backend

- [x] Autenticación
- [x] Clientes
- [x] Cuentas
- [x] Seguridad entre microservicios
- [x] Transferencias
- [x] Idempotencia
- [x] Compensación
- [x] Recuperación de transferencias
- [x] Kafka
- [x] Auditoría
- [x] Tests e integración E2E

### Frontend

- [x] Configuración Angular
- [x] Login y logout
- [x] JWT interceptor
- [x] Route guard
- [x] Shell responsive y mobile-first
- [ ] Dashboard
- [ ] Cuentas
- [ ] Transferencias
- [ ] Perfil
- [ ] Auditoría

## Objetivo

Proyecto personal desarrollado para demostrar conocimientos de desarrollo full-stack, Java/Spring Boot, Angular, arquitectura de microservicios, seguridad, persistencia, mensajería asíncrona y consistencia distribuida.

## Disclaimer

Este proyecto tiene fines de portfolio.

No está afiliado a ninguna entidad financiera ni está pensado para utilizarse como sistema bancario de producción.
