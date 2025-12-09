# Variables Terraform pour le provisionnement de VMs

# Configuration Terraform Cloud
variable "terraform_organization" {
  description = "Nom de l'organisation Terraform Cloud"
  type        = string
  default     = "my-org"
}

variable "terraform_workspace" {
  description = "Nom du workspace Terraform Cloud"
  type        = string
  default     = "vm-provisioning"
}

# Configuration Azure
variable "azure_location" {
  description = "Région Azure où créer la VM"
  type        = string
  default     = "West Europe"
}

variable "environment" {
  description = "Environnement (dev, staging, production)"
  type        = string
  default     = "production"
}

variable "application_name" {
  description = "Nom de l'application"
  type        = string
  default     = "VM-Provisioning-System"
}

# Configuration de la VM
variable "vm_name" {
  description = "Nom de la machine virtuelle"
  type        = string
}

variable "demande_id" {
  description = "ID de la demande d'origine"
  type        = string
}

variable "vm_size" {
  description = "Taille de la VM Azure (ex: Standard_B2s, Standard_D2s_v3)"
  type        = string
  default     = "Standard_B2s"
}

variable "image_publisher" {
  description = "Publisher de l'image Azure (ex: Canonical, MicrosoftWindowsServer)"
  type        = string
  default     = "Canonical"
}

variable "image_offer" {
  description = "Offer de l'image Azure (ex: UbuntuServer, WindowsServer)"
  type        = string
  default     = "UbuntuServer"
}

variable "image_sku" {
  description = "SKU de l'image Azure (ex: 22.04-LTS, 2019-Datacenter)"
  type        = string
  default     = "22.04-LTS"
}

variable "admin_username" {
  description = "Nom d'utilisateur administrateur pour la VM"
  type        = string
  default     = "azureuser"
}

variable "admin_password" {
  description = "Mot de passe administrateur (pour Windows uniquement)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "ssh_public_key" {
  description = "Clé publique SSH pour l'authentification Linux"
  type        = string
  default     = ""
}

# Configuration OS
variable "os_type" {
  description = "Type de système d'exploitation (Ubuntu, CentOS, Windows, etc.)"
  type        = string
}

variable "os_version" {
  description = "Version du système d'exploitation"
  type        = string
}

# Configuration ressources
variable "cpu_cores" {
  description = "Nombre de cœurs CPU"
  type        = number
  default     = 2
}

variable "ram_gb" {
  description = "Quantité de RAM en GB"
  type        = number
  default     = 4
}

# Configuration stockage
variable "disk_size" {
  description = "Taille du disque racine en GB"
  type        = number
  default     = 20
}

variable "disk_type" {
  description = "Type de disque Azure (Premium_LRS, Standard_LRS, Premium_ZRS, StandardSSD_LRS, UltraSSD_LRS)"
  type        = string
  default     = "Premium_LRS"
}

variable "disk_encryption" {
  description = "Activer le chiffrement du disque"
  type        = bool
  default     = true
}

variable "additional_storage_size" {
  description = "Taille du stockage additionnel en GB (0 pour désactiver)"
  type        = number
  default     = 0
}

# Configuration réseau
variable "subnet_id" {
  description = "ID du subnet Azure (optionnel, sera créé si create_vnet = true)"
  type        = string
  default     = ""
}

variable "create_vnet" {
  description = "Créer un Virtual Network et Subnet si ils n'existent pas"
  type        = bool
  default     = false
}

variable "vnet_address_space" {
  description = "Espace d'adressage du Virtual Network (si création)"
  type        = string
  default     = "10.0.0.0/16"
}

variable "subnet_address_prefix" {
  description = "Préfixe d'adresse du Subnet (si création)"
  type        = string
  default     = "10.0.1.0/24"
}

variable "assign_public_ip" {
  description = "Assigner une IP publique"
  type        = bool
  default     = true
}

# Configuration pare-feu (Network Security Group)
variable "firewall_rules" {
  description = "Règles de pare-feu pour le Network Security Group Azure"
  type = list(object({
    name                       = string
    priority                   = number
    protocol                   = string
    destination_port_range     = string
    source_address_prefix      = string
    description                = string
  }))
  default = [
    {
      name                   = "SSH"
      priority               = 1000
      protocol               = "Tcp"
      destination_port_range = "22"
      source_address_prefix  = "*"
      description            = "SSH"
    },
    {
      name                   = "HTTP"
      priority               = 1001
      protocol               = "Tcp"
      destination_port_range = "80"
      source_address_prefix  = "*"
      description            = "HTTP"
    },
    {
      name                   = "HTTPS"
      priority               = 1002
      protocol               = "Tcp"
      destination_port_range = "443"
      source_address_prefix  = "*"
      description            = "HTTPS"
    }
  ]
}

# Configuration monitoring
variable "enable_monitoring" {
  description = "Activer le monitoring détaillé CloudWatch"
  type        = bool
  default     = true
}

# User data (script de configuration initiale)
variable "user_data" {
  description = "Script user-data pour configuration initiale de la VM"
  type        = string
  default     = ""
}

