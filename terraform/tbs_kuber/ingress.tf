resource "kubernetes_ingress_v1" "app" {
  metadata {
    name      = "tbs-kuber-ingress"
    namespace = kubernetes_namespace.app.metadata[0].name
    annotations = {
      "kubernetes.io/ingress.class"                     = "nginx"
      "nginx.ingress.kubernetes.io/ssl-redirect"       = "false"
      "nginx.ingress.kubernetes.io/force-ssl-redirect" = "false"
    }
  }
  spec {
    ingress_class_name = "nginx"
    rule {
      host = var.host
      http {
        path {
          path      = "/"
          path_type = "Prefix"
          backend {
            service {
              name = kubernetes_service.app.metadata[0].name
              port { number = 9900 }
            }
          }
        }
      }
    }
  }
}
