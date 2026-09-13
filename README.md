# EAPN – Portabilidad Numérica Paraguay

Proyecto académico desarrollado para la materia **Integración de Sistemas II**.

Implementa una simulación del proceso de portabilidad numérica entre operadores móviles de Paraguay utilizando una arquitectura orientada a eventos.

## Integrantes

- Hernan Silgueira
- Antonio Aguero
- Victor Martinez

## Objetivo

Simular el flujo completo de una solicitud de portabilidad numérica:

1. Recepción de la solicitud mediante una API REST.
2. Persistencia inicial en PostgreSQL.
3. Publicación de eventos mediante Apache ActiveMQ Artemis.
4. Generación y validación de un PIN.
5. Envío de la solicitud al operador donante.
6. Aprobación o rechazo de la portabilidad.
7. Actualización del operador actual del número.
8. Notificación del resultado al operador receptor.
9. Registro completo de auditoría.

## Tecnologías utilizadas

- Java 21
- Gradle
- Apache Camel 4.22
- Apache ActiveMQ Artemis 2.44
- PostgreSQL 17
- Docker Compose
- Jackson
- JUnit 5
- Camel Test
- Netty HTTP

## Arquitectura general
La documentación y los diagramas detallados están disponibles en
[Arquitectura de la EAPN](docs/arquitectura-eapn.md).

El sistema utiliza APIs REST para recibir solicitudes y Apache ActiveMQ Artemis para procesarlas de manera asíncrona.

```text
Cliente
   |
   | POST /api/portability/requests
   v
API REST
   |
   v
PostgreSQL
   |
   v
portability.requests
   |
   v
Generación de PIN
   |
   v
portability.pin.notifications
   |
   | POST /api/portability/pin-confirmations
   v
Validación del PIN
   |
   v
portability.approvals
   |
   v
Decisión del operador donante
   |
   v
portability.results
   |
   v
Finalización de portabilidad
   |
   +--> ported_numbers
   |
   +--> portability.recipient.notifications
```

Los cambios de estado también se publican en:

```text
portability.audit
portability.status.events
```

## Estados de una solicitud

| Estado | Descripción |
|---|---|
| `CREATED` | Solicitud creada |
| `PIN_GENERATED` | PIN generado y enviado |
| `CONFIRMED` | PIN confirmado correctamente |
| `PENDING_DONOR` | Solicitud enviada al operador donante |
| `APPROVED` | Solicitud aprobada |
| `REJECTED` | Solicitud rechazada |
| `COMPLETED` | Portabilidad finalizada |

## Requisitos previos

Antes de ejecutar el proyecto se necesita:

- Java 21
- Docker Desktop
- Git
- PowerShell
- Puertos disponibles `5432`, `61616`, `8161` y `8082`

No es necesario instalar Gradle porque el proyecto incluye Gradle Wrapper.

## Configuración inicial

Clonar el repositorio:

```powershell
git clone https://github.com/hernansilgueira-ccp/eapn-portabilidad-paraguay.git
cd eapn-portabilidad-paraguay
```

Levantar PostgreSQL y ActiveMQ Artemis:

```powershell
docker compose up -d
docker compose ps
```

Los contenedores deben aparecer activos y PostgreSQL debe mostrar el estado `healthy`.

## Ejecutar las pruebas

En Windows:

```powershell
.\gradlew.bat clean test
```

Resultado esperado:

```text
BUILD SUCCESSFUL
```
La suite automatizada contiene actualmente **30 pruebas**:

- 30 ejecutadas.
- 0 fallidas.
- 0 errores.
- 0 omitidas.

El reporte HTML de las pruebas se genera en:

```text
app/build/reports/tests/test/index.html
```

## Ejecutar la aplicación

Desactivar la solicitud automática de demostración:

```powershell
Remove-Item Env:EAPN_DEMO_ENABLED -ErrorAction SilentlyContinue
```

Iniciar la aplicación:

```powershell
.\gradlew.bat :app:run
```

La aplicación permanece ejecutándose y Gradle puede mostrar:

```text
75% EXECUTING
```

Este comportamiento es normal porque Apache Camel continúa escuchando solicitudes y mensajes.

Para detenerla se utiliza:

```text
Ctrl + C
```

## Crear una solicitud de portabilidad

Endpoint:

```text
POST http://localhost:8082/api/portability/requests
```

Ejemplo en PowerShell:

```powershell
$body = @{
    msisdn = "595985555555"
    documentNumber = "1234568"
    donorOperator = "TIGO"
    recipientOperator = "PERSONAL"
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8082/api/portability/requests" `
    -ContentType "application/json" `
    -Body $body

$response
```

Respuesta esperada:

```json
{
  "requestId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status": "CREATED"
}
```

Guardar el identificador:

```powershell
$requestId = $response.requestId
```

## Confirmar el PIN

El PIN generado se publica en la cola:

```text
portability.pin.notifications
```

Puede consultarse desde la consola de Artemis:

```text
http://localhost:8161/console/artemis
```

Luego se confirma mediante:

```text
POST http://localhost:8082/api/portability/pin-confirmations
```

Ejemplo:

