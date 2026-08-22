# NexoShop

Backend REST académico de comercio electrónico, desarrollado para aplicar programación orientada a objetos, arquitectura por capas, persistencia relacional y construcción de una API REST.

- Autor: Tobias González Galeano
- Estado: backend funcional y validado dentro del alcance académico
- API base: `http://localhost:8080/api/v1`

## Alcance funcional

El backend implementa:

- registro y consulta de usuarios;
- categorías y productos;
- listados públicos filtrables mediante `activeOnly=true`;
- inventario con operaciones de stock;
- carritos por usuario;
- checkout transaccional;
- pedidos y transiciones logísticas;
- estados de pago simulados;
- manejo uniforme de errores;
- control de concurrencia para evitar sobreventa.

El checkout crea el pedido en estado `PENDING`; la confirmación es una transición posterior.

## Fuera de alcance

Esta entrega no incluye autenticación, JWT, autorización, pasarela de pago externa, frontend Angular ni despliegue productivo. El frontend se mantiene separado del backend y los pagos son únicamente simulados.

## Stack tecnológico

- Java 25
- Spring Boot 4.1.0
- Maven Wrapper
- Spring Web MVC, Spring Data JPA y Hibernate
- PostgreSQL 18.6
- Flyway
- Docker Compose
- JUnit, MockMvc y Testcontainers
- H2 únicamente para pruebas aisladas de contexto

## Arquitectura

NexoShop es un monolito organizado por capas. El flujo principal es:

`Controller → Service → Repository → PostgreSQL`

Los DTO representan los contratos HTTP y los mappers convierten entre DTO y entidades, evitando exponer entidades JPA directamente desde los controladores. Flyway es la autoridad del esquema y Hibernate utiliza `ddl-auto=validate`.

Consulta el detalle en [la arquitectura](docs/architecture.md) y [el dominio y sus reglas](docs/domain-and-business-rules.md).

## Requisitos y ejecución local

Requisitos:

- JDK 25 con `JAVA_HOME` configurado;
- Docker Engine y Docker Compose.

Crea la configuración local a partir del ejemplo y reemplaza el valor de contraseña de ejemplo por uno seguro, sin versionar `.env`:

```bash
cp .env.example .env
```

Inicia PostgreSQL y comprueba su healthcheck:

```bash
docker compose up -d postgres
docker compose ps postgres
```

Para iniciar la API, carga las variables en la terminal actual y ejecuta el Maven Wrapper:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080/api/v1`. CORS permite por defecto `http://localhost:4200` y puede configurarse con `CORS_ALLOWED_ORIGINS`.

Ejecuta la suite con:

```bash
./mvnw test
```

Para detener PostgreSQL conservando el volumen persistente:

```bash
docker compose stop postgres
```

> **Advertencia:** `docker compose down -v` elimina el volumen `postgres_data` y todos los datos locales de PostgreSQL. Úsalo únicamente si deseas perder esos datos.

## Documentación

- [Índice documental](docs/README.md)
- [Arquitectura](docs/architecture.md)
- [Dominio y reglas de negocio](docs/domain-and-business-rules.md)
- [Contrato oficial de la API](docs/api/README.md)
- [Referencia de endpoints](docs/api/endpoints.md)
- [Especificación OpenAPI 3.0.3](docs/api/openapi.yaml)
- [Guía de Postman](docs/api/postman.md)
- [Colección Postman](postman/NexoShop.postman_collection.json)
- [Entorno local Postman](postman/NexoShop.local.postman_environment.json)
- [Informe final de validación](docs/final-validation-report.md)
- [Diagrama de entidad-relación](docs/database/der.md)
- [Diccionario de datos](docs/database/data-dictionary.md)
- [Modelo DBML](docs/database/nexoshop.dbml)

## Pruebas y validación

La suite automatizada actual contiene 110 pruebas satisfactorias, con cobertura unitaria, MVC y persistencia real mediante PostgreSQL/Testcontainers. El informe final de validación conserva el checkpoint anterior del 20 de agosto de 2026, compuesto por 107 pruebas y la validación manual de Postman. Flyway aplica únicamente V1 y Hibernate valida el esquema existente mediante `ddl-auto=validate`; `open-in-view` permanece deshabilitado.
