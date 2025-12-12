resource "kubernetes_horizontal_pod_autoscaler_v2" "app" {
  metadata {
    name      = "tbs-kuber-hpa"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    min_replicas = 1
    max_replicas = 5

    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment.app.metadata[0].name
    }

    metric {
      type = "Resource"
      resource {
        name = "cpu"
        target {
          type                = "Utilization"
          average_utilization = 45
        }
      }
    }
    # Добавляем масштабирование по памяти
    metric {
      type = "Resource"
      resource {
        name = "memory"
        target {
          type                = "Utilization"
          average_utilization = 90  # Масштабируем при 90% использования памяти
        }
      }
    }

    behavior {
      scale_down {
        select_policy                 = "Max"
        stabilization_window_seconds = 20
        policy {
          type           = "Percent"
          value          = 40
          period_seconds = 60
        }
      }
      scale_up {
        select_policy                 = "Max"
        stabilization_window_seconds = 10
        policy {
          type           = "Percent"
          value          = 90
          period_seconds = 10
        }
      }
    }
  }

  depends_on = [kubernetes_deployment.app]
}
