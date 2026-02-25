variable "railway_token" {
  description = "Railway API token. Set via RAILWAY_TOKEN env var or TF_VAR_railway_token."
  type        = string
  sensitive   = true
  default     = null
}

variable "project_name" {
  description = "Name for the Railway project"
  type        = string
  default     = "trasparenza-app"
}

variable "environment_name" {
  description = "Railway environment name"
  type        = string
  default     = "production"
}

variable "service_name" {
  description = "Name for the backend service"
  type        = string
  default     = "trasparenza-backend"
}

# ─── App environment variables ─────────────────────────────────────────────────

variable "gemini_api_key" {
  description = "Google Gemini API key"
  type        = string
  sensitive   = true
}

variable "google_api_key" {
  description = "Google API key (for Search/Knowledge Graph APIs)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "google_cse_id" {
  description = "Google Custom Search Engine ID"
  type        = string
  sensitive   = true
  default     = ""
}

variable "google_application_credentials_json" {
  description = "Google Service Account JSON string (for Vision API)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "node_env" {
  description = "Node.js environment"
  type        = string
  default     = "production"
}

variable "port" {
  description = "Port the server listens on (Railway sets this automatically via $PORT)"
  type        = string
  default     = "3000"
}
