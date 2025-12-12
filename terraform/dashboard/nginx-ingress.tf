# Ingress to expose test nginx via ingress-nginx, within the Dashboard module
resource "kubernetes_ingress_v1" "nginx" {
  metadata {
    name      = "nginx-ingress"
    namespace = kubernetes_namespace.dashboard.metadata[0].name
    annotations = {
      "kubernetes.io/ingress.class"                    = "nginx"
      "nginx.ingress.kubernetes.io/ssl-redirect"       = "false"
      "nginx.ingress.kubernetes.io/force-ssl-redirect" = "false"
    }
  }
  spec {
    ingress_class_name = "nginx"
    rule {
      host = "nginx.localdev.me"
      http {
        path {
          path      = "/"
          path_type = "Prefix"
          backend {
            service {
              name = kubernetes_service.nginx_clusterip.metadata[0].name
              port {
                number = 80
              }
            }
          }
        }
      }
    }
  }

  depends_on = [helm_release.ingress_nginx, kubernetes_service.nginx_clusterip]
}
