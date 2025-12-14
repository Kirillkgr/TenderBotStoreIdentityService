# Install ingress-nginx via Helm within the same Terraform module
resource "helm_release" "ingress_nginx" {
  name             = "diagnostik-ingress-nginx"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  namespace        = kubernetes_namespace.diagnostik.metadata[0].name
  version          = "4.10.0"
  create_namespace = false

  # For Docker Desktop, expose via NodePort
  set {
    name  = "controller.service.type"
    value = "NodePort"
  }
  set {
    name  = "controller.service.nodePorts.http"
    value = "32080"
  }
  set {
    name  = "controller.service.nodePorts.https"
    value = "32443"
  }
  set {
    name  = "controller.ingressClass"
    value = "diagnostik-nginx"
  }
  set {
    name  = "controller.ingressClassResource.name"
    value = "diagnostik-nginx"
  }
  set {
    name  = "controller.ingressClassResource.enabled"
    value = "true"
  }
  set {
    name  = "controller.ingressClassResource.default"
    value = "false"
  }

  set {
    name  = "controller.admissionWebhooks.enabled"
    value = "false"
  }

  set {
    name  = "controller.admissionWebhooks.patch.enabled"
    value = "false"
  }
  set {
    name  = "controller.resources.requests.cpu"
    value = "100m"
  }
  set {
    name  = "controller.resources.requests.memory"
    value = "128Mi"
  }
  set {
    name  = "controller.resources.limits.cpu"
    value = "105m"
  }
  set {
    name  = "controller.resources.limits.memory"
    value = "135Mi"
  }
  set {
    name  = "defaultBackend.resources.requests.cpu"
    value = "10m"
  }
  set {
    name  = "defaultBackend.resources.requests.memory"
    value = "20Mi"
  }
  set {
    name  = "defaultBackend.resources.limits.cpu"
    value = "11m"
  }
  set {
    name  = "defaultBackend.resources.limits.memory"
    value = "21Mi"
  }
  # Allow HTTP traffic without redirect to HTTPS (no certs in dev)
  set {
    name  = "controller.config.force-ssl-redirect"
    value = "false"
  }

  depends_on = [kubernetes_namespace.diagnostik]
}
