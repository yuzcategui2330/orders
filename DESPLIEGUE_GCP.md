# Despliegue en Google Cloud Platform

La demo corre en Render. Este documento despliega la misma imagen (Spring Boot 21 y PostgreSQL 15) en **Cloud Run** y **Cloud SQL**, sin servidores que administrar.

La aplicación ya lee el puerto y la base de datos desde variables de entorno. El perfil `prod` (`src/main/resources/application-prod.yaml`) exige URL, usuario y contraseña de PostgreSQL, la API key de tokenización, la clave AES y el secreto JWT. Flyway crea el esquema al arrancar. La base de Cloud SQL se llama `farmatodo_db`. En local y en Docker Compose la base sigue llamándose `order`.

No guardes contraseñas, API keys ni el secreto JWT en el repositorio, en el Dockerfile ni en la línea de comandos del historial. Créalos en Secret Manager y referencia el secreto desde Cloud Run.

## Variables de esta guía

Sustituye los valores antes de ejecutar los bloques. El resto de los comandos usa estas variables.

```bash
PROJECT_ID="mi-proyecto-gcp"
REGION="us-central1"
INSTANCE_NAME="farmatodo-pg"
DB_NAME="farmatodo_db"
DB_USER="orders_app"
REPO_NAME="orders"
SERVICE_NAME="orders"
RUNTIME_SA_NAME="orders-run"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}/order-api:1.0.0"
INSTANCE_CONNECTION_NAME="${PROJECT_ID}:${REGION}:${INSTANCE_NAME}"
```

Genera las contraseñas y las claves en la terminal. No las pegues en un archivo del proyecto.

```bash
SQL_ROOT_PASSWORD="$(openssl rand -base64 24)"
SQL_APP_PASSWORD="$(openssl rand -base64 24)"
TOKENIZATION_API_KEY="$(openssl rand -base64 32)"
TOKENIZATION_ENCRYPTION_KEY="$(openssl rand -base64 32)"
SECURITY_JWT_SECRET="$(openssl rand -base64 48)"
```

`TOKENIZATION_ENCRYPTION_KEY` cifra el PAN en reposo. Si la cambias, las tarjetas ya guardadas no se pueden descifrar. `SECURITY_JWT_SECRET` firma los access token; al rotarlo, las sesiones activas dejan de ser válidas.

## Prerrequisitos

1. Una cuenta de Google Cloud con facturación activa. Cloud SQL no entra en el nivel gratuito de Cloud Run.
2. [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) instalada.
3. Docker, solo si vas a construir la imagen en tu máquina. `gcloud builds submit` no lo necesita.
4. El código de este repositorio, con el `Dockerfile` de la raíz.

Crea el proyecto si todavía no existe y selecciónalo.

```bash
gcloud projects create "${PROJECT_ID}" --name="Farmatodo Orders"
gcloud config set project "${PROJECT_ID}"
gcloud auth login
gcloud auth application-default login
```

`gcloud projects create` falla si el ID ya está ocupado en GCP. En ese caso elige otro `PROJECT_ID` y vuelve a exportar las variables que dependen de él (`IMAGE`, `INSTANCE_CONNECTION_NAME`).

Vincula la facturación (sustituye el ID de la cuenta de facturación):

```bash
gcloud billing projects link "${PROJECT_ID}" --billing-account="BILLING_ACCOUNT_ID"
```

Habilita las APIs. Service Networking y Secret Manager entran en el camino de IP privada y de secretos.

```bash
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  sqladmin.googleapis.com \
  servicenetworking.googleapis.com \
  secretmanager.googleapis.com \
  vpcaccess.googleapis.com
```

## Variables de entorno de la aplicación

Cloud Run inyecta `PORT`. El proceso escucha ese valor (`server.port: ${PORT:8130}`). En el contenedor el valor es `8080`.

