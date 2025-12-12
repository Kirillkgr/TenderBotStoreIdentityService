variable "server_port" {
  type    = number
  default = 9900
}

variable "bucket_name" {
  type    = string
  default = "kirillkgr"
}

variable "host" {
  type    = string
  default = "tbs.localdev.me"
}

# PostgreSQL переменные
variable "postgres_database" {
  type    = string
  default = "tbsdb"  # Изменил с "tbs.localdev.me" на нормальное имя БД
}

variable "postgres_user" {
  type    = string
  default = "tbsuser"
}

variable "postgres_password" {
  type    = string
  sensitive = true
  default = ""
}

variable "google_client_id" {
  type = string
  default = "dummy-google-client-id"
}

variable "google_client_secret" {
  type      = string
  sensitive = true
  default   = ""
}

variable "google_redirect_uri" {
  type = string
  default = "https://tbspro.ru/login/oauth2/code/google"
}

variable "s3_access_key" {
  type      = string
  sensitive = true
  default   = ""
}

variable "s3_secret_key" {
  type      = string
  sensitive = true
  default   = ""
}

variable "app_cookie_domain" {
  type = string
  default = ""
}

variable "redis_password" {
  type      = string
  sensitive = true
  default   = ""
}

variable "yandex_dockerconfigjson_b64" {
  type      = string
  sensitive = true
  default   = ""
}

variable "yc_sa_key_json_b64" {
  type      = string
  sensitive = true
  default   = ""
}
