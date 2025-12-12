resource "kubernetes_secret" "redis" {
  metadata {
    name      = "redis-secret"
    namespace = kubernetes_namespace.app.metadata[0].name
  }
  type = "Opaque"
  data = {
    REDIS_PASSWORD = var.redis_password
  }
}