| Variable | Obligatoria en `prod` | Uso |
| --- | --- | --- |
| `PORT` | La fija Cloud Run | Puerto HTTP. No la definas a mano si el servicio ya declara `--port=8080`. |
| `SPRING_PROFILES_ACTIVE` | Sí | `prod`. Activa `ddl-auto: validate` y exige el resto de secretos. |
| `SPRING_DATASOURCE_URL` | Sí | JDBC. Con IP privada: `jdbc:postgresql://IP:5432/farmatodo_db`. |
| `SPRING_DATASOURCE_USERNAME` | Sí | Usuario de Cloud SQL. En esta guía, `orders_app`. |
| `SPRING_DATASOURCE_PASSWORD` | Sí | Contraseña de ese usuario. Solo en Secret Manager. |
| `TOKENIZATION_API_KEY` | Sí | Valor del header `X-API-Key` en `POST /api/v1/tokens`. |
| `TOKENIZATION_ENCRYPTION_KEY` | Sí | Clave AES-GCM de la tarjeta. |
| `SECURITY_JWT_SECRET` | Sí | Secreto HMAC del JWT. |
| `TOKEN_REJECTION_RATE` | No | Probabilidad de rechazo simulado. Default `0.0`. |
| `MIN_STOCK_THRESHOLD` | No | Stock mínimo de la búsqueda. Default `0`. |
| `SPRING_MAIL_USERNAME` | No | Cuenta SMTP. Si no se define, el contenedor usa el valor de desarrollo embebido en `application.yaml`. |
| `SPRING_MAIL_PASSWORD` | No | Contraseña de aplicación SMTP. Defínela en Secret Manager para no enviar correo con la cuenta de desarrollo. |
| `SPRING_FLYWAY_URL` | No | En `prod` Flyway reutiliza `SPRING_DATASOURCE_URL`. |
| `SPRING_FLYWAY_USER` | No | En `prod` Flyway reutiliza `SPRING_DATASOURCE_USERNAME`. |
| `SPRING_FLYWAY_PASSWORD` | No | En `prod` Flyway reutiliza `SPRING_DATASOURCE_PASSWORD`. |

Flyway ejecuta `CREATE EXTENSION pg_trgm` (migración V3). En Cloud SQL esa sentencia la puede correr un usuario con el rol `cloudsqlsuperuser`. Esta guía se lo concede a `orders_app` para que el arranque aplique las migraciones. En un entorno real, las migraciones las corre un usuario distinto al de la API.

## Paso 1. Cloud SQL para PostgreSQL

### 1.1 Acceso privado en la VPC `default`

Una sola vez por proyecto y por VPC. Reserva un rango y emparéjalo con Service Networking. Puede tardar varios minutos.

```bash
gcloud compute addresses create google-managed-services-default \
  --global \
  --purpose=VPC_PEERING \
  --prefix-length=16 \
  --network=default \
  --project="${PROJECT_ID}"

gcloud services vpc-peerings connect \
  --service=servicenetworking.googleapis.com \
  --ranges=google-managed-services-default \
  --network=default \
  --project="${PROJECT_ID}"
```

### 1.2 Instancia

PostgreSQL 15, edición Enterprise, 1 vCPU y 3.75 GB. Sin IP pública: Cloud Run no tiene una IP de salida fija, así que una lista de redes autorizadas no sirve para el servicio.

```bash
gcloud sql instances create "${INSTANCE_NAME}" \
  --project="${PROJECT_ID}" \
  --database-version=POSTGRES_15 \
  --edition=ENTERPRISE \
  --tier=db-custom-1-3840 \
  --region="${REGION}" \
  --storage-size=10 \
  --storage-type=SSD \
  --storage-auto-increase \
  --availability-type=ZONAL \
  --network="projects/${PROJECT_ID}/global/networks/default" \
  --no-assign-ip \
  --root-password="${SQL_ROOT_PASSWORD}"
```

La creación suele tardar entre 5 y 15 minutos.

### 1.3 Base de datos y usuario

```bash
gcloud sql databases create "${DB_NAME}" \
  --instance="${INSTANCE_NAME}" \
  --project="${PROJECT_ID}"

gcloud sql users create "${DB_USER}" \
  --instance="${INSTANCE_NAME}" \
  --project="${PROJECT_ID}" \
  --password="${SQL_APP_PASSWORD}"
```

Abre una consola como `postgres` y concede el rol que Flyway necesita para `pg_trgm`. La contraseña no queda escrita en el archivo SQL.

```bash
gcloud sql connect "${INSTANCE_NAME}" \
  --project="${PROJECT_ID}" \
  --user=postgres \
  --database="${DB_NAME}"
```

