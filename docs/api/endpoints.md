# Referencia de endpoints

Todas las rutas incluyen el prefijo `/api/v1` y producen DTO de respuesta, nunca entidades JPA. La auditoría de los cinco controladores confirma **33 operaciones**: Users (2), Categories (6), Products (9), Carts (5) y Orders (11). No se documentan operaciones adicionales.

Los errores indicados en las tablas remiten al contrato común de [`ErrorResponse`](errors.md). En todos los ejemplos los identificadores son ficticios y no representan datos reales.

## Users — 2 operaciones

### `POST /api/v1/users`

Registra un usuario `CUSTOMER` activo y devuelve `201 Created`. La cabecera `Location` apunta a `/api/v1/users/{id}`. Entrada `RegisterUserRequest`: `firstName`, `lastName` y `email` son obligatorios y tienen máximo 80, 80 y 150 caracteres; `email` debe ser válido; `password` es obligatorio y admite entre 8 y 72 caracteres. La contraseña se recibe solo en esta solicitud, se almacena como hash BCrypt y nunca aparece en `UserResponse`. Errores: `400 VALIDATION_ERROR` o `MALFORMED_REQUEST`, `409 DUPLICATE_RESOURCE`/`INTEGRITY_VIOLATION` e `500 INTERNAL_ERROR`.

### `GET /api/v1/users/{id}`

Consulta un usuario por `id` positivo (`Long`) y devuelve `200 UserResponse`. Errores: `400 VALIDATION_ERROR` o `MALFORMED_REQUEST`, `404 RESOURCE_NOT_FOUND` y `500 INTERNAL_ERROR`. La respuesta contiene `id`, nombres, email normalizado, `role`, `active`, `createdAt` y `updatedAt`; no contiene contraseña ni hash.

## Categories — 6 operaciones

### `POST /api/v1/categories`

Crea una categoría y devuelve `201 CategoryResponse` con cabecera `Location`. Entrada `CreateCategoryRequest`: `name` obligatorio, no vacío, máximo 100; `description` opcional, máximo 500. Errores: `400`, `409 DUPLICATE_RESOURCE`/`INTEGRITY_VIOLATION` y `500`.

### `PUT /api/v1/categories/{id}`

Actualiza nombre y descripción de la categoría indicada por `id` positivo. Entrada `UpdateCategoryRequest` con las mismas restricciones que la creación. Devuelve `200 CategoryResponse`. Errores: `400`, `404 RESOURCE_NOT_FOUND`, `409 DUPLICATE_RESOURCE`/`INTEGRITY_VIOLATION` y `500`.

### `GET /api/v1/categories/{id}`

Consulta una categoría por `id` positivo y devuelve `200 CategoryResponse`. Errores: `400`, `404 RESOURCE_NOT_FOUND` y `500`.

### `GET /api/v1/categories`

Lista categorías ordenadas por nombre e identificador. No recibe parámetros y devuelve `200` con `CategoryResponse[]`. Errores: `500 INTERNAL_ERROR`.

### `POST /api/v1/categories/{id}/activate`

Activa la categoría indicada por `id` positivo y devuelve `200 CategoryResponse`. Errores: `400`, `404 RESOURCE_NOT_FOUND`, `409 BUSINESS_RULE` si la transición no es válida y `500`.

### `POST /api/v1/categories/{id}/deactivate`

Desactiva la categoría indicada por `id` positivo y devuelve `200 CategoryResponse`. Errores: `400`, `404 RESOURCE_NOT_FOUND`, `409 BUSINESS_RULE` y `500`.

## Products — 9 operaciones

`ProductResponse` contiene `id`, `categoryId`, `categoryName`, `sku`, `name`, `description`, `price`, `stock`, `imageUrl`, `active`, `createdAt` y `updatedAt`. `price` es positivo con dos decimales; `stock` es no negativo; `sku` y `name` son obligatorios, con máximos de 50 y 150; `imageUrl` admite como máximo 500 caracteres.

### `POST /api/v1/products`

Crea un producto en una categoría activa y devuelve `201 ProductResponse` con `Location`. Entrada `CreateProductRequest`: `categoryId` positivo, `sku`, `name`, `price` y `stock` son obligatorios; `description` es opcional. Errores: `400`, `404 RESOURCE_NOT_FOUND` para la categoría, `409 DUPLICATE_RESOURCE`/`BUSINESS_RULE`/`INTEGRITY_VIOLATION` y `500`.

### `PUT /api/v1/products/{id}`

Actualiza los datos descriptivos, categoría, SKU, precio e imagen del producto `id` positivo. Entrada `UpdateProductRequest`: `categoryId`, `sku`, `name` y `price` obligatorios; no cambia el stock. Devuelve `200 ProductResponse`. Errores: `400`, `404`, `409 DUPLICATE_RESOURCE`/`BUSINESS_RULE`/`INTEGRITY_VIOLATION` y `500`.

