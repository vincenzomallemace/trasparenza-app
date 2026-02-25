terraform {
  required_version = ">= 1.5.0"

  required_providers {
    # Railway non ha un provider ufficiale stabile nel Terraform Registry.
    # Usiamo 'null' + local-exec con la Railway CLI, che e' il metodo
    # raccomandato per l'IaC su Railway.
    null = {
      source  = "hashicorp/null"
      version = "~> 3.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.0"
    }
  }
}

# Il token viene passato via env var RAILWAY_TOKEN
# oppure come TF_VAR_railway_token e usato nei local-exec tramite
# la variabile terraform railway_token.
