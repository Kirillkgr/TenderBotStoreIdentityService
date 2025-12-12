# configmap.tf
# ConfigMap для основного приложения
resource "kubernetes_config_map" "app" {
  metadata {
    name      = "tbs-config"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  data = {
    # Порт приложения
    SERVER_PORT = tostring(var.server_port)
    SPRING_DATASOURCE_URL = kubernetes_secret.app.data.SPRING_DATASOURCE_URL

    # S3 bucket
    BUCKET_NAME = var.bucket_name

    # Дополнительные настройки Spring Boot
    SPRING_JPA_HIBERNATE_DDL_AUTO = "validate"
    SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT = "org.hibernate.dialect.PostgreSQLDialect"
    SPRING_JPA_SHOW_SQL = "false"
    SPRING_JPA_PROPERTIES_HIBERNATE_JDBC_BATCH_SIZE = "10"

    # Настройки для Redis
    REDIS_HOST = "redis"
    REDIS_PORT = "6379"
  }

  depends_on = [kubernetes_namespace.app]
}

# ConfigMap для PostgreSQL (несекретные данные)
resource "kubernetes_config_map" "postgres" {
  metadata {
    name      = "postgres-config"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  data = {
    POSTGRES_DB   = var.postgres_database
    POSTGRES_USER = var.postgres_user

    # Настройки производительности
    POSTGRES_SHARED_BUFFERS = "128MB"
    POSTGRES_EFFECTIVE_CACHE_SIZE = "384MB"
  }

  depends_on = [kubernetes_namespace.app]
}