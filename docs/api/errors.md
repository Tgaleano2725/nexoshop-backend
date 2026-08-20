# Contrato de errores

Todas las respuestas de error usan `application/json` y el record `ErrorResponse`:

| Propiedad | Tipo JSON | Descripción |
|---|---|---|
| `timestamp` | `string`, `date-time` | Instante UTC generado por el servidor. |
| `status` | `integer` | Código HTTP numérico. |
| `code` | `string` | Código interno estable del contrato. |
| `message` | `string` | Mensaje comprensible y seguro. |
| `path` | `string` | Ruta solicitada, sin credenciales ni cuerpo. |
| `fieldErrors` | `array` | Lista de `{ field, message }`; queda vacía cuando no aplica. |

El orden y los nombres anteriores coinciden con `ErrorResponse` y `FieldErrorResponse`. Los mensajes de validación pueden variar según la restricción, pero el código, el estado y la estructura permanecen uniformes.

## Códigos manejados

### `400 VALIDATION_ERROR`

Se produce cuando un DTO no cumple Jakarta Validation o cuando falla la validación de parámetros anotados (`@Positive`, `@Min`, etc.). `fieldErrors` contiene los campos del cuerpo cuando Spring los identifica; para validación de parámetros puede quedar vacío.

```json
{
  "timestamp": "2026-08-20T05:27:46Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/products",
  "fieldErrors": [
    { "field": "price", "message": "must be greater than 0.00" }
  ]
}
```

### `400 MALFORMED_REQUEST`

Se produce para JSON ilegible, enums desconocidos, argumentos ilegales y parámetros de ruta o consulta que no pueden convertirse al tipo declarado. Incluye `MethodArgumentTypeMismatchException` explícitamente. La respuesta no incluye el valor rechazado, stack trace ni nombres internos.

```json
{
  "timestamp": "2026-08-20T05:27:46Z",
  "status": 400,
  "code": "MALFORMED_REQUEST",
  "message": "Request parameter has an invalid format",
  "path": "/api/v1/categories/abc",
  "fieldErrors": []
}
```

Para un cuerpo JSON ilegible el mensaje seguro es `Request body or parameter is invalid`.

### `404 RESOURCE_NOT_FOUND`

Se produce cuando el usuario, categoría, producto, carrito o pedido solicitado no existe en el alcance de la operación.

```json
{
  "timestamp": "2026-08-20T05:27:46Z",
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Product not found: 3",
  "path": "/api/v1/products/3",
  "fieldErrors": []
}
```

### `409 DUPLICATE_RESOURCE`

Se produce cuando una operación intenta repetir un email, nombre de categoría, SKU u otra combinación única gestionada por el servicio.

```json
{
  "timestamp": "2026-08-20T05:27:46Z",
  "status": 409,
  "code": "DUPLICATE_RESOURCE",
  "message": "Email already exists",
  "path": "/api/v1/users",
  "fieldErrors": []
}
```

### `409 BUSINESS_RULE`

Se produce cuando la entrada es formalmente válida pero infringe una regla de dominio: stock insuficiente, recurso inactivo, cantidad no permitida, checkout vacío o transición de pedido/pago inválida.

```json
{
  "timestamp": "2026-08-20T05:27:46Z",
  "status": 409,
  "code": "BUSINESS_RULE",
  "message": "Insufficient stock",
  "path": "/api/v1/users/2/orders/checkout",
  "fieldErrors": []
}
```

### `409 INTEGRITY_VIOLATION`

Se produce cuando PostgreSQL rechaza una escritura por una restricción de integridad que no fue traducida como duplicado específico. La respuesta pública no contiene SQL ni detalles del proveedor.

```json
{
  "timestamp": "2026-08-20T05:27:46Z",
  "status": 409,
  "code": "INTEGRITY_VIOLATION",
  "message": "Request conflicts with existing data",
  "path": "/api/v1/products",
  "fieldErrors": []
}
```

### `500 INTERNAL_ERROR`

Es la respuesta genérica para excepciones inesperadas. El servidor registra la excepción completa mediante SLF4J junto con método y ruta, pero la respuesta no expone stack trace, SQL, credenciales, contraseñas, hashes, tokens ni otros detalles internos.

```json
{
  "timestamp": "2026-08-20T05:27:46Z",
  "status": 500,
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "path": "/api/v1/products",
  "fieldErrors": []
}
```

## Reglas de consumo

- Un cliente debe decidir por `status` y `code`, no analizar textos libres.
- `fieldErrors` siempre está presente y siempre es una lista, aunque esté vacía.
- Ningún error documentado contiene datos de autenticación: esta API no implementa autenticación ni JWT.
- Los códigos no enumerados aquí no forman parte del contrato actual.
