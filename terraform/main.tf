terraform {
  required_version = ">= 1.5.0"

  required_providers {
    railway = {
      # Railway Terraform Provider
      # https://registry.terraform.io/providers/terraform-railway/railway
      source  = "terraform-railway/railway"
      version = "~> 0.3"
    }
  }

  # Optional: store state remotely (recommended for production)
  # backend "s3" {
  #   bucket = "your-tf-state-bucket"
  #   key    = "trasparenza/terraform.tfstate"
  #   region = "eu-central-1"
  # }
}

provider "railway" {
  # Set via RAILWAY_TOKEN env var or directly (not recommended):
  # token = var.railway_token
}
