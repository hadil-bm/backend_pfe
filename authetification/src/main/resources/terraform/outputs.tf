# Outputs Terraform pour récupérer les informations de la VM Azure créée

output "vm_id" {
  description = "ID de la VM Azure créée"
  value       = var.os_type != "Windows" ? azurerm_linux_virtual_machine.vm[0].id : azurerm_windows_virtual_machine.vm_windows[0].id
}

output "vm_name" {
  description = "Nom de la VM"
  value       = var.vm_name
}

output "vm_private_ip" {
  description = "Adresse IP privée de la VM"
  value       = azurerm_network_interface.vm_nic.private_ip_address
}

output "vm_public_ip" {
  description = "Adresse IP publique de la VM"
  value       = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].ip_address : null
}

output "vm_fqdn" {
  description = "FQDN (Fully Qualified Domain Name) de la VM"
  value       = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].fqdn : null
}

output "vm_location" {
  description = "Localisation Azure de la VM"
  value       = azurerm_resource_group.vm_rg.location
}

output "vm_size" {
  description = "Taille de la VM"
  value       = var.vm_size
}

output "vm_resource_group_name" {
  description = "Nom du Resource Group"
  value       = azurerm_resource_group.vm_rg.name
}

output "vm_network_security_group_id" {
  description = "ID du Network Security Group"
  value       = azurerm_network_security_group.vm_nsg.id
}

output "vm_network_security_group_name" {
  description = "Nom du Network Security Group"
  value       = azurerm_network_security_group.vm_nsg.name
}

output "vm_network_interface_id" {
  description = "ID de la Network Interface"
  value       = azurerm_network_interface.vm_nic.id
}

output "vm_os_disk_id" {
  description = "ID du disque OS"
  value       = var.os_type != "Windows" ? azurerm_linux_virtual_machine.vm[0].os_disk[0].disk_id : azurerm_windows_virtual_machine.vm_windows[0].os_disk[0].disk_id
}

output "vm_additional_storage_id" {
  description = "ID du disque de données additionnel (si créé)"
  value       = var.additional_storage_size > 0 ? azurerm_managed_disk.additional_storage[0].id : null
}

output "vm_computer_name" {
  description = "Nom d'ordinateur de la VM"
  value       = var.os_type != "Windows" ? azurerm_linux_virtual_machine.vm[0].computer_name : azurerm_windows_virtual_machine.vm_windows[0].computer_name
}

output "vm_tags" {
  description = "Tags de la VM"
  value       = var.os_type != "Windows" ? azurerm_linux_virtual_machine.vm[0].tags : azurerm_windows_virtual_machine.vm_windows[0].tags
}

output "demande_id" {
  description = "ID de la demande d'origine"
  value       = var.demande_id
}

output "vm_name" {
  description = "Nom de la VM"
  value       = var.vm_name
}

# Output pour l'intégration avec l'application
output "provisioning_info" {
  description = "Informations complètes de provisionnement pour l'application"
  value = {
    vm_id                    = var.os_type != "Windows" ? azurerm_linux_virtual_machine.vm[0].id : azurerm_windows_virtual_machine.vm_windows[0].id
    vm_public_ip            = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].ip_address : null
    vm_private_ip           = azurerm_network_interface.vm_nic.private_ip_address
    vm_fqdn                 = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].fqdn : null
    vm_size                 = var.vm_size
    vm_location             = azurerm_resource_group.vm_rg.location
    resource_group_name     = azurerm_resource_group.vm_rg.name
    network_security_group   = azurerm_network_security_group.vm_nsg.id
    network_interface_id     = azurerm_network_interface.vm_nic.id
    demande_id              = var.demande_id
    vm_name                 = var.vm_name
    os_type                 = var.os_type
    os_version              = var.os_version
  }
}

