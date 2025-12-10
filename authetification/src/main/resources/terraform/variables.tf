# Variables Terraform pour le provisionnement de VMs

# --- SUPPRESSION DES VARIABLES CLOUD ---

# Configuration Azure
variable "azure_location" {
  description = "Région Azure où créer la VM"
  type        = string
  default     = "francecentral"
}

variable "environment" {
  description = "Environnement"
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
  description = "Taille de la VM Azure"
  type        = string
  default     = "Standard_B2s"
}

variable "image_publisher" {
  type        = string
  default     = "Canonical"
}

variable "image_offer" {
  type        = string
  default     = "UbuntuServer"
}

variable "image_sku" {
  type        = string
  default     = "22.04-LTS"
}

variable "admin_username" {
  type        = string
  default     = "azureuser"
}

variable "admin_password" {
  type        = string
  default     = ""
  sensitive   = true
}

variable "ssh_public_key" {
  type        = string
  default     = ""
}

variable "os_type" {
  type        = string
}

variable "os_version" {
  type        = string
}

variable "cpu_cores" {
  type        = number
  default     = 2
}

variable "ram_gb" {
  type        = number
  default     = 4
}

variable "disk_size" {
  type        = number
  default     = 30
}

variable "disk_type" {
  type        = string
  default     = "Premium_LRS"
}

variable "additional_storage_size" {
  type        = number
  default     = 0
}

# Configuration réseau
variable "subnet_id" {
  type        = string
  default     = ""
}

variable "create_vnet" {
  type        = bool
  default     = true
}

variable "vnet_address_space" {
  type        = string
  default     = "10.0.0.0/16"
}

variable "subnet_address_prefix" {
  type        = string
  default     = "10.0.1.0/24"
}

variable "assign_public_ip" {
  type        = bool
  default     = true
}

variable "firewall_rules" {
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
    }
  ]
}

variable "enable_monitoring" {
  type        = bool
  default     = false
}

variable "user_data" {
  type        = string
  default     = ""
}
