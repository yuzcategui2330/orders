# Documentación de Prompts - Proyecto Prueba Técnica

Historial de las instrucciones usadas para construir la API de tokenización, clientes, pedidos y compras. Cada sección conserva el sentido del prompt original, redactado de forma continua.

## Tabla de contenidos

1. [Inicialización del contexto y paquetes](#1-inicialización-del-contexto-y-paquetes)
2. [Dominio de usuarios y estándar de mapeo](#2-dominio-de-usuarios-y-estándar-de-mapeo)
3. [API de usuarios, excepciones y respuestas HTTP](#3-api-de-usuarios-excepciones-y-respuestas-http)
4. [Actualización, estado activo y autenticación](#4-actualización-estado-activo-y-autenticación)
5. [Request de login](#5-request-de-login)
6. [Spring Security y JWT](#6-spring-security-y-jwt)
7. [Estándar para los modelos siguientes](#7-estándar-para-los-modelos-siguientes)
8. [Clientes y registro público](#8-clientes-y-registro-público)
9. [Usuario activo por defecto](#9-usuario-activo-por-defecto)
10. [Catálogo y búsqueda de productos](#10-catálogo-y-búsqueda-de-productos)
11. [Tokenización de tarjetas](#11-tokenización-de-tarjetas)
12. [Órdenes, reserva de inventario y pago](#12-órdenes-reserva-de-inventario-y-pago)
13. [Pago con el token de la tarjeta](#13-pago-con-el-token-de-la-tarjeta)
14. [Correo de orden pagada o rechazada](#14-correo-de-orden-pagada-o-rechazada)
15. [Auditoría de transacciones](#15-auditoría-de-transacciones)
16. [Docker y despliegue en Render](#16-docker-y-despliegue-en-render)
17. [Integración, pruebas y despliegue continuo](#17-integración-pruebas-y-despliegue-continuo)
18. [Semilla de 150 productos](#18-semilla-de-150-productos)
19. [Descripción y rendimiento de la búsqueda](#19-descripción-y-rendimiento-de-la-búsqueda)
20. [Descripciones en la semilla](#20-descripciones-en-la-semilla)
21. [Documentación del proyecto](#21-documentación-del-proyecto)
22. [Despliegue en Google Cloud](#22-despliegue-en-google-cloud)
23. [Colección de Postman](#23-colección-de-postman)
24. [Listados de clientes y órdenes](#24-listados-de-clientes-y-órdenes)

## 1. Inicialización del contexto y paquetes

**Fase / Objetivo:** Inicialización del contexto.

**Prompt utilizado:**

> Actúa como desarrollador backend senior. Vamos a trabajar con Java y Spring Boot en un sistema API que simule tokenización de tarjetas de crédito, gestión de clientes y pedidos, y un flujo de compras en línea. La estructura de paquetes es config, controller, domains, services y repositories. Crea la paquetería correspondiente.

**Propósito:** Fijar el stack, el alcance de la prueba y la organización del código antes de modelar datos.

**Resultado esperado / Entregable:** Paquetes `config`, `controller`, `domains`, `services` y `repositories` bajo el paquete base de la aplicación.

## 2. Dominio de usuarios y estándar de mapeo

**Fase / Objetivo:** Definición del modelo de usuarios.

**Prompt utilizado:**

> Crea el dominio y la migración de users con id bigserial, username de 25 caracteres, password, created_at, updated_at y last_login, todos los tiempos como timestamp without time zone. En el mapeo usa JsonProperty y Column en snake_case, y la variable Java en camelCase, por ejemplo created_at y createdAt. Usa ese estándar, columnas en snake_case y nombres en inglés, en las demás clases de dominio.

**Propósito:** Dejar la primera tabla y la convención de nombres que el resto del modelo debía repetir.

**Resultado esperado / Entregable:** Entidad User, migración Flyway y el criterio de mapeo JSON, columnas y Java para las entidades siguientes.

## 3. API de usuarios, excepciones y respuestas HTTP

**Fase / Objetivo:** Definición de la API de usuarios.

**Prompt utilizado:**

> Crea el curl de users. En la creación, si el username ya existe, responde 404 con el mensaje Username Already Exist. Crea un paquete de excepciones para reutilizar el mismo manejo en los servicios siguientes. Los controllers deben responder con ResponseEntity.

**Propósito:** Cerrar el alta de usuarios con un error uniforme y un formato de respuesta HTTP común.

**Resultado esperado / Entregable:** Endpoint de creación, ejemplo curl, excepción de API reutilizable y respuestas envueltas en ResponseEntity.

## 4. Actualización, estado activo y autenticación

**Fase / Objetivo:** Identidad y sesión.

**Prompt utilizado:**

> Faltó el update. Agrega is_active a users como booleano con default false, modificando la migración existente y sin crear otra de Flyway. Implementa login, refresh token y change password. La contraseña debe tener más de 8 caracteres e incluir letras y números, también al crear el usuario. Si no cumple, devuelve el error en la respuesta.

**Propósito:** Completar el ciclo de usuario y las reglas mínimas de contraseña junto con el inicio de sesión.

**Resultado esperado / Entregable:** Actualización de usuario, columna is_active, política de contraseña y los tres flujos de autenticación.

## 5. Request de login

**Fase / Objetivo:** Ajuste del contrato de login.

**Prompt utilizado:**

> Para el login usa un request que pida solo username y password, no el objeto completo de users.

**Propósito:** Evitar que el login reciba o exponga el modelo persistente.

**Resultado esperado / Entregable:** DTO de login con username y password, y el endpoint consumiendo solo esos campos.

## 6. Spring Security y JWT

**Fase / Objetivo:** Seguridad de la API.

**Prompt utilizado:**

> Complementa Spring Security y el uso de JWT. La sesión debe ser STATELESS. Agrega un filtro, antes del filtro de autorización, que verifique el access token con la misma clave HMAC y rechace firma inválida o token vencido. change-password debe tomar el usuario del token, no el username del body.

**Propósito:** Hacer que la sesión no dependa del servidor y que el cambio de contraseña quede atado a quien presenta el token.

**Resultado esperado / Entregable:** Configuración stateless, filtro JWT y cambio de contraseña resuelto desde el access token.

## 7. Estándar para los modelos siguientes

**Fase / Objetivo:** Continuidad del diseño.

**Prompt utilizado:**

> Tenlo en cuenta para los siguientes modelos a implementar.

**Propósito:** Confirmar que el estándar de mapeo, seguridad y respuestas aplica a clientes, productos, tarjetas y órdenes.

**Resultado esperado / Entregable:** Los modelos posteriores siguen snake_case en JSON y base de datos, camelCase en Java, ResponseEntity y el esquema de seguridad ya definido.

## 8. Clientes y registro público

**Fase / Objetivo:** Gestión de clientes.

**Prompt utilizado:**

> Crea la tabla clients con id, name, last_name, phone, email, user_id, address, created_at y updated_at. Implementa creación, actualización, consulta por id y listado. Para el alta usa un endpoint make-registration que cree cliente y usuario en el mismo request. Ese endpoint debe estar desprotegido.

**Propósito:** Relacionar cliente y usuario en un solo registro público y dejar el resto de operaciones de cliente autenticadas.

**Resultado esperado / Entregable:** Migración y entidad Client, CRUD del cliente y el registro combinado sin JWT.

## 9. Usuario activo por defecto

**Fase / Objetivo:** Ajuste de datos de usuario.

**Prompt utilizado:**

> is_active por default en true.

**Propósito:** Corregir el valor inicial del usuario para que una cuenta recién creada quede activa.

**Resultado esperado / Entregable:** La columna is_active nace en true, tanto en la migración como al persistir el usuario.

## 10. Catálogo y búsqueda de productos

**Fase / Objetivo:** Catálogo y búsqueda.

**Prompt utilizado:**

> Crea un enum de categorías con MEDICINES, HYGYENE y PERSONAL_CARE, asociado a products. La tabla products lleva id, name, short_name, category, stock_quantity y created_at. Implementa una búsqueda por nombre o descripción, con paginación opcional, que solo devuelva productos cuyo stock sea mayor que MIN_STOCK_THRESHOLD, leído de la configuración. Cada búsqueda exitosa debe guardar término, cantidad de resultados y fecha en search_logs, de forma asíncrona, sin afectar la respuesta si el log falla. Incluye índices y pruebas del filtro de stock y del guardado asíncrono.

**Propósito:** Tener un catálogo consultable para el flujo de compra, con un umbral de stock y una auditoría que no retrase la respuesta.

**Resultado esperado / Entregable:** Enum, migración de products, endpoint de búsqueda paginada, propiedad de stock mínimo, tabla search_logs, ejecución asíncrona y pruebas.

## 11. Tokenización de tarjetas

**Fase / Objetivo:** Tokenización y seguridad de datos de tarjeta.

**Prompt utilizado:**

> Crea credit_cards con id, card_number, cvv, expiration_month, expiration_year, holder_name y client_id. Implementa un servicio que reciba esos datos, valide Luhn, CVV y vencimiento, genere un token único y guarde la tarjeta protegiendo el dato sensible. Expón POST /api/v1/tokens protegido por API Key en el header. Si la clave falta o no coincide, responde 401. Antes de tokenizar, aplica una probabilidad configurable de rechazo; si el azar cae en ese rango, responde un error de negocio indicando que el proveedor rechazó la operación. Documenta api-key y rejection-rate en la configuración y cubre tokenización, rechazo y API Key con pruebas.

**Propósito:** Sustituir el PAN por un token y simular el rechazo del proveedor sin debilitar la autenticación del endpoint.

**Resultado esperado / Entregable:** Tabla y entidad de tarjeta, endpoint de tokenización, filtro de API Key, tasa de rechazo configurable, cifrado en reposo y pruebas.

## 12. Órdenes, reserva de inventario y pago

**Fase / Objetivo:** Carrito, inventario y compra.

**Prompt utilizado:**

> Implementa el carrito y la gestión de órdenes con reserva de inventario. Products debe tener price y reserved_qty; el disponible es stock menos reservado. Order lleva client_id, amount, status DRAFT, PAID o CANCELLED, delivered y fechas. OrderDetail lleva order_id, product_id, quantity, amount y fechas. Crear una orden la deja en DRAFT. Agregar un producto valida stock, suma la cantidad si ya está en la orden, incrementa la reserva y recalcula el total. Bajar la cantidad libera reserva; llegar a cero borra el detalle. Pagar descuenta stock y reserva, pasa la orden a PAID y registra un payment con amount, reference, credit_card_id, status y order_id. Cancelar una orden DRAFT libera la reserva y la deja en CANCELLED, conservando el detalle. El pago debe usar una tarjeta válida. Las operaciones van en transacción, con control de concurrencia, errores HTTP claros, DTOs y pruebas de totales, reserva y cancelación.

**Propósito:** Modelar la compra de punta a punta: reserva mientras el carrito está abierto, confirmación al pagar y liberación al cancelar.

**Resultado esperado / Entregable:** Migraciones de orders, order_details y payments, servicios transaccionales, endpoints de carrito, pago y cancelación, y pruebas de inventario.

## 13. Pago con el token de la tarjeta

**Fase / Objetivo:** Ajuste del contrato de pago.

**Prompt utilizado:**

> POST /orders/{id}/pay debe recibir un cuerpo con el token. Busca la tarjeta por token y por el clientId de la orden. Si no existe o no es de ese cliente, responde 404 Credit card Not Found. El resto del pago se mantiene: orden en DRAFT, ítems, vencimiento, rechazo del proveedor, descuento de stock y reserva, orden pagada y payment APPROVED. payments.credit_card_id sigue siendo la llave interna. No cambies el esquema ni la migración de tarjetas, ni la tokenización, el cifrado, el CVV, el JWT ni el API Key. Actualiza la prueba de OrderService y el curl de órdenes, y confirma que un pago con el token de la tokenización encuentra la tarjeta de ese cliente.

**Propósito:** Hacer que el pago viaje con el token público y resuelva por dentro el id de la tarjeta, sin reabrir el diseño de tokenización.

**Resultado esperado / Entregable:** Request de pago por token, consulta por token y cliente, curl actualizado y prueba que recorre tokenización y pago.

## 14. Correo de orden pagada o rechazada

**Fase / Objetivo:** Notificación al cliente.

**Prompt utilizado:**

> Implementa el envío de correo cuando una orden queda pagada o cuando el pago es rechazado, con plantillas distintas. Usa la cuenta personal indicada y su clave de aplicación, sin dejar esa clave escrita en la documentación. Ambos correos informan el estado, la dirección de envío y los productos. En el rechazo, el texto indica que la orden fue cancelada por un error de pago. Toma como referencia la imagen adjunta. Deja el HTML en un paquete de templates e implementa un método genérico de envío reutilizable, con código limpio.

**Propósito:** Avisar el resultado del pago con dos piezas de correo y un único mecanismo de envío.

**Resultado esperado / Entregable:** Dependencia de correo, dos plantillas HTML, servicio genérico de envío y su uso al pagar o al rechazar el pago. Un fallo de correo no debe deshacer la operación de negocio.

## 15. Auditoría de transacciones

**Fase / Objetivo:** Trazabilidad de operaciones.

**Prompt utilizado:**

> Crea transaction_logs con id bigserial, transaction_id UUID, created_at, type, action, module, request_payload y response_payload en JSONB, status SUCCESS o ERROR, y error_message. Indexa transaction_id, type, created_at y campos dentro del JSON. El guardado debe ser asíncrono y en una transacción independiente, para que el log no bloquee la respuesta y sobreviva si el negocio hace rollback. No audites las consultas. Sí audita pagos, creación y actualización de órdenes, y creación y actualización de clientes.

**Propósito:** Dejar evidencia de las mutaciones de negocio sin acoplarla al tiempo de respuesta ni al commit de la operación.

**Resultado esperado / Entregable:** Migración de transaction_logs, servicio asíncrono con transacción nueva, correlación por petición HTTP y registro de las operaciones indicadas.

## 16. Docker y despliegue en Render

**Fase / Objetivo:** Contenedores y hosting de la demo.

**Prompt utilizado:**

> Actúa como ingeniero DevOps y backend. Dockeriza el proyecto Spring Boot para Render en plan gratuito y para ejecución local con Docker Compose. El Dockerfile debe ser multi-stage: compilar el JAR sin tests y ejecutarlo con un JRE ligero, puerto 8080 y la variable PORT. Compose debe levantar la API y PostgreSQL 15, con healthcheck y arranque de la app solo cuando la base esté sana. La URL, el usuario y la contraseña de la base deben salir de variables de entorno, con valores locales por defecto. En producción, Hibernate debe validar o actualizar el esquema. Deja indicado cómo usar la URL interna o externa de Render.

**Propósito:** Poder correr la API y PostgreSQL igual en la máquina local y en el hosting de la demo.

**Resultado esperado / Entregable:** Dockerfile, docker-compose, configuración por variables de entorno y notas de conexión para Render.

## 17. Integración, pruebas y despliegue continuo

**Fase / Objetivo:** CI, pruebas continuas y CD.

**Prompt utilizado:**

> Actúa como ingeniero DevOps y DevSecOps. Crea workflows de GitHub Actions. El de CI y pruebas corre en cada push a main y en cada pull request: compila, ejecuta las pruebas JUnit 5 y exige cobertura JaCoCo del 80 por ciento. Puede incluir un análisis de vulnerabilidades que no bloquee el pipeline. El de despliegue corre solo después de que CI termine bien por un push a main o master, y dispara el Deploy Hook de Render. Documenta en el README el flujo y el secreto RENDER_DEPLOY_HOOK_URL.

**Propósito:** Impedir que llegue a Render un cambio que no compile o que baje de la cobertura pedida.

**Resultado esperado / Entregable:** Workflow de CI/CT, workflow de CD condicionado al éxito anterior, reporte de cobertura y sección del README con el secreto.

## 18. Semilla de 150 productos

**Fase / Objetivo:** Datos de catálogo.

**Prompt utilizado:**

> Genera 150 productos en la tabla de catálogo mediante una migración.

**Propósito:** Poblar el catálogo para probar búsqueda y compra sin carga manual.

**Resultado esperado / Entregable:** Migración Flyway que inserta 150 productos repartidos en las categorías existentes, con precio y stock.

## 19. Descripción y rendimiento de la búsqueda

**Fase / Objetivo:** Evolución del catálogo.

**Prompt utilizado:**

> Escala la tabla product. Agrega description con longitud 70, mapea el campo en las entidades e inclúyelo en el filtro de búsqueda. El motor es de ecommerce, así que la búsqueda debe seguir siendo ágil.

**Propósito:** Ampliar el texto buscable sin cambiar el costo de la consulta por substring.

**Resultado esperado / Entregable:** Columna description, mapeo en Product y búsqueda por nombre, nombre corto o descripción, con el mismo tipo de índice que ya usaban los otros textos.

## 20. Descripciones en la semilla

**Fase / Objetivo:** Datos de la descripción.

**Prompt utilizado:**

> Modifica la migración que creaste y ponle una descripción a cada producto.

**Propósito:** Hacer que los 150 productos de ejemplo también puedan encontrarse por su descripción.

**Resultado esperado / Entregable:** La migración de semilla incluye una descripción de hasta 70 caracteres en cada fila.

## 21. Documentación del proyecto

**Fase / Objetivo:** Documentación funcional y técnica.

**Prompt utilizado:**

> Genera la documentación de todo el proyecto. Incluye arquitectura, tecnologías, diseño y la configuración necesaria de IDE y de base de datos. Déjala en un Markdown dentro del proyecto.

**Propósito:** Dejar una guía única para levantar y entender la prueba técnica.

**Resultado esperado / Entregable:** Un Markdown en la raíz del proyecto con arquitectura, stack, diseño de los módulos, preparación del IDE y creación de la base.

## 22. Despliegue en Google Cloud

**Fase / Objetivo:** Documentación de despliegue administrado.

**Prompt utilizado:**

> Actúa como ingeniero cloud y DevOps de GCP. Aunque la demo usa Render, documenta el despliegue desde cero en Cloud Run y Cloud SQL. El archivo DESPLIEGUE_GCP.md debe cubrir cuenta y proyecto, autenticación de gcloud, habilitación de APIs, creación de PostgreSQL, base y usuario, conexión segura, Artifact Registry, construcción y publicación de la imagen, despliegue en Cloud Run con variables y secretos, puerto 8080, rol Cloud SQL Client, acceso público para Postman y la prueba de GET /ping, que debe responder 200 y pong. Incluye un fragmento de Terraform y un extracto en el README. Lista las variables de entorno necesarias y no dejes contraseñas ni API keys escritas en el código.

**Propósito:** Tener el procedimiento de producción en servicios administrados, separado del hosting gratuito de la demo.

**Resultado esperado / Entregable:** DESPLIEGUE_GCP.md con comandos parametrizados, lista de variables, notas de seguridad, fragmento de Terraform y un extracto en el README.

## 23. Colección de Postman

**Fase / Objetivo:** Pruebas manuales automatizadas.

**Prompt utilizado:**

> Cómo hago una colección de Postman para pruebas automatizadas.

**Propósito:** Ordenar las llamadas de la API en una colección que encadene registro, login, tokenización, carrito y pago, y que valide los códigos de respuesta.

**Resultado esperado / Entregable:** Una guía para armar la colección, con variables de entorno, el token guardado desde el login y pruebas sobre el código HTTP y el cuerpo de cada request.

## 24. Listados de clientes y órdenes

**Fase / Objetivo:** Consultas de las entidades principales.

**Prompt utilizado:**

> Actúa como desarrollador backend. Implementa estos listados: clientes registrados; órdenes con un filtro de estado opcional que, por defecto, traiga todas; y órdenes por cliente, con un DTO que incluya el cliente, la tarjeta utilizada y los productos comprados en esa orden.

**Propósito:** Exponer las consultas que el flujo de compra todavía no tenía, y enriquecer la vista por cliente con la tarjeta y el detalle comprado.

**Resultado esperado / Entregable:** Lista de clientes del usuario autenticado, lista de órdenes filtrable por estado y lista de órdenes de un cliente con cliente, tarjeta y productos. La tarjeta no debe incluir el número completo.