```powershell
$pinBody = @{
    requestId = $requestId
    pin = "123456"
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8082/api/portability/pin-confirmations" `
    -ContentType "application/json" `
    -Body $pinBody
```

Respuesta exitosa:

```json
{
  "requestId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "status": "CONFIRMED"
}
```

## Códigos HTTP

| Código | Significado |
|---|---|
| `201` | Solicitud creada |
| `200` | PIN confirmado |
| `400` | JSON o datos inválidos |
| `409` | Solicitud procesada previamente |
| `422` | PIN incorrecto o vencido |
| `500` | Error interno no controlado |

## Regla de decisión del operador donante

Para fines de demostración, el operador donante aplica la siguiente regla:

- Si el número de documento termina en `9`, la solicitud es rechazada.
- En los demás casos, la solicitud es aprobada.

Esta regla permite demostrar los dos resultados posibles sin depender de un sistema externo real.

## Seguridad del PIN

El PIN:

- Tiene 6 dígitos.
- Se genera de forma segura.
- Tiene una vigencia limitada.
- No se almacena en texto plano.
- Se almacena utilizando un hash SHA-256.
- Se marca como consumido después de una confirmación válida.

## Colas de Artemis

| Cola | Función |
|---|---|
| `portability.requests` | Solicitudes nuevas |
| `portability.pin.notifications` | Notificaciones con el PIN |
| `portability.approvals` | Solicitudes para el operador donante |
| `portability.results` | Resultado del operador donante |
| `portability.recipient.notifications` | Notificación final al operador receptor |
| `portability.audit` | Eventos para auditoría |
| `portability.errors.dlq` | Mensajes que no pudieron procesarse |

También se utiliza el tópico:

```text
portability.status.events
```

## Reintentos y DLQ

Los consumidores JMS realizan hasta tres reintentos cuando ocurre un error.

Si el mensaje continúa fallando, se envía a:

```text
portability.errors.dlq
```

Esto evita la pérdida silenciosa de mensajes y permite analizar posteriormente los errores.

## Base de datos

El proyecto utiliza las tablas:

### `portability_requests`

Almacena las solicitudes, operadores, estados, PIN y fechas del proceso.

### `ported_numbers`

Almacena los números cuya portabilidad fue completada correctamente.

### `portability_audit`

Almacena el historial completo de cambios de estado.

Los scripts de creación se encuentran en:

```text
database/init/
```

## Consultas de verificación

Consultar una solicitud:

```powershell
docker compose exec postgres psql `
    -U eapn_user `
    -d eapn `
    -c "SELECT request_id, msisdn, donor_operator, recipient_operator, status FROM portability_requests ORDER BY requested_at DESC;"
```

Consultar números portados:

```powershell
docker compose exec postgres psql `
    -U eapn_user `
    -d eapn `
    -c "SELECT request_id, msisdn, previous_operator, current_operator, ported_at FROM ported_numbers;"
```

Consultar la auditoría:

```powershell
docker compose exec postgres psql `
    -U eapn_user `
    -d eapn `
    -c "SELECT request_id, event_type, status, details, occurred_at FROM portability_audit ORDER BY occurred_at;"
```

## Auditoría esperada

Una solicitud aprobada genera normalmente esta secuencia:

```text
CREATED
PIN_GENERATED
CONFIRMED
PENDING_DONOR
APPROVED
COMPLETED
```

Una solicitud rechazada finaliza en:

```text
REJECTED
```

## Validaciones principales

El sistema valida:

- Formato internacional del MSISDN.
- Documento obligatorio.
- Operador donante obligatorio.
- Operador receptor obligatorio.
- Operadores donante y receptor diferentes.
- Existencia de la solicitud.
- Estado actual de la solicitud.
- PIN correcto.
- PIN no vencido.
- Confirmaciones duplicadas.
- Integridad de los números portados.

## Evidencias

Las capturas de las pruebas y ejecuciones se encuentran en:

```text
docs/evidencias/
```

Incluyen evidencias de:

- Compilación exitosa.
- Contenedores activos.
- Creación de solicitudes.
- Mensajes de Artemis.
- Generación y confirmación del PIN.
- Aprobación del operador donante.
- Finalización de la portabilidad.
- Registro en PostgreSQL.
- Auditoría completa.
- API REST de solicitudes.

## Credenciales locales

PostgreSQL:

```text
Usuario: eapn_user
Base de datos: eapn
Contraseña: eapn_password
```

La consola local de Artemis utiliza las credenciales definidas en `compose.yaml`.

Estas credenciales son exclusivamente para el entorno académico y local.

## Detener el entorno

Detener los contenedores sin eliminar los datos:

```powershell
docker compose down
```

No utilizar `docker compose down -v` si se desea conservar la información almacenada en PostgreSQL y Artemis.

## Estado del proyecto

El proyecto incluye:

- API REST para creación de solicitudes.
- API REST para confirmación del PIN.
- Mensajería asíncrona con Artemis.
- Persistencia en PostgreSQL.
- Auditoría completa.
- Reintentos y Dead Letter Queue.
- Validación de solicitudes duplicadas.
- Pruebas automatizadas.
- Evidencias de ejecución.