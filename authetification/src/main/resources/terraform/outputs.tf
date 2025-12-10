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
  description = "FQDN de la VM"
  value       = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].fqdn : null
}

output "vm_resource_group_name" {
  description = "Nom du Resource Group"
  value       = azurerm_resource_group.vm_rg.name
}

output "demande_id" {
  description = "ID de la demande d'origine"
  value       = var.demande_id
}

# Output complet pour l'app
output "provisioning_info" {
  description = "Informations complètes de provisionnement"
  value = {
    vm_id               = var.os_type != "Windows" ? azurerm_linux_virtual_machine.vm[0].id : azurerm_windows_virtual_machine.vm_windows[0].id
    vm_public_ip        = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].ip_address : null
    vm_private_ip       = azurerm_network_interface.vm_nic.private_ip_address
    resource_group_name = azurerm_resource_group.vm_rg.name
    vm_name             = var.vm_name
  }
}
