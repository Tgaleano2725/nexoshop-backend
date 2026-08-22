# Contrato oficial de la API

## Propósito

NexoShop expone una API REST académica para gestionar usuarios, catálogo, carritos y pedidos. Este contrato describe únicamente las rutas, DTO, validaciones, estados y respuestas implementados actualmente; no incorpora autenticación, JWT, autorización ni pagos externos.

## Convenciones generales

- **Versión:** `/api/v1`.
- **Servidor local:** `http://localhost:8080`.
- **Base path:** `http://localhost:8080/api/v1`.
- **Contenido:** `application/json; charset=UTF-8` para solicitudes y respuestas con cuerpo.
- **Identificadores:** enteros positivos de 64 bits (`Long`). Los ejemplos usan valores ficticios pequeños.
- **Importes:** números decimales representados por `BigDecimal`, con dos posiciones decimales. Los precios son positivos; subtotal, envío y total son no negativos.
- **Cantidades y stock:** enteros positivos en las operaciones de cantidad; el stock persistido es un entero no negativo.
- **Timestamps:** instantes ISO-8601 UTC, por ejemplo `2026-08-20T05:27:46Z`.
- **Paginación:** `page` comienza en `0` y por defecto es `0`; `size` es positivo y por defecto es `20`. Las respuestas incluyen `content`, `page`, `size`, `totalElements`, `totalPages`, `first` y `last`.
- **Catálogo público:** los listados de categorías y productos aceptan `activeOnly`; ausente o `false` conserva el listado completo y `true` devuelve únicamente recursos activos.
- **Errores:** todos utilizan el objeto [`ErrorResponse`](errors.md).
- **CORS:** se aplica a `/api/**`; por defecto permite `http://localhost:4200` y puede configurarse con `CORS_ALLOWED_ORIGINS`. No se habilitan credenciales.

## Flujo recomendado

Un consumidor puede seguir esta secuencia:

1. registrar o consultar un **usuario**;
2. crear y consultar una **categoría**;
3. crear y consultar un **producto**;
4. obtener el **carrito** y agregar o ajustar artículos;
5. ejecutar **checkout** con los datos de entrega y pago simulado;
6. consultar y avanzar el **pedido**.

Checkout descuenta el stock dentro de una transacción, vacía el carrito y crea el pedido inicialmente en estado `PENDING`. La confirmación logística (`CONFIRMED`) es una operación posterior.

## Referencias

- [Referencia de endpoints](endpoints.md)
- [Contrato de errores](errors.md)
- [Especificación OpenAPI 3.0.3](openapi.yaml)
- [Guía de Postman](postman.md)
- [Colección Postman](../../postman/NexoShop.postman_collection.json)
- [Entorno local Postman](../../postman/NexoShop.local.postman_environment.json)
- [Informe final de validación](../final-validation-report.md)
