# deployment_postgres.tf
resource "kubernetes_priority_class" "postgres" {
  metadata {
    name = "database-priority"
  }
  value          = 1000000
  global_default = false
  description    = "Для критически важных БД"
}

resource "kubernetes_deployment" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.app.metadata[0].name
    labels = {
      app = "postgres"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "postgres"
      }
    }

    template {
      metadata {
        labels = {
          app = "postgres"
        }
      }

      spec {
        priority_class_name = kubernetes_priority_class.postgres.metadata[0].name

        container {
          name  = "postgres"
          image = "postgres:15-alpine"

          # Вариант 1: Прямые значения (рекомендую для простоты):
          env {
            name  = "POSTGRES_DB"
            value = "tbsdb"  # Прямое значение
          }

          env {
            name  = "POSTGRES_USER"
            value = "tbsuser"  # Прямое значение
          }

          env {
            name  = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.postgres.metadata[0].name
                key  = "POSTGRES_PASSWORD"
              }
            }
          }

          # Настройки производительности
          env {
            name  = "POSTGRES_SHARED_BUFFERS"
            value = "128MB"  # Прямое значение
          }

          env {
            name  = "PGDATA"
            value = "/var/lib/postgresql/data/pgdata"
          }

          # Port
          port {
            container_port = 5432
          }

          # Volume mount
          volume_mount {
            name       = "postgres-data"
            mount_path = "/var/lib/postgresql/data"
          }

          # Health checks - ИСПРАВЬТЕ КОМАНДУ!
          liveness_probe {
            exec {
              command = [
                "sh",
                "-c",
                "pg_isready -U tbsuser -d tbsdb"  # Укажите и пользователя И базу!
              ]
            }
            initial_delay_seconds = 60
            period_seconds        = 30
          }

          readiness_probe {
            exec {
              command = [
                "sh",
                "-c",
                "pg_isready -U tbsuser -d tbsdb"  # Укажите и пользователя И базу!
              ]
            }
            initial_delay_seconds = 15
            period_seconds        = 10
          }

          resources {
            requests = {
              cpu    = "500m"
              memory = "512Mi"
            }
            limits = {
              cpu    = "800m"
              memory = "512Mi"
            }
          }
        }

        volume {
          name = "postgres-data"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.postgres_data.metadata[0].name
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_namespace.app,
    kubernetes_priority_class.postgres,
    kubernetes_persistent_volume_claim.postgres_data
  ]
}

resource "kubernetes_service" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.app.metadata[0].name
    labels = {
      app = "postgres"
    }
  }

  spec {
    selector = {
      app = "postgres"
    }

    port {
      name        = "postgres"
      port        = 5432 # Внешний порт
      target_port = 5432 # Порт внутри контейнера
      protocol    = "TCP"
      node_port = 30111
    }
    type = "NodePort"
    # type = "ClusterIP"
  }

  depends_on = [kubernetes_deployment.postgres]
}

# docker push cr.yandex/crpk4vp9h6qgr06hnusi/kirillkgr/tbs_kuber:1.1.1