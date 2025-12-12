# PVC для данных приложения (Spring Boot)
resource "kubernetes_persistent_volume_claim" "data" {
  metadata {
    name      = "tbs-data"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    access_modes = ["ReadWriteOnce"]

    resources {
      requests = {
        storage = "1Gi"
      }
    }
  }

  depends_on = [kubernetes_namespace.app]
}

# PVC для данных PostgreSQL
resource "kubernetes_persistent_volume_claim" "postgres_data" {
  metadata {
    name      = "postgres-data"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    access_modes = ["ReadWriteOnce"]

    resources {
      requests = {
        storage = "2Gi"
      }
    }
  }

  depends_on = [kubernetes_namespace.app]
}
