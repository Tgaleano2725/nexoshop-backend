# Informe final de validación

## Alcance y fecha

Este documento registra la evidencia de validación final del backend académico NexoShop verificada el **20 de agosto de 2026**. El alcance comprende la suite automatizada, el flujo manual principal de Postman, consultas representativas, contratos de error y la limpieza técnica previa al cierre operativo.

Los identificadores, duraciones y tiempos indicados corresponden a una ejecución local específica. Pueden variar en ejecuciones posteriores sin alterar el contrato funcional.

## Suite automatizada

La ejecución completa mediante Maven produjo:

- 107 pruebas ejecutadas;
- 0 fallos;
- 0 errores;
- 0 pruebas omitidas;
- PostgreSQL real y aislado mediante Testcontainers;
- una única migración Flyway aplicada: V1;
- validación del esquema por Hibernate con `ddl-auto=validate`.

## Validación manual con Postman

La ejecución utilizó el entorno **NexoShop Local** y la URL base `http://localhost:8080`.

| Evidencia | Resultado |
|---|---:|
| Solicitudes del flujo principal | 16 |
| Assertions aprobadas | 27 |
| Assertions fallidas | 0 |
| Assertions omitidas | 0 |
| Duración observada | 2.569 segundos |
| Tiempo promedio observado | 82 ms |

El flujo funcional verificó:

1. registro de usuario;
2. creación de categoría;
3. creación de producto;
4. creación y consulta de carrito;
5. agregado y actualización de artículos;
6. checkout, descuento de inventario y vaciado del carrito;
7. consulta y listado de pedidos;
8. confirmación, preparación, envío y entrega;
9. pago fallido, recuperación hacia pagado y reembolso;
10. conservación de snapshots históricos del pedido.

## Consultas representativas

Las consultas manuales posteriores confirmaron:

- respuesta del usuario sin contraseña ni hash;
- categoría activa;
- producto activo;
- stock final de 19 unidades después de descontar una unidad;
- carrito vacío después del checkout;
- pedido final en estado `DELIVERED`;
- pago final en estado `REFUNDED`;
- subtotal de 49.90;
- costo de envío de 5.00;
- total de 54.90.

## Contratos de error

| Escenario | HTTP | Código interno |
|---|---:|---|
| Identificador no numérico | 400 | `MALFORMED_REQUEST` |
| Paginación inválida | 400 | `MALFORMED_REQUEST` |
| Validación inválida | 400 | `VALIDATION_ERROR` |
| Recurso inexistente | 404 | `RESOURCE_NOT_FOUND` |
| Enum inválido | 400 | `MALFORMED_REQUEST` |
| Stock insuficiente | 409 | `BUSINESS_RULE` |
| Recurso duplicado | 409 | `DUPLICATE_RESOURCE` |

## Limpieza técnica

La espera residual `Thread.sleep(5)` de la prueba de auditoría fue sustituida por una sincronización condicionada al avance real del reloj y protegida por un timeout monotónico. La comprobación estricta de que `updatedAt` avanza se conserva, sin pausas fijas ni cambios en reglas productivas.

La revisión final confirmó que la especificación OpenAPI conserva 33 operaciones, la colección Postman representa las mismas 33 combinaciones de método y ruta, y la migración V1 y los documentos del modelo de datos permanecen sin cambios accidentales.

## Seguridad de la evidencia

Este informe no registra contraseñas, hashes, tokens, credenciales, contenido de `.env` ni otros secretos. Los datos mencionados son exclusivamente valores funcionales y métricas de una validación local controlada.
