# Deployment для Redis
resource "kubernetes_deployment" "redis" {
  metadata {
    name      = "redis"
    namespace = kubernetes_namespace.app.metadata[0].name
    labels = {
      app = "redis"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "redis"
      }
    }

    template {
      metadata {
        labels = {
          app = "redis"
        }
      }

      spec {
        container {
          name  = "redis"
          image = "redis:7.2-alpine"
          port {
            container_port = 6379
          }

          resources {
            requests = {
              cpu    = "50m"
              memory = "64Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }

          liveness_probe {
            tcp_socket {
              port = 6379
            }
            initial_delay_seconds = 30
            period_seconds        = 10
          }

          readiness_probe {
            tcp_socket {
              port = 6379
            }
            initial_delay_seconds = 30
            period_seconds        = 5
          }
        }
      }
    }
  }
}

# Service для Redis
resource "kubernetes_service" "redis" {
  metadata {
    name      = "redis"
    namespace = kubernetes_namespace.app.metadata[0].name
    labels = {
      app = "redis"
    }
  }
  spec {
    selector = {
      app = "redis"
    }

    port {
      name      = "redis"
      port = 6379        # внешний порт
      target_port = 6379        # порт внутри контейнера
      protocol  = "TCP"
      node_port = 30163
    }

    type = "NodePort"           # или LoadBalancer
    #   type = "ClusterIP" # для закрытоко использования
  }
}