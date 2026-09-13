# Evidencias del proyecto

Este directorio contiene las evidencias técnicas de la implementación y validación de la EAPN de Portabilidad Numérica Paraguay.

| N.º | Archivo | Evidencia |
|---:|---|---|
| 01 | `01-mensaje-portability-requests-artemis.png` | Solicitud publicada en Artemis |
| 02 | `02-mensaje-json-portability-requests.png` | Cuerpo JSON de la solicitud |
| 03 | `03-solicitud-persistida-postgresql.png` | Persistencia inicial en PostgreSQL |
| 04 | `04-solicitudes-pin-generated-postgresql.png` | Estado `PIN_GENERATED` |
| 05 | `05-notificaciones-pin-artemis.png` | Notificación del PIN en Artemis |
| 06 | `06-api-pin-confirmado.png` | Confirmación del PIN mediante API |
| 07 | `07-pin-confirmado-postgresql.png` | Confirmación registrada en PostgreSQL |
| 08 | `08-solicitud-aprobacion-operador-donante.png` | Solicitud enviada al operador donante |
| 09 | `09-resultado-aprobado-artemis.png` | Resultado aprobado publicado en Artemis |
| 10 | `10-solicitud-aprobada-postgresql.png` | Estado `APPROVED` en PostgreSQL |
| 11 | `11-portabilidad-completada-postgresql.png` | Portabilidad finalizada |
| 12 | `12-notificacion-final-artemis.png` | Notificación final al operador receptor |
| 13 | `13-confirmacion-duplicada-http-409.png` | Control de confirmación duplicada |
| 14 | `14-mensaje-invalido-dlq-artemis.png` | Mensaje inválido enviado a la DLQ |
| 15 | `15-auditoria-created-postgresql.png` | Primer evento de auditoría |
| 16 | `16-trazabilidad-completa-postgresql.png` | Historial completo de estados |
| 17 | `17-creacion-api-rest-completada.png` | Flujo completo iniciado mediante API REST |
| 18 | `18-validacion-tecnica-final.png` | Docker Compose válido y build exitoso |

## Resultado final de las pruebas

```text
Pruebas ejecutadas: 30
Fallos: 0
Errores: 0
Omitidas: 0