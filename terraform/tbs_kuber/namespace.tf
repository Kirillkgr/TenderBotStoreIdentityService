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
      "limits.cpu" = "3500m"    # 3.5 ядра из 4
      "limits.memory" = "1800Mi"   # 1.8 ГБ из 2
      "requests.cpu"    = "2000m"
      "requests.memory" = "1200Mi"
      "pods"            = "10"       # Максимум 10 подов
    }
  }

  depends_on = [kubernetes_namespace.app]  # Ждем создания namespace
}