Dentro de `psql`:

```sql
GRANT cloudsqlsuperuser TO orders_app;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
\q
```

`gcloud sql connect` usa el Cloud SQL Auth Proxy por debajo y pide la contraseña de `postgres` (`SQL_ROOT_PASSWORD`). Instala el componente si falta: `gcloud components install cloud-sql-proxy`.

### 1.4 IP privada

```bash
PRIVATE_IP="$(gcloud sql instances describe "${INSTANCE_NAME}" \
  --project="${PROJECT_ID}" \
  --format=json \
  | python3 -c "import json,sys; ips=json.load(sys.stdin)['ipAddresses']; print(next(i['ipAddress'] for i in ips if i['type']=='PRIVATE'))")"

echo "${PRIVATE_IP}"
```

La URL que recibirá Cloud Run:

```bash
SPRING_DATASOURCE_URL="jdbc:postgresql://${PRIVATE_IP}:5432/${DB_NAME}"
```

El puerto 5432 no se publica en Internet. Solo las cargas de la VPC `default` (Cloud Run con Direct VPC egress) llegan a esa IP.

## Paso 2. Artifact Registry

```bash
gcloud artifacts repositories create "${REPO_NAME}" \
  --project="${PROJECT_ID}" \
  --repository-format=docker \
  --location="${REGION}" \
  --description="Imagenes de la API de ordenes"

gcloud auth configure-docker "${REGION}-docker.pkg.dev"
```

Permite que Cloud Build publique en el repositorio. En proyectos nuevos el build corre con la cuenta de Compute Engine.

```bash
PROJECT_NUMBER="$(gcloud projects describe "${PROJECT_ID}" --format='value(projectNumber)')"

gcloud artifacts repositories add-iam-policy-binding "${REPO_NAME}" \
  --project="${PROJECT_ID}" \
  --location="${REGION}" \
  --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"
```

## Paso 3. Imagen

Desde la raíz del repositorio (donde está el `Dockerfile`). Cloud Build compila con el JDK 21 del Dockerfile y sube la etiqueta.

```bash
gcloud builds submit \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --tag="${IMAGE}"
```

Equivalente en local, si ya autenticaste Docker:

```bash
docker build --tag "${IMAGE}" .
docker push "${IMAGE}"
```

La imagen no contiene `SPRING_DATASOURCE_PASSWORD`, la API key ni el JWT. Esos valores entran en el paso 4.

## Paso 4. Cloud Run

### 4.1 Cuenta de servicio

```bash
gcloud iam service-accounts create "${RUNTIME_SA_NAME}" \
  --project="${PROJECT_ID}" \
  --display-name="Cloud Run orders"

RUNTIME_SA="${RUNTIME_SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${RUNTIME_SA}" \
  --role="roles/cloudsql.client"
```

`roles/cloudsql.client` es el permiso del Auth Proxy y del conector JDBC. Con IP privada la conexión es TCP directo y ese rol no abre el puerto por sí solo; se asigna igual para la alternativa del proxy y para no ampliar la cuenta más adelante.

### 4.2 Secretos

```bash
printf '%s' "${SQL_APP_PASSWORD}" | gcloud secrets create orders-db-password --data-file=- --project="${PROJECT_ID}"
printf '%s' "${TOKENIZATION_API_KEY}" | gcloud secrets create orders-tokenization-api-key --data-file=- --project="${PROJECT_ID}"
printf '%s' "${TOKENIZATION_ENCRYPTION_KEY}" | gcloud secrets create orders-tokenization-encryption-key --data-file=- --project="${PROJECT_ID}"
printf '%s' "${SECURITY_JWT_SECRET}" | gcloud secrets create orders-jwt-secret --data-file=- --project="${PROJECT_ID}"
```

Si también vas a enviar correo, crea `orders-mail-password` con la contraseña de aplicación SMTP. No reutilices la de desarrollo ni la dejes en el repositorio.

```bash
for SECRET in orders-db-password orders-tokenization-api-key orders-tokenization-encryption-key orders-jwt-secret
do
  gcloud secrets add-iam-policy-binding "${SECRET}" \
    --project="${PROJECT_ID}" \
    --member="serviceAccount:${RUNTIME_SA}" \
    --role="roles/secretmanager.secretAccessor"
done
```

