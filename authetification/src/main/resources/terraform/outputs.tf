output "vm_id" {
  value = var.os_type == "Linux" ? azurerm_linux_virtual_machine.vm[0].id : ""
}

output "vm_private_ip" {
  value = azurerm_network_interface.vm_nic.private_ip_address
}

# On met une chaine vide ou "N/A" car il n'y a plus d'IP publique
output "vm_public_ip" {
  value = "N/A"
}

# Idem pour le FQDN
output "vm_fqdn" {
  value = "N/A"
}

output "provisioning_info" {
  value = {
    vm_name             = var.vm_name
    resource_group_name = azurerm_resource_group.vm_rg.name
    vm_id               = var.os_type == "Linux" ? azurerm_linux_virtual_machine.vm[0].id : ""
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
