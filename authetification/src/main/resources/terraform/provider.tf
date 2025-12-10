terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

provider "azurerm" {
  features {}
  # L'authentification se fait via les variables d'environnement (Managed Identity ou Service Principal)
  # donc pas besoin de mettre client_id / client_secret ici si ton cluster est bien configuré.
}
