resource "kubernetes_deployment" "app" {
  metadata {
    name      = "tbs-kuber"
    namespace = kubernetes_namespace.app.metadata[0].name

    labels = {
      app = "tbs-kuber"
    }
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "tbs-kuber"
      }
    }
    template {
      metadata {
        labels = {
          app = "tbs-kuber"
        }
      }
      spec {
        termination_grace_period_seconds = 120
        init_container {
          name  = "wait-for-postgres"
          image = "postgres:15-alpine"
          command = [
            "sh",
            "-c",
            "PGHOST=postgres.${kubernetes_namespace.app.metadata[0].name}.svc.cluster.local; until getent hosts $PGHOST; do echo waiting dns; sleep 2; done; until pg_isready -h $PGHOST -p 5432; do echo waiting postgres; sleep 2; done"
          ]
          resources {
            limits = {
              cpu    = "100m"
              memory = "128Mi"
            }
            requests = {
              cpu    = "50m"
              memory = "64Mi"
            }
          }
        }
        image_pull_secrets {
          name = "yandex-registry-secret"
        }
        container {
          name              = "tbs-kuber"
          image = "cr.yandex/crpp92t3gcj0he0j8nj2/tenderbotstore_server:master"
          image_pull_policy = "Always"

          port {
            container_port = 9900
          }
          # Ensure SERVER_PORT is explicitly present
          env {
            name = "SERVER_PORT"
            value_from {
              config_map_key_ref {
                name = kubernetes_config_map.app.metadata[0].name
                key  = "SERVER_PORT"
              }
            }
          }
          env_from {
            config_map_ref {
              name = kubernetes_config_map.app.metadata[0].name
            }
          }
          env_from {
            secret_ref {
              name = kubernetes_secret.app.metadata[0].name
            }
          }
          env {
            name  = "REDIS_HOST"
            value = "redis"
          }
          env {
            name  = "REDIS_PORT"
            value = "6379"
          }
          resources {
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
            requests = {
              cpu    = "200m"
              memory = "256Mi"
            }
          }

          readiness_probe {
            http_get {
              path = "/status"
              port = 9900
            }
            initial_delay_seconds = 60
            period_seconds        = 10
          }

          startup_probe {
            http_get {
              path = "/status"
              port = 9900
            }
            # Allow long startup/migrations before liveness begins to kill
            period_seconds    = 10
            failure_threshold = 60
          }

          liveness_probe {
            http_get {
              path = "/status"
              port = 9900
            }
            # Liveness should only begin after startup probe has succeeded
            initial_delay_seconds = 120
            period_seconds        = 15
          }
        }
        volume {
          name = "data"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.data.metadata[0].name
          }
        }
      }
    }
  }
}
