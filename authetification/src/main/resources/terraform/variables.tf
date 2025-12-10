variable "azure_location" {
  default = "francecentral"  # <-- ICI : On force la France
}

variable "environment" {
  default = "production"
}

variable "application_name" {
  default = "VM-Provisioning-System"
}

variable "vm_name" {
  type = string
}

variable "demande_id" {
  type = string
}

variable "vm_size" {
  default = "Standard_B2s"
}

variable "image_publisher" {
  default = "Canonical"
}

variable "image_offer" {
  default = "UbuntuServer"
}

variable "image_sku" {
  default = "22.04-LTS"
}

variable "admin_username" {
  default = "azureuser"
}

variable "admin_password" {
  default   = ""
  sensitive = true
}

variable "ssh_public_key" {
  default = ""
}

variable "os_type" {
  type = string
}

variable "os_version" {
  type = string
}

variable "cpu_cores" {
  default = 2
}

variable "ram_gb" {
  default = 4
}

variable "disk_size" {
  default = 30
}

variable "disk_type" {
  default = "Premium_LRS"
}

variable "additional_storage_size" {
  default = 0
}

variable "subnet_id" {
  default = ""
}

variable "create_vnet" {
  default = true
}

variable "vnet_address_space" {
  default = "10.0.0.0/16"
}

variable "subnet_address_prefix" {
  default = "10.0.1.0/24"
}

variable "assign_public_ip" {
  default = false  # <-- ICI : IMPORTANT ! On désactive l'IP publique par défaut
}

variable "firewall_rules" {
  default = []
}

variable "enable_monitoring" {
  default = false
}

variable "user_data" {
  default = ""
}
