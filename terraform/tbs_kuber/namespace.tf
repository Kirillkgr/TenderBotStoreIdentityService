# Пространство имен приложения
resource "kubernetes_namespace" "app" {
  metadata { name = "tbs-kuber" }
}


resource "kubernetes_resource_quota" "app" {
  metadata {
    name      = "app-quota"
    namespace = kubernetes_namespace.app.metadata[0].name  # Используем созданный namespace
  }

  spec {
    hard = {
      # Ограничиваем общее использование в namespace
      "limits.cpu" = "2000m"       # позволяет surge/обновления и дополнительные сервисы
      "limits.memory" = "2Gi"     # хватает для rolling update (+1 pod) и базовых сервисов
      "requests.cpu"    = "1500m"
      "requests.memory" = "1.5Gi"
      "pods"            = "20"       # запас по количеству pod'ов
    }
  }

  depends_on = [kubernetes_namespace.app]  # Ждем создания namespace
}