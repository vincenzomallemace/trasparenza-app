# ─── Railway deployment via CLI ───────────────────────────────────────────────
# Il token deve essere disponibile come variabile d'ambiente RAILWAY_TOKEN
# prima di eseguire terraform apply.

locals {
  backend_dir = "${path.module}/../backend"
  railway_env_vars = join(" ", [
    for k, v in {
      NODE_ENV       = var.node_env
      GEMINI_API_KEY = var.gemini_api_key
      GOOGLE_API_KEY = var.google_api_key
      GOOGLE_CSE_ID  = var.google_cse_id
      GOOGLE_APPLICATION_CREDENTIALS_JSON = var.google_application_credentials_json
    } : "--variable '${k}=${v}'" if v != ""
  ])
}

# Step 1: crea/inizializza il progetto Railway
resource "null_resource" "railway_project_init" {
  triggers = {
    project_name = var.project_name
    service_name = var.service_name
  }

  provisioner "local-exec" {
    working_dir = local.backend_dir
    command     = <<-EOT
      echo "Initializing Railway project: ${var.project_name}"
      railway init --name "${var.project_name}" 2>/dev/null || echo "Project may already exist, continuing..."
    EOT
    environment = {
      RAILWAY_TOKEN = var.railway_token != null ? var.railway_token : ""
    }
  }
}

# Step 2: imposta le variabili d'ambiente sul servizio Railway
resource "null_resource" "railway_env_vars" {
  depends_on = [null_resource.railway_project_init]

  triggers = {
    gemini_key   = var.gemini_api_key
    google_key   = var.google_api_key
    cse_id       = var.google_cse_id
    node_env     = var.node_env
  }

  provisioner "local-exec" {
    working_dir = local.backend_dir
    command     = <<-EOT
      echo "Setting Railway environment variables..."
      railway variables set NODE_ENV="${var.node_env}" 2>/dev/null || true
      %{ if var.gemini_api_key != "" ~}
      railway variables set GEMINI_API_KEY="${var.gemini_api_key}" 2>/dev/null || true
      %{ endif ~}
      %{ if var.google_api_key != "" ~}
      railway variables set GOOGLE_API_KEY="${var.google_api_key}" 2>/dev/null || true
      %{ endif ~}
      %{ if var.google_cse_id != "" ~}
      railway variables set GOOGLE_CSE_ID="${var.google_cse_id}" 2>/dev/null || true
      %{ endif ~}
      %{ if var.google_application_credentials_json != "" ~}
      railway variables set GOOGLE_APPLICATION_CREDENTIALS_JSON="${var.google_application_credentials_json}" 2>/dev/null || true
      %{ endif ~}
      echo "Environment variables set."
    EOT
    environment = {
      RAILWAY_TOKEN = var.railway_token != null ? var.railway_token : ""
    }
  }
}

# Step 3: deploy del backend
resource "null_resource" "railway_deploy" {
  depends_on = [null_resource.railway_env_vars]

  triggers = {
    # Re-deploy ogni volta che cambia il codice (usa hash del package.json)
    deploy_trigger = filemd5("${local.backend_dir}/package.json")
    service_name   = var.service_name
  }

  provisioner "local-exec" {
    working_dir = local.backend_dir
    command     = <<-EOT
      echo "Deploying ${var.service_name} to Railway..."
      railway up --service "${var.service_name}" --detach
      echo "Deploy submitted. Check: https://railway.app/dashboard"
    EOT
    environment = {
      RAILWAY_TOKEN = var.railway_token != null ? var.railway_token : ""
    }
  }
}
