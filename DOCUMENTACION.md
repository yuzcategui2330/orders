# Orders

API de comercio para Farmatodo: registro de clientes, catálogo, tokenización de tarjetas, carrito de compras, pago y auditoría. El esquema lo crea Flyway. Hibernate no genera tablas.

## Tecnologías

| Pieza | Versión / detalle |
| --- | --- |
| Java | 21 (toolchain de Gradle) |
| Spring Boot | 4.1.1 |
| Gradle | 9.7.1, wrapper incluido |
| PostgreSQL | 15 |
| Persistencia | Spring Data JPA, Hibernate, `ddl-auto: none` |
| Migraciones | Flyway |
| Seguridad | Spring Security, sesión stateless, JWT (jjwt 0.12.6), BCrypt |
| Correo | Spring Mail, SMTP de Gmail (587, STARTTLS) |
| Métricas | Actuator y Micrometer Prometheus |
| Pruebas | JUnit 5, Mockito, H2 en tests de repositorio |
| Cobertura | JaCoCo 0.8.12, mínimo 80 % de líneas en `check` |
| Contenedores | Docker multi-stage, Compose |
| CI/CD | GitHub Actions y Deploy Hook de Render |

## Arquitectura

Aplicación monolítica por capas, paquete `com.farmatodo.order`. Un solo proceso Spring Boot atiende HTTP, persiste en PostgreSQL y habla con Gmail por SMTP.

```mermaid
flowchart LR
  cliente["Cliente HTTP<br/>Postman"]

  subgraph app["Web Service · Docker"]
    direction TB
    ping["GET /ping"]
    apiKey["Filtro X-API-Key<br/>POST /api/v1/tokens"]
    jwt["Filtro JWT Bearer<br/>clientes, órdenes, cambio de clave"]
    publico["Rutas públicas<br/>login, registro, búsqueda"]
    controllers["Controllers REST JSON"]
    services["Services<br/>auth, catálogo, tokenización, órdenes"]
    async["Hilos async<br/>search_logs · transaction_logs · correo"]
    repos["Repositories JPA"]
  end

  pg[("PostgreSQL<br/>JDBC")]
  gmail["Gmail SMTP<br/>587 STARTTLS"]
  gh["GitHub Actions"]
  render["Render Deploy Hook"]

  cliente -->|"HTTPS JSON"| ping
  cliente -->|"HTTPS + X-API-Key"| apiKey
  cliente -->|"HTTPS + Bearer"| jwt
  cliente -->|"HTTPS"| publico
  apiKey --> controllers
  jwt --> controllers
  publico --> controllers
  ping --> controllers
  controllers --> services
  services --> repos
  services --> async
  repos -->|"SQL"| pg
  async -->|"SQL"| pg
  async -->|"SMTP"| gmail
  gh -->|"HTTPS POST"| render
  render -->|"reconstruye"| app
```

Flyway crea el esquema al arrancar. Hibernate valida en el perfil `prod` y no genera tablas. El cifrado de la tarjeta (AES-GCM) y la firma del JWT ocurren dentro del proceso; no hay un proveedor de pagos externo. El rechazo de token y de pago es `RejectionGate`, un azar configurable.

| Paquete | Responsabilidad |
| --- | --- |
| `config` | Seguridad, JWT, API key, async, propiedades |
| `controller` | HTTP. Devuelve `ResponseEntity` |
| `domains` | Entidades JPA y enums |
| `domains.request` / `domains.response` | Contratos de entrada y salida |
| `services` | Reglas de negocio, correo, auditoría y cifrado |
| `repositories` | Spring Data JPA |
| `exceptions` | `ApiException` y `GlobalExceptionHandler` |
| `templates` | Enum de plantillas y HTML en `resources` |

Los errores de negocio salen como `{"message":"..."}` con el HTTP status de `ApiException`.

JSON y columnas usan snake_case (`created_at`, `client_id`). En Java los campos van en camelCase.

## Diseño funcional

### Identidad

- `POST /users` crea un usuario. La contraseña se guarda con BCrypt y no se devuelve.
- Política de clave: más de 8 caracteres, al menos una letra y un dígito.
- `is_active` nace en `true`.
- Login pide solo `username` y `password`.
- El access token dura 15 minutos y el refresh 7 días. El refresh se guarda como hash SHA-256 y rota en cada uso.
- Cambiar la contraseña toma el usuario del access token y revoca sus refresh tokens.

### Clientes

`POST /clients/make-registration` es público y crea usuario y cliente en una transacción. El resto de `/clients` exige JWT y solo ve clientes de ese usuario.

### Catálogo

Categorías: `MEDICINES`, `HYGYENE`, `PERSONAL_CARE`.

`GET /products/search` es público. Busca en `name`, `short_name` y `description` con `LIKE` escapado. Solo entran productos cuyo stock disponible (`stock_quantity - reserved_qty`) es mayor que `products.search.min-stock` (variable `MIN_STOCK_THRESHOLD`, default 0). La página por defecto es 0 y el tamaño 20, con máximo 100.

