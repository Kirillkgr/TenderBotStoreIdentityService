# secret.tf
resource "kubernetes_secret" "app" {
  metadata {
    name      = "tbs-secrets"
    namespace = kubernetes_namespace.app.metadata[0].name
  }
  type = "Opaque"
  data = {
    # Spring profile
    SPRING_PROFILES_ACTIVE = "prod"

    # DB connection (aligns with docker-compose)
    SPRING_DATASOURCE_URL      = "jdbc:postgresql://postgres.${kubernetes_namespace.app.metadata[0].name}.svc.cluster.local:5432/${kubernetes_config_map.postgres.data.POSTGRES_DB}"
    SPRING_DATASOURCE_USERNAME = kubernetes_config_map.postgres.data.POSTGRES_USER
    SPRING_DATASOURCE_PASSWORD = var.postgres_password

    # OAuth2 Google (from docker-compose and CI)
    GOOGLE_CLIENT_ID     = var.google_client_id
    GOOGLE_CLIENT_SECRET = var.google_client_secret
    GOOGLE_REDIRECT_URI  = var.google_redirect_uri

    # S3 credentials (from CI/docker-compose)
    S3_ACCESS_KEY = var.s3_access_key
    S3_SECRET_KEY = var.s3_secret_key

    # Cookie domain for backend
    APP_COOKIE_DOMAIN = var.app_cookie_domain
  }
  depends_on = [kubernetes_namespace.app]
}

# Secret для PostgreSQL (пароль)
resource "kubernetes_secret" "postgres" {
  metadata {
    name      = "postgres-secret"
    namespace = kubernetes_namespace.app.metadata[0].name
  }
  type = "Opaque"
  data = {
    POSTGRES_PASSWORD = var.postgres_password
  }
  depends_on = [kubernetes_namespace.app]
}
# Yandex token
# Секрет с RSA-ключом сервисного аккаунта
resource "kubernetes_secret" "sa_key" {
  metadata {
    name      = "sa-key"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  type = "Opaque"

  data = {
    "authorized_key.json" = base64decode(var.yc_sa_key_json_b64)
  }
}
