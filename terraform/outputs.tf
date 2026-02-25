output "project_name" {
  description = "Railway project name"
  value       = var.project_name
}

output "service_name" {
  description = "Railway service name"
  value       = var.service_name
}

output "railway_dashboard_url" {
  description = "Railway dashboard URL"
  value       = "https://railway.app/dashboard"
}

output "deploy_instructions" {
  description = "How to get the deployed URL"
  value       = "After deploy: cd backend && RAILWAY_TOKEN=<token> railway domain"
}
