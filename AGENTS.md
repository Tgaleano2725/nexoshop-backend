# Reglas permanentes del proyecto

- Usar Java 25 y Spring Boot 4.1.0.
- Mantener un backend monolítico mediante API REST.
- Usar PostgreSQL como única base de datos de ejecución.
- Permitir H2 únicamente para pruebas iniciales aisladas.
- Organizar la arquitectura futura por capas: `config`, `controller`, `dto/request`, `dto/response`, `exception`, `mapper`, `model/entity`, `model/enums`, `repository`, `service`, `service/impl` y `validation`.
- Escribir los identificadores del código en inglés y la documentación académica en español.
- Usar exclusivamente inyección por constructor; no inyectar campos con `@Autowired`.
- No exponer entidades JPA directamente desde controladores.
- Representar importes monetarios con `BigDecimal`, nunca con `double` ni `float`.
- No usar `ddl-auto=create` ni `ddl-auto=update`; gestionar las migraciones con Flyway.
- No incorporar Lombok por ahora.
- No almacenar ni versionar contraseñas, tokens ni secretos.
- No agregar microservicios ni funcionalidades fuera del alcance aprobado.
- Ejecutar las pruebas pertinentes después de cada modificación.
- No hacer commit ni push sin autorización explícita.
- No crear entidades, DTO, repositorios, servicios, controladores, seguridad ni migraciones hasta que el DER sea aprobado.
