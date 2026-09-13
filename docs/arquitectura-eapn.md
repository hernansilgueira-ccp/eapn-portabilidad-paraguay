# Arquitectura de la EAPN

## Vista general

La EAPN implementa el proceso de portabilidad mediante APIs REST, rutas de Apache Camel, mensajería asíncrona con ActiveMQ Artemis y persistencia en PostgreSQL.

```mermaid
flowchart TD
    CLIENT["Cliente o sistema receptor"]
    API["API REST<br/>Apache Camel Netty HTTP"]
    REQUEST["Ruta de solicitudes"]
    BROKER["ActiveMQ Artemis"]
    DATABASE["PostgreSQL"]

    CLIENT -->|"POST solicitud y PIN"| API
    API --> REQUEST
    REQUEST -->|"Persistencia"| DATABASE
    REQUEST -->|"Mensajes y eventos"| BROKER
    BROKER -->|"Consumidores Camel"| DATABASE
    DATABASE -->|"Estado del proceso"| API
```

## Flujo de portabilidad

```mermaid
sequenceDiagram
    actor Cliente
    participant API as API REST
    participant DB as PostgreSQL
    participant MQ as Artemis
    participant PIN as Servicio PIN
    participant Donante as Operador donante

    Cliente->>API: Crear solicitud
    API->>DB: Guardar CREATED
    API->>MQ: portability.requests
    MQ->>PIN: Procesar solicitud
    PIN->>DB: Guardar PIN_GENERATED
    PIN->>MQ: portability.pin.notifications

    Cliente->>API: Confirmar PIN
    API->>DB: Validar hash y vencimiento
    API->>MQ: portability.approvals
    MQ->>Donante: Solicitar decisión
    Donante->>MQ: Publicar resultado

    alt Solicitud aprobada
        MQ->>DB: Guardar APPROVED
        MQ->>DB: Registrar número portado
        MQ->>DB: Guardar COMPLETED
        MQ->>Cliente: Notificación final
    else Solicitud rechazada
        MQ->>DB: Guardar REJECTED
        MQ->>Cliente: Notificación de rechazo
    end
```

## Estados de la solicitud

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> PIN_GENERATED: Generar PIN
    PIN_GENERATED --> CONFIRMED: PIN válido
    PIN_GENERATED --> REJECTED: PIN inválido o vencido
    CONFIRMED --> PENDING_DONOR: Enviar al donante
    PENDING_DONOR --> APPROVED: Aprobar
    PENDING_DONOR --> REJECTED: Rechazar
    APPROVED --> COMPLETED: Ejecutar portabilidad
    REJECTED --> [*]
    COMPLETED --> [*]
```

## Canales de mensajería

| Destino | Tipo | Responsabilidad |
|---|---|---|
| `portability.requests` | Cola | Recibir solicitudes nuevas |
| `portability.pin.notifications` | Cola | Notificar el PIN generado |
| `portability.approvals` | Cola | Solicitar la decisión del operador donante |
| `portability.results` | Cola | Publicar el resultado del operador donante |
| `portability.recipient.notifications` | Cola | Notificar el resultado final |
| `portability.audit` | Cola | Persistir eventos de auditoría |
| `portability.errors.dlq` | Cola | Conservar mensajes que agotaron sus reintentos |
| `portability.status.events` | Tópico | Distribuir cambios de estado |

## Persistencia

```mermaid
erDiagram
    PORTABILITY_REQUESTS ||--o| PORTED_NUMBERS : "genera"
    PORTABILITY_REQUESTS ||--o{ PORTABILITY_AUDIT : "registra"

    PORTABILITY_REQUESTS {
        uuid request_id PK
        varchar msisdn
        varchar document_number
        varchar donor_operator
        varchar recipient_operator
        varchar status
        varchar pin_hash
        timestamp pin_expires_at
        timestamp requested_at
        timestamp updated_at
    }

    PORTED_NUMBERS {
        uuid request_id PK
        varchar msisdn UK
        varchar previous_operator
        varchar current_operator
        timestamp ported_at
    }

    PORTABILITY_AUDIT {
        uuid audit_id PK
        uuid request_id FK
        varchar event_type
        varchar status
        text details
        timestamp occurred_at
    }
```

## Manejo de errores

Los consumidores JMS tienen configurados reintentos automáticos. Cuando un mensaje no puede procesarse después de agotar los intentos permitidos, se envía a:

```text
portability.errors.dlq
```

Este mecanismo permite conservar el mensaje problemático, investigar su causa y evitar su pérdida.