En PostgreSQL hay índices GIN `pg_trgm` sobre `lower(name)`, `lower(short_name)` y `lower(description)` para que la búsqueda por substring no recorra la tabla. Cada búsqueda exitosa se escribe en `search_logs` en otro hilo. Si el log falla, la respuesta de búsqueda sigue.

La migración `V8` carga 150 productos con descripción.

### Tarjetas

`POST /api/v1/tokens` no usa el JWT. Exige el header `X-API-Key`. Valida Luhn, vencimiento y CVV (3 o 4 dígitos). El CVV no se persiste. El PAN se guarda cifrado con AES-GCM. La respuesta es solo el token UUID.

`tokenization.rejection-rate` (`TOKEN_REJECTION_RATE`, default 0) simula un rechazo del proveedor antes de guardar. Si el número aleatorio cae bajo esa tasa, responde 422 `Tokenization rejected by the provider`.

### Órdenes y pago

Una orden nace en `DRAFT`. Agregar un ítem reserva stock (`reserved_qty`). Si el producto ya está en la orden, se suma la cantidad y solo se reserva el delta. Actualizar fija la cantidad absoluta: bajarla libera reserva y dejarla en 0 borra la línea. Cancelar una orden `DRAFT` libera toda la reserva y pasa a `CANCELLED`.

`GET /orders` lista las órdenes de los clientes del usuario autenticado. `status` es opcional (`DRAFT`, `PAID`, `CANCELLED`); si no viene, devuelve todas. Un valor desconocido responde 400 `Invalid status`.

`GET /clients/{id}/orders` lista las órdenes de ese cliente. Cada elemento trae el cliente, la tarjeta usada en el pago (token, titular y vencimiento, sin el PAN) y los productos de la orden con cantidad y monto. Si la orden no está pagada, `credit_card` es `null`.

`POST /orders/{id}/pay` recibe `{ "token": "<uuid>" }`. La tarjeta debe existir, pertenecer al cliente de la orden y no estar vencida. Si el proveedor rechaza el pago, la orden sigue en `DRAFT`, la reserva no cambia y no se crea fila en `payments`. Si el pago procede, se descuenta stock y reserva, la orden pasa a `PAID` y se guarda un pago `APPROVED` con el id interno de la tarjeta.

Las mutaciones de orden bloquean la fila de la orden y luego los productos (`PESSIMISTIC_WRITE`).

### Correo

Al quedar pagada se envía `order-paid.html`. Si el proveedor rechaza el pago se envía `order-payment-rejected.html`. Ambas incluyen estado, dirección y productos. El envío genérico es `EmailService.send`. Un fallo de SMTP no revierte el pago ni cambia el 422.

### Auditoría

`transaction_logs` registra creación y actualización de clientes, creación y actualización de órdenes (ítems y cancelación) y el pago. Las consultas GET no se auditan. Cada request HTTP comparte un `transaction_id`. El insert corre en otro hilo y en una transacción `REQUIRES_NEW`, así el log de un error queda aunque el negocio haga rollback. El de éxito se encola después del commit. La contraseña se quita del JSON antes de guardarlo.

## API

Base local sin Docker: `http://localhost:8130`. Con Compose: `http://localhost:8080`.

Ejemplos listos para PowerShell en `http/*.curl`.

| Método | Ruta | Acceso |
| --- | --- | --- |
| POST | `/users` | Público |
| PUT | `/users/{id}` | Público |
| POST | `/auth/login` | Público |
| POST | `/auth/refresh-token` | Público |
| POST | `/auth/change-password` | JWT |
| POST | `/clients/make-registration` | Público |
| PUT | `/clients/{id}` | JWT, dueño |
| GET | `/clients/{id}` | JWT, dueño |
| GET | `/clients` | JWT. Clientes registrados del usuario |
| GET | `/clients/{id}/orders` | JWT, dueño. Cliente, tarjeta y productos |
| GET | `/ping` | Público. Responde `pong` |
| GET | `/products/search?query=&page=&size=` | Público |
| POST | `/api/v1/tokens` | Header `X-API-Key` |
| POST | `/orders` | JWT |
| GET | `/orders?status=` | JWT. `status` opcional; sin filtro trae todas |
| GET | `/orders/{id}` | JWT, dueño |
| POST | `/orders/{id}/items` | JWT |
| PUT | `/orders/{id}/items` | JWT |
| DELETE | `/orders/{id}/items/{productId}` | JWT |
| POST | `/orders/{id}/pay` | JWT, cuerpo `{ "token" }` |
| POST | `/orders/{id}/cancel` | JWT |

## Modelo de datos

Flyway aplica `src/main/resources/db/migration` en orden.

| Versión | Contenido |
| --- | --- |
| V1 | `users`, `refresh_tokens` |
| V2 | `clients` |
| V3 | `products`, extensión `pg_trgm` e índices de búsqueda |
| V4 | `search_logs` |
| V5 | `credit_cards` (PAN cifrado, sin CVV) |
| V6 | `price` y `reserved_qty` en productos; `orders`, `order_details`, `payments` |
| V7 | `transaction_logs` |
| V8 | Columna `description` y 150 productos de catálogo |
| V9 | Índice GIN de `description` |

