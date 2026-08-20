# Documentación de NexoShop

Este directorio reúne la documentación oficial del backend. Cada documento tiene una fuente de verdad concreta y describe únicamente el comportamiento implementado.

## Documentos

| Documento | Propósito | Fuente de verdad |
|---|---|---|
| [Arquitectura](architecture.md) | Capas, dependencias, transacciones, persistencia y concurrencia. | Paquetes Java, configuración, servicios, repositorios y pruebas. |
| [Dominio y reglas de negocio](domain-and-business-rules.md) | Entidades, POO, estados y reglas funcionales. | Entidades, enums, servicios, DTO y pruebas. |
| [DER](database/der.md) | Modelo conceptual y relaciones. | Modelo relacional aprobado y V1. |
| [Diccionario de datos](database/data-dictionary.md) | Columnas, tipos, restricciones e índices. | `V1__create_initial_schema.sql`. |
| [DBML](database/nexoshop.dbml) | Representación DBML del esquema. | V1 y diccionario de datos. |

La documentación específica de endpoints y una colección Postman se incorporarán en una fase posterior, cuando se defina ese entregable. Este índice no enlaza archivos inexistentes.
