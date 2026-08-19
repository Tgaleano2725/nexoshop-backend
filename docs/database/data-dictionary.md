# Diccionario de datos de NexoShop

## Convenciones

- **Nulabilidad "No"** significa `NOT NULL`; **"Sí"** permite `NULL`.
- Todos los campos `id` son identidades generadas por PostgreSQL.
- `TIMESTAMP WITH TIME ZONE` se usará para conservar instantes sin depender de la zona horaria de la aplicación.
- Los campos `created_at` y `updated_at` son obligatorios y utilizan `DEFAULT CURRENT_TIMESTAMP`.
- El valor predeterminado de `updated_at` solo establece su valor durante la inserción. Las actualizaciones posteriores serán administradas por la futura aplicación mediante JPA; no se utilizarán triggers para actualizarlo automáticamente.

## `users`

Usuarios que pueden operar como clientes o administradores.

| Columna | Tipo PostgreSQL | Nulabilidad | Clave o restricción | Valor predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK, identidad | Generado como identidad | Identificador del usuario. |
| `first_name` | `VARCHAR(80)` | No | — | — | Nombres del usuario. |
| `last_name` | `VARCHAR(80)` | No | — | — | Apellidos del usuario. |
| `email` | `VARCHAR(150)` | No | UK; CHECK `email = lower(email)` | — | Correo normalizado en minúsculas utilizado para identificar al usuario. |
| `password_hash` | `VARCHAR(255)` | No | — | — | Hash de la contraseña; nunca contiene la contraseña en texto plano. |
| `role` | `VARCHAR(20)` | No | CHECK: `CUSTOMER`, `ADMIN` | — | Rol funcional del usuario. |
| `active` | `BOOLEAN` | No | — | `true` | Indica si el usuario puede operar; sustituye la eliminación física. |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante de creación. |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante inicial; las actualizaciones posteriores serán administradas por JPA. |

## `categories`

Clasificación principal del catálogo.

| Columna | Tipo PostgreSQL | Nulabilidad | Clave o restricción | Valor predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK, identidad | Generado como identidad | Identificador de la categoría. |
| `name` | `VARCHAR(100)` | No | UK | — | Nombre visible y único de la categoría. |
| `description` | `VARCHAR(500)` | Sí | — | — | Explicación opcional de la categoría. |
| `active` | `BOOLEAN` | No | — | `true` | Indica si la categoría está disponible; sustituye la eliminación física. |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante de creación. |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante inicial; las actualizaciones posteriores serán administradas por JPA. |

## `products`

Productos ofrecidos en el catálogo y su existencia disponible.

| Columna | Tipo PostgreSQL | Nulabilidad | Clave o restricción | Valor predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK, identidad | Generado como identidad | Identificador del producto. |
| `category_id` | `BIGINT` | No | FK → `categories.id`; ON DELETE RESTRICT; índice | — | Categoría a la que pertenece el producto. |
| `sku` | `VARCHAR(50)` | No | UK | — | Código único del producto. |
| `name` | `VARCHAR(150)` | No | — | — | Nombre comercial actual. |
| `description` | `TEXT` | Sí | — | — | Descripción extendida opcional. |
| `price` | `NUMERIC(12,2)` | No | CHECK `price > 0` | — | Precio de venta actual. |
| `stock` | `INTEGER` | No | CHECK `stock >= 0` | — | Unidades disponibles en el inventario único. |
| `image_url` | `VARCHAR(500)` | Sí | — | — | URL opcional de la imagen del producto. |
| `active` | `BOOLEAN` | No | — | `true` | Indica si el producto está disponible; sustituye la eliminación física. |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante de creación. |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante inicial; las actualizaciones posteriores serán administradas por JPA. |

## `carts`

Carrito vigente de un usuario. La unicidad de `user_id` limita a cada usuario a un carrito como máximo.

| Columna | Tipo PostgreSQL | Nulabilidad | Clave o restricción | Valor predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK, identidad | Generado como identidad | Identificador del carrito. |
| `user_id` | `BIGINT` | No | FK → `users.id`; UK; ON DELETE RESTRICT; índice único | — | Propietario único del carrito. |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante de creación. |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante inicial; las actualizaciones posteriores serán administradas por JPA. |

## `cart_items`

Productos y cantidades incorporados a un carrito.

| Columna | Tipo PostgreSQL | Nulabilidad | Clave o restricción | Valor predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK, identidad | Generado como identidad | Identificador del ítem. |
| `cart_id` | `BIGINT` | No | FK → `carts.id`; ON DELETE CASCADE; índice compuesto | — | Carrito que contiene el ítem. |
| `product_id` | `BIGINT` | No | FK → `products.id`; ON DELETE RESTRICT; índice | — | Producto agregado al carrito. |
| `quantity` | `INTEGER` | No | CHECK `quantity > 0` | — | Cantidad solicitada del producto. |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante de creación. |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante inicial; las actualizaciones posteriores serán administradas por JPA. |

