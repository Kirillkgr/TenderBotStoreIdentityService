# Namespace for ingress controller
resource "kubernetes_namespace" "ingress_nginx" {
  metadata { name = "ingress-nginx" }
}

# Install ingress-nginx via Helm within the same Terraform module
resource "helm_release" "ingress_nginx" {
  name             = "ingress-nginx"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  namespace        = kubernetes_namespace.ingress_nginx.metadata[0].name
  version          = "4.10.0"
  create_namespace = false

  # For Docker Desktop, expose via NodePort
  set {
    name  = "controller.service.type"
    value = "NodePort"
  }
  set {
    name  = "controller.service.nodePorts.http"
    value = "30080"
  }
  set {
    name  = "controller.service.nodePorts.https"
    value = "30443"
  }
  # Allow HTTP traffic without redirect to HTTPS (no certs in dev)
  set {
    name  = "controller.config.force-ssl-redirect"
    value = "false"
  }
}
