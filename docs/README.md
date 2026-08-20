# Documentación de NexoShop

Este directorio reúne la documentación oficial del backend. Cada documento tiene una fuente de verdad concreta y describe únicamente el comportamiento implementado.

## Documentos

| Documento | Propósito | Fuente de verdad |
|---|---|---|
| [Arquitectura](architecture.md) | Capas, dependencias, transacciones, persistencia y concurrencia. | Paquetes Java, configuración, servicios, repositorios y pruebas. |
| [Dominio y reglas de negocio](domain-and-business-rules.md) | Entidades, POO, estados y reglas funcionales. | Entidades, enums, servicios, DTO y pruebas. |
| [API](api/README.md) | Propósito, convenciones y flujo de consumo. | Controladores y DTO actuales. |
| [Endpoints](api/endpoints.md) | Referencia humana de las 33 operaciones REST. | Controladores, DTO, validaciones, servicios y pruebas. |
| [Errores de API](api/errors.md) | Formato y códigos de error HTTP. | `GlobalExceptionHandler` y `ErrorResponse`. |
| [OpenAPI](api/openapi.yaml) | Contrato importable OpenAPI 3.0.3. | Rutas y schemas del código actual. |
| [Postman](api/postman.md) | Guía de importación y ejecución controlada. | Colección y entorno local. |
| [DER](database/der.md) | Modelo conceptual y relaciones. | Modelo relacional aprobado y V1. |
| [Diccionario de datos](database/data-dictionary.md) | Columnas, tipos, restricciones e índices. | `V1__create_initial_schema.sql`. |
| [DBML](database/nexoshop.dbml) | Representación DBML del esquema. | V1 y diccionario de datos. |

La documentación específica de endpoints y una colección Postman se incorporarán en una fase posterior, cuando se defina ese entregable. Este índice no enlaza archivos inexistentes.
