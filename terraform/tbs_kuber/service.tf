resource "kubernetes_service" "app" {
  metadata {
    name      = "tbs-kuber"
    namespace = kubernetes_namespace.app.metadata[0].name
    labels = {
      app = "tbs-kuber"
    }
  }
  spec {
    selector = {
      app = "tbs-kuber"
    }
    port {
      name        = "http"
      port        = 9900
      target_port = 9900
      protocol    = "TCP"
    }
    type = "ClusterIP"
  }
}

resource "kubernetes_service" "app_nodeport" {
  metadata {
    name      = "tbs-kuber-nodeport"
    namespace = kubernetes_namespace.app.metadata[0].name
    labels = {
      app = "tbs-kuber"
    }
  }
  spec {
    type = "NodePort"
    selector = {
      app = "tbs-kuber"
    }
    port {
      name        = "http"
      port        = 9900
      target_port = 9900
      node_port   = 30101
      protocol    = "TCP"
    }
  }
}