### `GET /api/v1/products/{id}`

Consulta un producto por `id` positivo y devuelve `200 ProductResponse`. Errores: `400`, `404 RESOURCE_NOT_FOUND` y `500`.

### `GET /api/v1/products/sku/{sku}`

Consulta un producto por SKU. `sku` es un segmento de texto normalizado por el servicio; devuelve `200 ProductResponse`. Errores: `404 RESOURCE_NOT_FOUND` y `500`.

### `GET /api/v1/products?page=0&size=20`

Lista todos los productos en una respuesta `PageResponse<ProductResponse>`, ordenada establemente por `id` ascendente. `page` es entero mayor o igual que 0 y `size` es entero positivo. Devuelve `200`. Errores: `400 VALIDATION_ERROR` o `MALFORMED_REQUEST` y `500`.

### `POST /api/v1/products/{id}/activate`

Activa un producto `id` positivo si su categoría está activa. Devuelve `200 ProductResponse`. Errores: `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/products/{id}/deactivate`

Desactiva un producto `id` positivo y devuelve `200 ProductResponse`. Errores: `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/products/{id}/stock/increase`

Incrementa el stock del producto bloqueado para la operación. Entrada `StockAdjustmentRequest` con `quantity` obligatoria y positiva. Devuelve `200 ProductResponse`. Errores: `400`, `404`, `409 BUSINESS_RULE`/`INTEGRITY_VIOLATION` y `500`.

### `POST /api/v1/products/{id}/stock/decrease`

Reduce el stock del producto bloqueado. Entrada `StockAdjustmentRequest` con `quantity` obligatoria y positiva; nunca se permite stock negativo. Devuelve `200 ProductResponse`. Errores: `400`, `404`, `409 BUSINESS_RULE`/`INTEGRITY_VIOLATION` y `500`.

## Carts — 5 operaciones

`CartResponse` contiene `cartId`, `userId`, `items`, `lineCount`, `totalUnits`, `subtotal`, `createdAt` y `updatedAt`. Cada `CartItemResponse` contiene `productId`, `sku`, `name`, `unitPrice`, `quantity`, `lineTotal` y `availableStock`.

### `GET /api/v1/users/{userId}/cart`

Obtiene el carrito del usuario activo; si aún no existe, lo crea. `userId` debe ser positivo. Devuelve `200 CartResponse`. Errores: `400`, `404 RESOURCE_NOT_FOUND`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/users/{userId}/cart/items`

Agrega un producto activo al carrito. Entrada `AddCartItemRequest`: `productId` y `quantity` obligatorios y positivos. Si el producto ya está en el carrito, se acumula la cantidad; esta operación no reserva stock. Devuelve `200 CartResponse`. Errores: `400`, `404`, `409 BUSINESS_RULE`/`DUPLICATE_RESOURCE` y `500`.

### `PUT /api/v1/users/{userId}/cart/items/{productId}`

Establece la cantidad de un producto del carrito. `userId` y `productId` son positivos. Entrada `UpdateCartItemQuantityRequest` con `quantity` obligatoria y positiva, sin superar el stock actual. Devuelve `200 CartResponse`. Errores: `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `DELETE /api/v1/users/{userId}/cart/items/{productId}`