Restricción adicional: UK compuesta sobre `cart_id, product_id`. El índice único resultante cubre la clave foránea `cart_id`; existe además un índice independiente sobre `product_id`.

## `orders`

Pedido y fotografía histórica de sus datos de entrega, importe y selección simulada de pago.

| Columna | Tipo PostgreSQL | Nulabilidad | Clave o restricción | Valor predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK, identidad | Generado como identidad | Identificador interno del pedido. |
| `order_number` | `VARCHAR(30)` | No | UK | — | Número público y único del pedido. |
| `user_id` | `BIGINT` | No | FK → `users.id`; ON DELETE RESTRICT; índice | — | Usuario que realizó el pedido. |
| `status` | `VARCHAR(30)` | No | CHECK: `PENDING`, `CONFIRMED`, `PREPARING`, `SHIPPED`, `DELIVERED`, `CANCELLED` | — | Estado logístico del pedido. |
| `payment_method` | `VARCHAR(30)` | No | CHECK: `CREDIT_CARD`, `BANK_TRANSFER`, `CASH_ON_DELIVERY` | — | Método de pago seleccionado para la simulación. |
| `payment_status` | `VARCHAR(30)` | No | CHECK: `PENDING`, `PAID`, `FAILED`, `REFUNDED` | — | Estado simulado del pago. |
| `recipient_name` | `VARCHAR(160)` | No | — | — | Nombre del destinatario al crear el pedido. |
| `recipient_phone` | `VARCHAR(30)` | No | — | — | Teléfono de contacto histórico de la entrega. |
| `shipping_address` | `VARCHAR(255)` | No | — | — | Dirección histórica de entrega. |
| `shipping_city` | `VARCHAR(100)` | No | — | — | Ciudad histórica de entrega. |
| `shipping_reference` | `VARCHAR(255)` | Sí | — | — | Indicaciones opcionales para localizar la entrega. |
| `subtotal` | `NUMERIC(12,2)` | No | CHECK `subtotal >= 0` | — | Suma de los detalles antes del envío. |
| `shipping_cost` | `NUMERIC(12,2)` | No | CHECK `shipping_cost >= 0` | `0` | Costo aplicado al envío. |
| `total` | `NUMERIC(12,2)` | No | CHECK `total >= 0`; CHECK `total = subtotal + shipping_cost` | — | Importe final del pedido. |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante de creación. |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante inicial; las actualizaciones posteriores serán administradas por JPA. |

## `order_items`

Detalle histórico de los productos incluidos en un pedido.

| Columna | Tipo PostgreSQL | Nulabilidad | Clave o restricción | Valor predeterminado | Descripción funcional |
|---|---|---|---|---|---|
| `id` | `BIGINT` | No | PK, identidad | Generado como identidad | Identificador del detalle. |
| `order_id` | `BIGINT` | No | FK → `orders.id`; ON DELETE RESTRICT; índice | — | Pedido histórico al que pertenece. |
| `product_id` | `BIGINT` | No | FK → `products.id`; ON DELETE RESTRICT; índice | — | Producto que originó el detalle. |
| `product_sku` | `VARCHAR(50)` | No | — | — | Copia histórica e intencional del SKU al comprar. |
| `product_name` | `VARCHAR(150)` | No | — | — | Copia histórica e intencional del nombre al comprar. |
| `unit_price` | `NUMERIC(12,2)` | No | CHECK `unit_price > 0` | — | Copia histórica e intencional del precio unitario al comprar. |
| `quantity` | `INTEGER` | No | CHECK `quantity > 0` | — | Unidades compradas. |
| `line_total` | `NUMERIC(12,2)` | No | CHECK `line_total > 0`; CHECK `line_total = unit_price * quantity` | — | Importe del detalle. |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante de creación. |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | No | — | `CURRENT_TIMESTAMP` | Instante inicial; las actualizaciones posteriores serán administradas por JPA. |

## Índices y unicidad

| Tabla | Índice o restricción | Finalidad |
|---|---|---|
| `users` | UK `email` | Impedir correos duplicados ya normalizados en minúsculas. |
| `categories` | UK `name` | Impedir nombres exactamente duplicados. |
| `products` | UK `sku`; índice `category_id` | Identificar productos y optimizar la relación con categorías. |
| `carts` | UK `user_id` | Aplicar la relación 1 a 0..1 y cubrir la clave foránea. |
| `cart_items` | UK (`cart_id`, `product_id`); índice `product_id` | Evitar duplicados por carrito y cubrir ambas claves foráneas. |
| `orders` | UK `order_number`; índice `user_id` | Identificar pedidos y consultar el historial de un usuario. |
| `order_items` | índice `order_id`; índice `product_id` | Optimizar ambas relaciones históricas. |

Como endurecimiento futuro específico de PostgreSQL, la unicidad de usuarios y categorías será insensible a mayúsculas mediante índices únicos de expresión sobre `lower(email)` y `lower(name)`, respectivamente. Estos índices sustituirán o complementarán las restricciones ordinarias según se defina en la futura migración, sin cambiar el modelo conceptual.
