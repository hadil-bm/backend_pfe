output "vm_id" {
  # Utilisation de 'length' pour éviter les erreurs si la VM n'est pas créée
  value = length(azurerm_linux_virtual_machine.vm) > 0 ? azurerm_linux_virtual_machine.vm[0].id : ""
}

output "vm_private_ip" {
  value = azurerm_network_interface.vm_nic.private_ip_address
}

output "vm_public_ip" {
  value = "N/A"
  description = "Aucune IP publique assignée"
}

output "vm_fqdn" {
  value = "N/A"
  description = "Aucun FQDN assigné"
}

output "provisioning_info" {
  value = {
    vm_name             = var.vm_name
    resource_group_name = azurerm_resource_group.vm_rg.name
    vm_id               = length(azurerm_linux_virtual_machine.vm) > 0 ? azurerm_linux_virtual_machine.vm[0].id : ""
    vm_private_ip       = azurerm_network_interface.vm_nic.private_ip_address
    vm_public_ip        = "N/A"
  }
}

output "demande_id" {
  value = var.demande_id
}

output "vm_name" {
  value = var.vm_name
}

output "vm_resource_group_name" {
  value = azurerm_resource_group.vm_rg.name
}