### 4.3 Despliegue

`--network` y `--subnet` enganchan el contenedor a la VPC donde vive la IP privada. `--vpc-egress=private-ranges-only` manda el tráfico interno (la IP de Cloud SQL) por esa VPC y deja el resto por Internet. `--allow-unauthenticated` abre el servicio para Postman. Login, órdenes y tokenización siguen exigiendo JWT o `X-API-Key`.

```bash
gcloud run deploy "${SERVICE_NAME}" \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --image="${IMAGE}" \
  --service-account="${RUNTIME_SA}" \
  --port=8080 \
  --memory=1Gi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=2 \
  --timeout=300 \
  --network=default \
  --subnet=default \
  --vpc-egress=private-ranges-only \
  --allow-unauthenticated \
  --set-env-vars="^||^SPRING_PROFILES_ACTIVE=prod||SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}||SPRING_DATASOURCE_USERNAME=${DB_USER}||TOKEN_REJECTION_RATE=0.0||MIN_STOCK_THRESHOLD=0" \
  --set-secrets="SPRING_DATASOURCE_PASSWORD=orders-db-password:latest,TOKENIZATION_API_KEY=orders-tokenization-api-key:latest,TOKENIZATION_ENCRYPTION_KEY=orders-tokenization-encryption-key:latest,SECURITY_JWT_SECRET=orders-jwt-secret:latest"
```

El separador `^||^` evita que `gcloud` parta la URL JDBC si más adelante le agregas parámetros. El primer arranque aplica Flyway y puede acercarse al timeout de 300 segundos.

Correo, solo si creaste el secreto. Sustituye el usuario SMTP. No pongas la contraseña en `--set-env-vars`.

```bash
gcloud run services update "${SERVICE_NAME}" \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --update-env-vars="^||^SPRING_MAIL_HOST=smtp.gmail.com||SPRING_MAIL_PORT=587||SPRING_MAIL_USERNAME=tu-cuenta@gmail.com" \
  --update-secrets="SPRING_MAIL_PASSWORD=orders-mail-password:latest"
```

## Paso 5. Verificación

```bash
SERVICE_URL="$(gcloud run services describe "${SERVICE_NAME}" \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --format='value(status.url)')"

echo "${SERVICE_URL}"
curl --show-error --fail-with-body "${SERVICE_URL}/ping"
```

La respuesta es HTTP 200 y el cuerpo `pong`.

Si el contenedor no arranca, revisa el log de Flyway y de la conexión:

```bash
gcloud run services logs read "${SERVICE_NAME}" \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --limit=50
```

Un `Connection refused` o un timeout hacia la IP privada indica que el servicio no está en la VPC `default` o que la instancia no tiene IP privada. Un error `permission denied to create extension "pg_trgm"` indica que falta el `GRANT cloudsqlsuperuser` del paso 1.3.

## Alternativa: Cloud SQL Auth Proxy en Cloud Run

Cloud Run puede montar el proxy con `--add-cloudsql-instances` y un socket Unix en `/cloudsql/PROYECTO:REGION:INSTANCIA`. El driver PostgreSQL 42.7 que trae esta aplicación no abre ese socket. Para usar el proxy hay que agregar la librería antes de construir la imagen:

```kotlin
implementation("com.google.cloud.sql:postgres-socket-factory:1.28.4")
```

URL correspondiente (usuario y contraseña siguen en variables, no dentro de la URL):

```text
jdbc:postgresql:///${DB_NAME}?cloudSqlInstance=${INSTANCE_CONNECTION_NAME}&socketFactory=com.google.cloud.sql.postgres.SocketFactory&ipTypes=PRIVATE
```

El deploy cambia la red por el conector integrado y mantiene el rol `roles/cloudsql.client`:

