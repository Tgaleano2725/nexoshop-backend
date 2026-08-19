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

```bash
./mvnw test
```

## Arquitectura prevista

El backend será un monolito organizado por capas de configuración, controladores, DTO de entrada y salida, excepciones, mapeadores, entidades y enumeraciones del modelo, repositorios, servicios y validaciones.

## Estado actual

El DER está aprobado y la infraestructura local PostgreSQL con la migración inicial de Flyway está disponible. Todavía no existen componentes Java de negocio.

## Documentación del modelo de datos

- [Diagrama de Entidad-Relación](docs/database/der.md)
- [Diccionario de datos](docs/database/data-dictionary.md)
- [Modelo DBML](docs/database/nexoshop.dbml)
