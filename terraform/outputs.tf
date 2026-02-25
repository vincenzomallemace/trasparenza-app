output "project_id" {
  description = "Railway project ID"
  value       = railway_project.trasparenza.id
}

output "service_id" {
  description = "Railway backend service ID"
  value       = railway_service.backend.id
}

output "environment_id" {
  description = "Railway production environment ID"
  value       = railway_environment.production.id
}

output "railway_dashboard_url" {
  description = "Railway project dashboard URL"
  value       = "https://railway.app/project/${railway_project.trasparenza.id}"
}