```bash
gcloud run deploy "${SERVICE_NAME}" \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --image="${IMAGE}" \
  --service-account="${RUNTIME_SA}" \
  --port=8080 \
  --allow-unauthenticated \
  --add-cloudsql-instances="${INSTANCE_CONNECTION_NAME}" \
  --set-env-vars="^||^SPRING_PROFILES_ACTIVE=prod||SPRING_DATASOURCE_URL=jdbc:postgresql:///${DB_NAME}?cloudSqlInstance=${INSTANCE_CONNECTION_NAME}&socketFactory=com.google.cloud.sql.postgres.SocketFactory&ipTypes=PRIVATE||SPRING_DATASOURCE_USERNAME=${DB_USER}" \
  --set-secrets="SPRING_DATASOURCE_PASSWORD=orders-db-password:latest,TOKENIZATION_API_KEY=orders-tokenization-api-key:latest,TOKENIZATION_ENCRYPTION_KEY=orders-tokenization-encryption-key:latest,SECURITY_JWT_SECRET=orders-jwt-secret:latest"
```

Sin esa dependencia, usa el paso 4 (IP privada y Direct VPC egress).

## Alternativa: IP pública restringida

Sirve para conectar un cliente con IP fija (tu laptop o Cloud SQL Auth Proxy local). No sirve como camino de Cloud Run: el egress del servicio no es una IP estable salvo que montes Cloud NAT.

```bash
gcloud sql instances patch "${INSTANCE_NAME}" \
  --project="${PROJECT_ID}" \
  --assign-ip \
  --authorized-networks="TU_IP_PUBLICA/32"
```

Quita la red cuando termines la prueba: `--clear-authorized-networks`. La contraseña sigue fuera del código.

## Terraform

Fragmento de referencia. No es un módulo completo: no crea el peering de Service Networking ni los secretos. Las contraseñas salen de variables, no del código.

```hcl
variable "project_id" {}
variable "region" { default = "us-central1" }
variable "db_password" { sensitive = true }

resource "google_sql_database_instance" "orders" {
  name             = "farmatodo-pg"
  project          = var.project_id
  region           = var.region
  database_version = "POSTGRES_15"

  settings {
    edition = "ENTERPRISE"
    tier    = "db-custom-1-3840"

    ip_configuration {
      ipv4_enabled    = false
      private_network = "projects/${var.project_id}/global/networks/default"
    }
  }

  deletion_protection = true
}

resource "google_sql_database" "app" {
  name     = "farmatodo_db"
  project  = var.project_id
  instance = google_sql_database_instance.orders.name
}

resource "google_sql_user" "app" {
  name     = "orders_app"
  project  = var.project_id
  instance = google_sql_database_instance.orders.name
  password = var.db_password
}

resource "google_cloud_run_v2_service" "api" {
  name     = "orders"
  project  = var.project_id
  location = var.region

  template {
    service_account = "orders-run@${var.project_id}.iam.gserviceaccount.com"

    vpc_access {
      egress = "PRIVATE_RANGES_ONLY"
      network_interfaces {
        network    = "default"
        subnetwork = "default"
      }
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/orders/order-api:1.0.0"

      ports {
        container_port = 8080
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }

      env {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${google_sql_database_instance.orders.private_ip_address}:5432/farmatodo_db"
      }

      env {
        name = "SPRING_DATASOURCE_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = "orders-db-password"
            version = "latest"
          }
        }
      }
    }
  }
}
```

Guarda `db_password` en `terraform.tfvars` y no lo subas a git. El mismo patrón de `secret_key_ref` aplica a `TOKENIZATION_API_KEY`, `TOKENIZATION_ENCRYPTION_KEY` y `SECURITY_JWT_SECRET`.

## Notas de seguridad

- Secret Manager es el único lugar de la contraseña de base de datos, la API key, la clave AES, el JWT y la contraseña SMTP.
- `--allow-unauthenticated` deja entrar a Postman. No apaga JWT ni `X-API-Key` en las rutas que ya los exigen.
- La instancia de esta guía no tiene IP pública. No abras `0.0.0.0/0` en redes autorizadas.
- Rota un secreto creando una versión nueva (`gcloud secrets versions add`) y desplegando de nuevo. No edites la versión `latest` a mano dentro de la imagen.
- Borra las variables `SQL_ROOT_PASSWORD` y `SQL_APP_PASSWORD` de la sesión al terminar: `unset SQL_ROOT_PASSWORD SQL_APP_PASSWORD TOKENIZATION_API_KEY TOKENIZATION_ENCRYPTION_KEY SECURITY_JWT_SECRET`.
