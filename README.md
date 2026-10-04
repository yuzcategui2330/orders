# orders

Desafio tecnico Farmatodo- Generación de ordenes, pagos con tdc, gestion de clientes

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
