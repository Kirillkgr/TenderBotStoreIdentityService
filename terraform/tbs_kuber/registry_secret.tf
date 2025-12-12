# registry_secret.tf
# Native secret for Yandex Container Registry pull, no scripts
resource "kubernetes_secret" "registry" {
  metadata {
    name      = "yandex-registry-secret"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  type = "kubernetes.io/dockerconfigjson"

  data = {
    ".dockerconfigjson" = base64decode(var.yandex_dockerconfigjson_b64)
  }

  depends_on = [kubernetes_namespace.app]
}
