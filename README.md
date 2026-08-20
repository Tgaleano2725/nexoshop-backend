# NexoShop

Backend REST académico para un sistema de comercio electrónico, orientado a demostrar principios de programación orientada a objetos.

## Stack tecnológico

- Java 25
- Spring Boot 4.1.0
- Maven Wrapper
- Spring Web MVC, Spring Data JPA y Validation
- PostgreSQL y Flyway
- JUnit, Spring Boot Starter Test y H2 solamente para pruebas iniciales aisladas

## Requisitos locales

- JDK 25 con `JAVA_HOME` configurado
- Docker Engine y Docker Compose

## PostgreSQL local

La infraestructura utiliza PostgreSQL 18.6 mediante la imagen oficial `postgres:18.6-bookworm`.

Crear la configuración local y reemplazar la contraseña de ejemplo por una contraseña segura:

```bash
cp .env.example .env
```

Iniciar PostgreSQL:

```bash
docker compose up -d postgres
```

Comprobar el estado y el healthcheck:

```bash
docker compose ps postgres
```

Detener PostgreSQL sin eliminar el volumen persistente:

```bash
docker compose stop postgres
```

> **Operación destructiva:** el siguiente comando elimina los contenedores y el volumen con todos los datos locales de PostgreSQL.

```bash
docker compose down -v
```

Para iniciar Spring Boot, cargar primero las variables en la terminal actual y ejecutar el proyecto con Maven Wrapper:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

Flyway aplica automáticamente las migraciones pendientes durante el inicio.

## Pruebas

La suite conserva una prueba de contexto aislada con H2 y ejecuta las pruebas reales de persistencia mediante Testcontainers con PostgreSQL 18.6. Docker debe estar disponible; el contenedor efímero se elimina automáticamente al finalizar.

```bash
./mvnw test
```

## Arquitectura

El backend es un monolito organizado por capas de configuración, DTO de entrada y salida, excepciones, mapeadores, entidades y enumeraciones del modelo, repositorios, servicios y validaciones. Los controladores se incorporarán en una fase posterior.

La capa de aplicación del catálogo coordina los casos de uso de categorías y productos. Sus servicios transaccionales aplican reglas de negocio y traducen errores de persistencia, los repositorios encapsulan Spring Data JPA, los mapeadores manuales evitan exponer entidades y `PageResponse` mantiene la paginación desacoplada de Spring Data para la futura API.

La capa de aplicación del carrito permite obtener o crear el carrito de un usuario, agregar productos, actualizar cantidades, retirar líneas y vaciarlo. El carrito valida la disponibilidad y el stock actual, pero no reserva ni descuenta inventario; el futuro checkout deberá volver a validar ambos antes de confirmar el pedido.

La capa de pedidos permite convertir el carrito en un pedido dentro de una única transacción: bloquea usuario, carrito y productos en orden estable, toma snapshots históricos, descuenta el inventario y vacía el carrito. La cancelación repone el stock una sola vez; no se implementan pagos externos.

La API REST se expone bajo `/api/v1` para usuarios, categorías, productos, carritos y pedidos. El registro usa BCrypt y nunca devuelve contraseñas ni hashes. CORS se configura con `CORS_ALLOWED_ORIGINS` (por defecto `http://localhost:4200`). Esta entrega no incluye autenticación ni autorización de producción.

Para ejecutar localmente, configura las variables de `.env.example` y ejecuta `./mvnw spring-boot:run`; la suite completa se ejecuta con `./mvnw test`.

## Estado actual

El DER está aprobado, la infraestructura local PostgreSQL con la migración inicial de Flyway está disponible y el modelo JPA está implementado.

El dominio contiene las entidades `User`, `Category`, `Product`, `Cart`, `CartItem`, `Order` y `OrderItem`, junto con sus enumeraciones y auditoría JPA. El catálogo y el carrito disponen de repositorios, DTO, mapeadores y servicios; la API REST todavía está pendiente.

La futura capa de servicio deberá crear cada pedido completo, con su primer detalle, dentro de una única transacción mediante la fábrica del agregado `Order`.

## Documentación del modelo de datos

- [Diagrama de Entidad-Relación](docs/database/der.md)
- [Diccionario de datos](docs/database/data-dictionary.md)
- [Modelo DBML](docs/database/nexoshop.dbml)
