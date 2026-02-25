# ─── Railway Project ──────────────────────────────────────────────────────────

resource "railway_project" "trasparenza" {
  name        = var.project_name
  description = "Trasparenza - Product packaging & sustainability scanner backend"
}

# ─── Railway Environment ──────────────────────────────────────────────────────

resource "railway_environment" "production" {
  project_id = railway_project.trasparenza.id
  name       = var.environment_name
}

# ─── Railway Service (Node.js backend) ───────────────────────────────────────

resource "railway_service" "backend" {
  project_id = railway_project.trasparenza.id
  name       = var.service_name

  # Railway auto-detects Node.js from package.json.
  # Point to the backend/ subdirectory if using a monorepo.
  # source {
  #   repo   = "vincenzomallemace/trasparenza-app"
  #   branch = "main"
  # }
}

# ─── Environment Variables ────────────────────────────────────────────────────

locals {
  env_vars = {
    NODE_ENV                            = var.node_env
    GEMINI_API_KEY                      = var.gemini_api_key
    GOOGLE_API_KEY                      = var.google_api_key
    GOOGLE_CSE_ID                       = var.google_cse_id
    GOOGLE_APPLICATION_CREDENTIALS_JSON = var.google_application_credentials_json
  }
}

resource "railway_variable" "env" {
  for_each = local.env_vars

  project_id     = railway_project.trasparenza.id
  environment_id = railway_environment.production.id
  service_id     = railway_service.backend.id

  name  = each.key
  value = each.value
}

# ─── Custom Domain (optional) ────────────────────────────────────────────────

# Uncomment to add a custom domain:
# resource "railway_custom_domain" "api" {
#   project_id     = railway_project.trasparenza.id
#   environment_id = railway_environment.production.id
#   service_id     = railway_service.backend.id
#   domain         = "api.yourdomain.com"
# }
