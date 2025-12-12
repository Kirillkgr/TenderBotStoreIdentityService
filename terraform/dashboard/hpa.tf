resource "kubernetes_horizontal_pod_autoscaler_v2" "nginx" {
  metadata {
    name      = "nginx-hpa"
    namespace = kubernetes_namespace.dashboard.metadata[0].name
  }

  spec {
    min_replicas = 1
    max_replicas = 5

    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment.nginx.metadata[0].name
    }

    metric {
      type = "Resource"
      resource {
        name = "cpu"
        target {
          type                = "Utilization"
          average_utilization = 30
        }
      }
    }

    behavior {
      scale_down {
        select_policy                = "Max"
        stabilization_window_seconds = 20
        policy {
          type           = "Percent"
          value          = 100
          period_seconds = 15
        }
      }
      scale_up {
        select_policy                = "Max"
        stabilization_window_seconds = 0
        policy {
          type           = "Percent"
          value          = 100
          period_seconds = 10
        }
      }
    }
  }

  depends_on = [kubernetes_deployment.nginx]
}
