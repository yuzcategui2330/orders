# orders

Desafio tecnico Farmatodo- Generación de ordenes, pagos con tdc, gestion de clientes

La documentación de arquitectura, tecnologías, diseño y configuración de IDE y base de datos está en [DOCUMENTACION.md](DOCUMENTACION.md).

## CI, pruebas y despliegue

Hay dos workflows en `.github/workflows/`.

**CI/CT** (`ci-ct-pipeline.yml`) corre en cada push a `main` y en cada pull request. Compila con JDK 21 (Temurin), el toolchain del proyecto. Ejecuta `./gradlew test` (JUnit 5) contra un PostgreSQL 15 de servicio y después `jacocoTestCoverageVerification`. JaCoCo publica el reporte HTML/XML como artefacto `jacoco-report` y el build falla si la cobertura de líneas queda por debajo del 80%. Trivy revisa vulnerabilidades altas y críticas de las dependencias; ese paso informa y no bloquea el pipeline.

**CD** (`cd-pipeline.yml`) arranca solo cuando CI/CT termina en éxito por un push a `main` o `master`. Un pull request no despliega. El job hace `POST` al Deploy Hook de Render, que reconstruye el contenedor Docker del Web Service.

### Secret en GitHub

En el repositorio: Settings, Secrets and variables, Actions, New repository secret.

| Secret | Uso |
| --- | --- |
| `RENDER_DEPLOY_HOOK_URL` | URL del Deploy Hook del Web Service en Render (Settings, Deploy Hook). |

No hace falta commitear esa URL. Render inyecta `PORT` y las variables de base de datos se configuran en el servicio, no en GitHub.

## Despliegue en Google Cloud

La demo usa Render. El paso a paso de Cloud Run y Cloud SQL está en [DESPLIEGUE_GCP.md](DESPLIEGUE_GCP.md): proyecto, APIs, PostgreSQL 15 (`farmatodo_db`), Artifact Registry, imagen y servicio público para Postman.

Cloud Run usa el perfil `prod`. Estas variables salen de Secret Manager o de la configuración del servicio. No van en el repositorio.

| Variable | Origen |
| --- | --- |
| `PORT` | Cloud Run, puerto `8080` |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://IP_PRIVADA:5432/farmatodo_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de Cloud SQL |
| `SPRING_DATASOURCE_PASSWORD` | Secreto |
| `TOKENIZATION_API_KEY` | Secreto. Header `X-API-Key` |
| `TOKENIZATION_ENCRYPTION_KEY` | Secreto AES de la tarjeta |
| `SECURITY_JWT_SECRET` | Secreto HMAC del JWT |
| `TOKEN_REJECTION_RATE` | Opcional. Default `0.0` |
| `MIN_STOCK_THRESHOLD` | Opcional. Default `0` |
| `SPRING_MAIL_USERNAME` | Opcional. Cuenta SMTP |
| `SPRING_MAIL_PASSWORD` | Opcional. Secreto SMTP |

`GET /ping` responde HTTP 200 y el cuerpo `pong`.
