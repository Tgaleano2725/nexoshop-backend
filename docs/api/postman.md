# Uso de la colección Postman

## Archivos

Importa estos dos archivos en Postman:

- [Colección NexoShop API](../../postman/NexoShop.postman_collection.json)
- [Entorno NexoShop Local](../../postman/NexoShop.local.postman_environment.json)

En Postman selecciona **Import**, elige ambos archivos y, después, selecciona **NexoShop Local** en el selector de entornos de la esquina superior derecha. El entorno usa `http://localhost:8080` como `baseUrl`; la colección no contiene contraseñas reales, tokens, credenciales PostgreSQL ni headers de autenticación.

## Requisitos

Para ejecutar solicitudes deben estar activos PostgreSQL y Spring Boot, con la API disponible en `{{baseUrl}}`. Esta fase solo crea y valida los artefactos; la colección no ha sido ejecutada contra la API ni contra el PostgreSQL permanente.

La colección no sustituye la configuración CORS del navegador: Postman no aplica las restricciones CORS. Una respuesta exitosa en Postman no demuestra que un origen web esté autorizado.

## Flujo principal

La carpeta **00 - Flujo principal** está ordenada para ejecutarse secuencialmente:

1. registra un usuario ficticio;
2. crea una categoría;
3. crea un producto activo con stock 20;
4. obtiene o crea el carrito;
5. agrega una unidad;
6. actualiza la cantidad;
7. ejecuta checkout y crea un pedido `PENDING`;
8. consulta el pedido y sus snapshots;
9. confirma el pedido;
10. lista los pedidos con paginación;
11. prueba `PENDING → FAILED → PAID → REFUNDED` para el pago simulado;
12. prueba `CONFIRMED → PREPARING → SHIPPED → DELIVERED` para la logística.

La primera solicitud genera `runId` usando `Date.now()` y un componente aleatorio corto. Ese valor hace únicos el email `demo.{{runId}}@example.com`, la categoría y el SKU `NS-{{runId}}`. No se usan esperas artificiales ni valores dependientes del reloj fuera de esa generación de unicidad.

Cada test valida el código HTTP esperado, el JSON cuando corresponde y campos esenciales. Los scripts detienen la comprobación con mensajes claros si falta un identificador; nunca imprimen contraseñas ni cuerpos sensibles.

## Variables

El entorno incluye, habilitadas, `baseUrl`, `runId`, `userId`, `categoryId`, `productId`, `cartId`, `orderId`, `cancelOrderId`, `sku`, `page` y `size`. Los scripts posteriores a las respuestas capturan automáticamente los identificadores positivos y el SKU después de comprobar que la respuesta es JSON.

La carpeta **05 - Orders** contiene además la preparación de un segundo checkout: agrega otra unidad, crea un segundo pedido y captura `cancelOrderId`; la siguiente solicitud lo cancela para verificar la restitución indicada por el dominio. Estas solicitudes repiten rutas oficiales y no crean endpoints auxiliares.

## Ejecución

- **Manual:** abre una solicitud, comprueba el entorno seleccionado y ejecútala respetando las dependencias de variables.
- **Collection Runner:** selecciona únicamente **00 - Flujo principal** para ejecutar el flujo de extremo a extremo en orden. Después puedes ejecutar **01 - Users** a **05 - Orders** como referencia, teniendo presente que algunas operaciones de escritura crean o actualizan datos.
- **Tests:** consulta la pestaña **Test Results** de cada solicitud y la consola de Postman solo para diagnósticos que no contengan datos sensibles. Los scripts usan la API moderna `pm.*` y no usan `setTimeout`, `postman.setNextRequest` ni APIs obsoletas.
- **Errores:** ejecuta **99 - Casos de error** después de disponer de las variables del flujo. Incluye tipo de ID, paginación, validación, inexistente, enum inválido, stock insuficiente y duplicado; cada caso comprueba HTTP, `code`, `status`, `path` cuando es estable y `fieldErrors`.

## Datos y repetición

El flujo crea un usuario, una categoría, un producto, un carrito y uno o dos pedidos en PostgreSQL. Para repetirlo, vuelve a ejecutar **00 - Flujo principal**: un nuevo `runId` evita las colisiones de email, categoría y SKU. Los pedidos y demás datos creados son reales en la base local y deben limpiarse mediante el procedimiento de administración del entorno, no desde esta colección.

No introduzcas datos personales, contraseñas reales, tokens, credenciales ni información del propietario del repositorio. La contraseña de demostración aparece únicamente en el body de registro y es ficticia.

## Problemas comunes

- **Conexión rechazada:** confirma que PostgreSQL y Spring Boot estén activos y que `baseUrl` apunte al puerto correcto.
- **Entorno no seleccionado:** el texto `{{baseUrl}}` no se resolverá; selecciona **NexoShop Local**.
- **Variable sin resolver:** ejecuta primero la solicitud que la captura o revisa el valor habilitado en el entorno.
- **HTTP 409 por datos repetidos:** inicia nuevamente el flujo para generar otro `runId`; no reemplaces los datos por credenciales reales.
- **Aplicación o PostgreSQL detenidos:** inicia los servicios aprobados del proyecto y vuelve a comprobar la URL local.