Retira una línea del carrito. Los identificadores son positivos y devuelve `200 CartResponse`. Errores: `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `DELETE /api/v1/users/{userId}/cart`

Vacía el carrito del usuario. `userId` positivo. Devuelve `200 CartResponse` con `items: []`, `lineCount: 0`, `totalUnits: 0` y `subtotal: 0.00`. Errores: `400`, `404` y `500`.

## Orders — 11 operaciones

`OrderResponse` contiene identificador, número de pedido, usuario, estados, snapshot de entrega, artículos históricos, importes y timestamps. El checkout conserva SKU, nombre y precio unitario de cada producto en `OrderItemResponse`.

### `POST /api/v1/users/{userId}/orders/checkout`

Convierte el carrito no vacío del usuario activo en un pedido dentro de una transacción. Entrada `CreateOrderRequest`: `paymentMethod` obligatorio (`CREDIT_CARD`, `BANK_TRANSFER` o `CASH_ON_DELIVERY`), `shippingCost` obligatorio no negativo con dos decimales, `recipientName` obligatorio máximo 160, `recipientPhone` máximo 30, `shippingAddress` máximo 255, `shippingCity` máximo 100 y `shippingReference` opcional máximo 255. Devuelve `200 OrderResponse` con `status: PENDING` y `paymentStatus: PENDING`. Descuenta stock y vacía el carrito solo si todo finaliza correctamente. Errores: `400`, `404`, `409 BUSINESS_RULE`/`INTEGRITY_VIOLATION` y `500`; una transacción fallida no crea un pedido parcial.

### `GET /api/v1/users/{userId}/orders/{orderId}`

Consulta un pedido perteneciente al usuario. Ambos identificadores son positivos y devuelve `200 OrderResponse`. Errores: `400`, `404 RESOURCE_NOT_FOUND` y `500`.

### `GET /api/v1/users/{userId}/orders?page=0&size=20`

Lista los pedidos del usuario en `PageResponse<OrderResponse>`, ordenados por `id` descendente. `page >= 0` y `size > 0`. Devuelve `200`; errores `400`, `404` para usuario inexistente y `500`.

### `POST /api/v1/users/{userId}/orders/{orderId}/cancel`

Cancela un pedido `PENDING` o `CONFIRMED`, restaura una vez el stock de sus detalles y devuelve `200 OrderResponse` con `status: CANCELLED`. Errores: `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/users/{userId}/orders/{orderId}/confirm`

Confirma un pedido `PENDING` que tiene al menos un detalle. Devuelve `200 OrderResponse` con `status: CONFIRMED`. Errores: `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/users/{userId}/orders/{orderId}/preparing`

Avanza únicamente `CONFIRMED` a `PREPARING`. Devuelve `200 OrderResponse`; errores `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/users/{userId}/orders/{orderId}/ship`

Avanza únicamente `PREPARING` a `SHIPPED`. Devuelve `200 OrderResponse`; errores `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/users/{userId}/orders/{orderId}/deliver`

Avanza únicamente `SHIPPED` a `DELIVERED`. Devuelve `200 OrderResponse`; errores `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/users/{userId}/orders/{orderId}/payment/paid`

Marca el pago como `PAID` desde `PENDING` o `FAILED`. Devuelve `200 OrderResponse`; errores `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/users/{userId}/orders/{orderId}/payment/failed`

Marca el pago como `FAILED` únicamente desde `PENDING`. Devuelve `200 OrderResponse`; errores `400`, `404`, `409 BUSINESS_RULE` y `500`.

### `POST /api/v1/users/{userId}/orders/{orderId}/payment/refund`

Marca el pago como `REFUNDED` únicamente desde `PAID`. Devuelve `200 OrderResponse`; errores `400`, `404`, `409 BUSINESS_RULE` y `500`.

## Ejemplos JSON

### Registro de usuario

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.test",
  "password": "DemoPass!42"
}
```
### Creación de categoría

```json
{
  "name": "Libros",
  "description": "Lecturas de ejemplo"
}
```

### Creación de producto

```json
{
  "categoryId": 2,
  "sku": "BOOK-DEMO-01",
  "name": "Libro de ejemplo",
  "description": "Producto ficticio para documentación",
  "price": 49.90,
  "stock": 12,
  "imageUrl": "https://example.test/book.jpg"
}
```

### Ajuste de stock

```json
{
  "quantity": 3
}
```

### Agregado y actualización del carrito

```json
{
  "productId": 3,
  "quantity": 2
}
```

```json
{
  "quantity": 1
}
```

### Checkout

```json
{
  "paymentMethod": "CASH_ON_DELIVERY",
  "shippingCost": 5.00,
  "recipientName": "Ada Lovelace",
  "recipientPhone": "+595 21 555000",
  "shippingAddress": "Calle Demo 123",
  "shippingCity": "Asunción",
  "shippingReference": "Edificio de ejemplo"
}
```

### Respuesta de pedido

```json
{
  "orderId": 4,
  "orderNumber": "NXDEMO0000000000000000000001",
  "userId": 2,
  "status": "PENDING",
  "paymentMethod": "CASH_ON_DELIVERY",
  "paymentStatus": "PENDING",
  "recipientName": "Ada Lovelace",
  "recipientPhone": "+595 21 555000",
  "shippingAddress": "Calle Demo 123",
  "shippingCity": "Asunción",
  "shippingReference": "Edificio de ejemplo",
  "items": [{
    "productId": 3,
    "productSku": "BOOK-DEMO-01",
    "productName": "Libro de ejemplo",
    "unitPrice": 49.90,
    "quantity": 2,
    "lineTotal": 99.80
  }],
  "subtotal": 99.80,
  "shippingCost": 5.00,
  "total": 104.80,
  "createdAt": "2026-08-20T05:27:46Z",
  "updatedAt": "2026-08-20T05:27:46Z"
}
```

### Respuesta paginada

```json
{
  "content": [{
    "id": 3,
    "categoryId": 2,
    "categoryName": "Libros",
    "sku": "BOOK-DEMO-01",
    "name": "Libro de ejemplo",
    "description": "Producto ficticio para documentación",
    "price": 49.90,
    "stock": 12,
    "imageUrl": "https://example.test/book.jpg",
    "active": true,
    "createdAt": "2026-08-20T05:27:46Z",
    "updatedAt": "2026-08-20T05:27:46Z"
  }],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```
