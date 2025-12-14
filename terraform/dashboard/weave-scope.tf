locals {
  weave_scope_namespace = "diagnostik-tbspro-service"
  weave_scope_node_port = 32110
}

resource "kubernetes_service_account" "weave_scope" {
  metadata {
    name      = "diagnostik-weave-scope"
    namespace = kubernetes_namespace.diagnostik.metadata[0].name
  }
}

resource "kubernetes_cluster_role" "weave_scope" {
  metadata {
    name = "diagnostik-weave-scope"
  }

  rule {
    api_groups = ["*"]
    resources  = ["*"]
    verbs      = ["*"]
  }

  rule {
    non_resource_urls = ["*"]
    verbs             = ["*"]
  }
}

resource "kubernetes_cluster_role_binding" "weave_scope" {
  metadata {
    name = "diagnostik-weave-scope"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = kubernetes_cluster_role.weave_scope.metadata[0].name
  }

  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account.weave_scope.metadata[0].name
    namespace = kubernetes_namespace.diagnostik.metadata[0].name
  }
}

# Install Weave Scope via Helm
# NOTE: Официальный чарт из archived stable repo (OBSOLETE), но он до сих пор часто используется в lab/dev.
# Если вы решите сменить репозиторий/чарт позже — структура ресурсов останется той же.
resource "helm_release" "weave_scope" {
  name             = "diagnostik-weave-scope"
  repository       = "https://charts.helm.sh/stable"
  chart            = "weave-scope"
  namespace        = kubernetes_namespace.diagnostik.metadata[0].name
  version          = "1.1.8"
  create_namespace = false

  # Включаем компоненты (в чарте по умолчанию они true, но фиксируем явно)
  set {
    name  = "weave-scope-frontend.enabled"
    value = "true"
  }

  set {
    name  = "weave-scope-agent.enabled"
    value = "true"
  }

  set {
    name  = "weave-scope-cluster-agent.enabled"
    value = "true"
  }

  set {
    name  = "weave-scope-frontend.resources.requests.cpu"
    value = "25m"
  }

  set {
    name  = "weave-scope-frontend.resources.requests.memory"
    value = "64Mi"
  }

  set {
    name  = "weave-scope-frontend.resources.limits.cpu"
    value = "26m"
  }

  set {
    name  = "weave-scope-frontend.resources.limits.memory"
    value = "67Mi"
  }

  set {
    name  = "weave-scope-agent.resources.requests.cpu"
    value = "50m"
  }

  set {
    name  = "weave-scope-agent.resources.requests.memory"
    value = "128Mi"
  }

  set {
    name  = "weave-scope-agent.resources.limits.cpu"
    value = "53m"
  }

  set {
    name  = "weave-scope-agent.resources.limits.memory"
    value = "135Mi"
  }

  set {
    name  = "weave-scope-cluster-agent.resources.requests.cpu"
    value = "25m"
  }

  set {
    name  = "weave-scope-cluster-agent.resources.requests.memory"
    value = "64Mi"
  }

  set {
    name  = "weave-scope-cluster-agent.resources.limits.cpu"
    value = "26m"
  }

  set {
    name  = "weave-scope-cluster-agent.resources.limits.memory"
    value = "67Mi"
  }

  # Оставляем сервис чарта ClusterIP, а внешний доступ делаем отдельным NodePort Service (как у вас с Dashboard)
  set {
    name  = "global.service.type"
    value = "ClusterIP"
  }

  set {
    name  = "weave-scope-cluster-agent.rbac.create"
    value = "false"
  }

  set {
    name  = "weave-scope-cluster-agent.serviceAccount.create"
    value = "false"
  }

  set {
    name  = "weave-scope-cluster-agent.serviceAccount.name"
    value = kubernetes_service_account.weave_scope.metadata[0].name
  }

  depends_on = [
    kubernetes_cluster_role_binding.weave_scope,
  ]
}

# Expose Weave Scope UI over NodePort for local access
resource "kubernetes_service" "weave_scope_nodeport" {
  metadata {
    name      = "weave-scope-nodeport"
    namespace = kubernetes_namespace.diagnostik.metadata[0].name
    labels = {
      app = "weave-scope"
    }
  }

  spec {
    type = "NodePort"

    # Метки чарта (stable/weave-scope) для frontend сервиса/деплоя:
    # selector:
    #   app: weave-scope
    #   release: <helm_release name>
    #   component: frontend
    selector = {
      app       = "weave-scope"
      release   = helm_release.weave_scope.name
      component = "frontend"
    }

    port {
      name        = "http"
      port        = 80
      target_port = "http"
      node_port   = local.weave_scope_node_port
      protocol    = "TCP"
    }
  }

  depends_on = [helm_release.weave_scope]
}