`description` es `VARCHAR(70)` y puede ser nula. `reserved_qty` no puede superar `stock_quantity`.

## Entorno de desarrollo

### IDE

Sirve IntelliJ IDEA o Cursor / VS Code con soporte Java.

1. Instalar JDK 21 (Temurin). El proyecto no compila en 17: el toolchain está fijado en 21.
2. Abrir la carpeta del proyecto. Gradle importa solo con `gradlew`.
3. Activar el plugin de Lombok y el annotation processing. Sin eso los getters no resuelven en el IDE.
4. Codificación UTF-8.
5. Clase principal: `com.farmatodo.order.OrderApplication`.
6. No hace falta un perfil de Spring para desarrollar. El puerto local es 8130.

Comandos:

```text
gradlew.bat bootRun
gradlew.bat test
gradlew.bat check
```

`check` corre los tests y falla si JaCoCo baja de 80 % de líneas. El reporte HTML queda en `build/reports/jacoco/test/html`.

### Base de datos local

PostgreSQL 15 en `localhost:5432`. Usuario `postgres`, clave `Post12.gres`. La base se llama `order`. Como `ORDER` es palabra reservada, créala entre comillas:

```sql
CREATE DATABASE "order";
```

No crees las tablas a mano y no uses `ddl-auto` en `update`. Al arrancar, Flyway ejecuta V1 a V9, incluida la extensión `pg_trgm` y el catálogo. El usuario de PostgreSQL debe poder crear extensiones. En una instalación local el rol `postgres` ya puede.

Si una migración ya aplicada se edita, Flyway rechaza el checksum. En desarrollo se recrea la base:

```sql
DROP DATABASE IF EXISTS "order";
CREATE DATABASE "order";
```

La URL por defecto es `jdbc:postgresql://localhost:5432/order`.

### Docker Compose

```text
docker compose up --build
```

Levanta PostgreSQL 15 Alpine y la API en el puerto 8080. La app espera el healthcheck de la base. Dentro de la red el host de la base es `postgres`, no `localhost`.

## Configuración

Los valores locales viven en `src/main/resources/application.yaml`. En producción, `SPRING_PROFILES_ACTIVE=prod` carga `application-prod.yaml`: Hibernate pasa a `validate` y base, API key, cifrado y secreto JWT dejan de tener default. Hay que definirlos en el entorno.

| Variable | Uso | Default local |
| --- | --- | --- |
| `PORT` | Puerto HTTP. Render lo inyecta | `8130` |
| `SPRING_DATASOURCE_URL` | JDBC | `jdbc:postgresql://localhost:5432/order` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Clave de la base | `Post12.gres` |
| `MIN_STOCK_THRESHOLD` | Stock mínimo de la búsqueda | `0` |
| `TOKENIZATION_API_KEY` | Header `X-API-Key` | `local-dev-tokenization-key` |
| `TOKEN_REJECTION_RATE` | Tasa de rechazo simulado, de 0 a 1 | `0.0` |
| `TOKENIZATION_ENCRYPTION_KEY` | Clave de la que se deriva AES-GCM | valor local del yaml |
| `SECURITY_JWT_SECRET` | HMAC del JWT. Obligatorio con perfil `prod` | valor local del yaml |
| `SPRING_PROFILES_ACTIVE` | `prod` en Render | vacío |

El correo local usa SMTP de Gmail con la cuenta y la clave de aplicación definidas en `spring.mail`. En un despliegue real esas dos propiedades se sustituyen por variables de entorno y no se versionan.

Flyway reutiliza la URL, el usuario y la clave del datasource si no se definen `SPRING_FLYWAY_URL`, `SPRING_FLYWAY_USER` y `SPRING_FLYWAY_PASSWORD`.

## Despliegue

La imagen usa JDK 21 para compilar (`bootJar -x test`) y JRE 21 Alpine para correr. Escucha `$PORT` (8080 si no viene definido) y limita el heap al 75 % de la RAM del contenedor.

En Render, API y PostgreSQL van en la misma cuenta y región. En el Web Service:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/DB
SPRING_DATASOURCE_USERNAME=USER
SPRING_DATASOURCE_PASSWORD=PASSWORD
TOKENIZATION_API_KEY=...
TOKENIZATION_ENCRYPTION_KEY=...
SECURITY_JWT_SECRET=...
```

Usa `INTERNAL_DB_URL`. Render la entrega como `postgres://USER:PASSWORD@HOST:5432/DB`. Hay que pasarla a JDBC y separar usuario y clave. `EXTERNAL_DB_URL` es para conectar desde tu máquina y suele exigir `?sslmode=require`. No definas `PORT`.

### GitHub Actions

`ci-ct-pipeline.yml` corre en cada push a `main` y en cada pull request: compila, prueba contra PostgreSQL 15, exige JaCoCo al 80 % y publica el reporte. Trivy informa vulnerabilidades altas y críticas y no bloquea el pipeline.

`cd-pipeline.yml` hace POST al Deploy Hook de Render solo si CI/CT terminó bien por un push a `main` o `master`. El secret del repositorio es `RENDER_DEPLOY_HOOK_URL`.
