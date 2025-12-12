# Test nginx resources deployed into the same namespace as Dashboard
resource "kubernetes_deployment" "nginx" {
  metadata {
    name      = "nginx"
    namespace = kubernetes_namespace.dashboard.metadata[0].name
    labels = {
      app = "nginx"
    }
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "nginx"
      }
    }
    template {
      metadata {
        labels = {
          app = "nginx"
        }
      }
      spec {
        container {
          name  = "nginx"
          image = "nginx:1.25-alpine"
          port {
            container_port = 80
          }
          resources {
            limits = {
              cpu    = "50m"
              memory = "128Mi"
            }
            requests = {
              cpu    = "50m"
              memory = "64Mi"
            }
          }
          liveness_probe {
            http_get {
              path = "/"
              port = 80
            }
            initial_delay_seconds = 5
            period_seconds        = 10
          }
          readiness_probe {
            http_get {
              path = "/"
              port = 80
            }
            initial_delay_seconds = 2
            period_seconds        = 5
          }
        }
      }
    }
  }

  depends_on = [helm_release.kubernetes_dashboard]
}

resource "kubernetes_service" "nginx_nodeport" {
  metadata {
    name      = "nginx-nodeport"
    namespace = kubernetes_namespace.dashboard.metadata[0].name
    labels = {
      app = "nginx"
    }
  }
  spec {
    type = "NodePort"
    selector = {
      app = "nginx"
    }
    port {
      name        = "http"
      port        = 80
      target_port = 80
      node_port   = 30081
    }
  }

  depends_on = [helm_release.kubernetes_dashboard]
}

# ClusterIP service for ingress routing
resource "kubernetes_service" "nginx_clusterip" {
  metadata {
    name      = "nginx"
    namespace = kubernetes_namespace.dashboard.metadata[0].name
    labels = {
      app = "nginx"
    }
  }
  spec {
    selector = {
      app = "nginx"
    }
    port {
      name        = "http"
      port        = 80
      target_port = 80
      protocol    = "TCP"
    }
  }

  depends_on = [kubernetes_deployment.nginx]
}
