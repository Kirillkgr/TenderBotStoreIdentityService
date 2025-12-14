terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.32"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
  }
}

# Explicitly use Docker Desktop kubeconfig/context
provider "kubernetes" {
  config_path = pathexpand("~/.kube/config")
  config_context = "docker-desktop"
}

provider "helm" {
  kubernetes {
    config_path = pathexpand("~/.kube/config")
    config_context = "docker-desktop"
  }
}

variable "enable_metrics_server" {
  type    = bool
  default = false
}
