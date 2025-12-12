# Пространство имен дашбоард
resource "kubernetes_namespace" "dashboard" {
  metadata {
    name = "kubernetes-dashboard"
  }
}

# Grant the Dashboard's own ServiceAccount full access (DEV ONLY, skip-login)
resource "kubernetes_cluster_role_binding" "dashboard_sa_admin" {
  metadata {
    name = "kubernetes-dashboard-sa-admin"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "cluster-admin"
  }
  subject {
    kind      = "ServiceAccount"
    name      = "kubernetes-dashboard"
    namespace = kubernetes_namespace.dashboard.metadata[0].name
  }

  depends_on = [helm_release.kubernetes_dashboard]
}
# Install Kubernetes Dashboard via Helm
resource "helm_release" "kubernetes_dashboard" {
  name             = "kubernetes-dashboard"
  repository       = "https://kubernetes.github.io/dashboard/"
  chart            = "kubernetes-dashboard"
  namespace        = kubernetes_namespace.dashboard.metadata[0].name
  version          = "6.0.8"
  create_namespace = false

  # Keep default ClusterIP; we'll add a separate NodePort Service below
  # You can set custom values here if needed via set block

  # Serve over HTTP (no TLS) on container port 9090
  set {
    name  = "protocolHttp"
    value = "true"
  }

  # Allow opening Dashboard without token (DEV ONLY)
  set {
    name  = "enableSkipLogin"
    value = "true"
  }
}

# Admin ServiceAccount (dev/local only)
resource "kubernetes_service_account" "admin_user" {
  metadata {
    name      = "admin-user"
    namespace = kubernetes_namespace.dashboard.metadata[0].name
  }
}

resource "kubernetes_cluster_role_binding" "admin_user_binding" {
  metadata {
    name = "admin-user-binding"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "cluster-admin"
  }
  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account.admin_user.metadata[0].name
    namespace = kubernetes_service_account.admin_user.metadata[0].namespace
  }
}

# Expose Dashboard over NodePort for local access
resource "kubernetes_service" "dashboard_nodeport" {
  metadata {
    name      = "kubernetes-dashboard-nodeport"
    namespace = kubernetes_namespace.dashboard.metadata[0].name
    labels = {
      app = "kubernetes-dashboard"
    }
  }
  spec {
    type = "NodePort"

    # Match labels created by the Helm chart
    selector = {
      "app.kubernetes.io/name"     = "kubernetes-dashboard"
      "app.kubernetes.io/instance" = helm_release.kubernetes_dashboard.name
    }

    port {
      name        = "http"
      port        = 333
      target_port = 9090
      node_port   = 30100
      protocol    = "TCP"
    }
  }

  depends_on = [helm_release.kubernetes_dashboard]
}
