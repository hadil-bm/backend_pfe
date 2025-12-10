# Configuration Terraform principale pour le provisionnement de VMs Azure
terraform {
  required_version = ">= 1.0"
  
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
  # ⚠️ SUPPRESSION DU BLOC CLOUD (incompatible ici)
}

# Provider Azure
provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
  # Pas besoin de mettre subscription_id ici, il est lu depuis les variables d'env du Pod
}

# Resource Group
resource "azurerm_resource_group" "vm_rg" {
  name     = "rg-${var.vm_name}-${var.demande_id}"
  location = var.azure_location

  tags = {
    Environment   = var.environment
    ManagedBy     = "VM-Provisioning-System"
    Application   = var.application_name
    DemandeID     = var.demande_id
  }
}

# Virtual Network
resource "azurerm_virtual_network" "vm_vnet" {
  count               = var.create_vnet ? 1 : 0
  name                = "vnet-${var.vm_name}"
  address_space       = [var.vnet_address_space]
  location            = azurerm_resource_group.vm_rg.location
  resource_group_name = azurerm_resource_group.vm_rg.name

  tags = {
    DemandeID = var.demande_id
  }
}

# Subnet
resource "azurerm_subnet" "vm_subnet" {
  count                = var.create_vnet ? 1 : 0
  name                 = "subnet-${var.vm_name}"
  resource_group_name  = azurerm_resource_group.vm_rg.name
  virtual_network_name = azurerm_virtual_network.vm_vnet[0].name
  address_prefixes     = [var.subnet_address_prefix]
}

# Network Security Group
resource "azurerm_network_security_group" "vm_nsg" {
  name                = "nsg-${var.vm_name}"
  location            = azurerm_resource_group.vm_rg.location
  resource_group_name = azurerm_resource_group.vm_rg.name

  dynamic "security_rule" {
    for_each = var.firewall_rules
    content {
      name                       = security_rule.value.name
      priority                   = security_rule.value.priority
      direction                  = "Inbound"
      access                     = "Allow"
      protocol                   = security_rule.value.protocol
      source_port_range          = "*"
      destination_port_range     = security_rule.value.destination_port_range
      source_address_prefix      = security_rule.value.source_address_prefix
      destination_address_prefix = "*"
      description                = security_rule.value.description
    }
  }

  tags = {
    DemandeID = var.demande_id
  }
}

# Public IP
resource "azurerm_public_ip" "vm_public_ip" {
  count               = var.assign_public_ip ? 1 : 0
  name                = "pip-${var.vm_name}"
  location            = azurerm_resource_group.vm_rg.location
  resource_group_name = azurerm_resource_group.vm_rg.name
  allocation_method   = "Static"
  sku                 = "Standard"

  tags = {
    DemandeID = var.demande_id
  }
}

# Network Interface
resource "azurerm_network_interface" "vm_nic" {
  name                = "nic-${var.vm_name}"
  location            = azurerm_resource_group.vm_rg.location
  resource_group_name = azurerm_resource_group.vm_rg.name

  ip_configuration {
    name                          = "internal"
    subnet_id                     = var.subnet_id != "" ? var.subnet_id : (var.create_vnet ? azurerm_subnet.vm_subnet[0].id : null)
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = var.assign_public_ip ? azurerm_public_ip.vm_public_ip[0].id : null
  }

  tags = {
    DemandeID = var.demande_id
  }
}

# Association NSG <-> NIC
resource "azurerm_network_interface_security_group_association" "vm_nic_nsg" {
  network_interface_id      = azurerm_network_interface.vm_nic.id
  network_security_group_id = azurerm_network_security_group.vm_nsg.id
}

# VM Linux
resource "azurerm_linux_virtual_machine" "vm" {
  count               = var.os_type != "Windows" ? 1 : 0
  name                = var.vm_name
  location            = azurerm_resource_group.vm_rg.location
  resource_group_name = azurerm_resource_group.vm_rg.name
  size                = var.vm_size
  admin_username      = var.admin_username

  network_interface_ids = [
    azurerm_network_interface.vm_nic.id,
  ]

  admin_ssh_key {
    username   = var.admin_username
    public_key = var.ssh_public_key != "" ? var.ssh_public_key : file("~/.ssh/id_rsa.pub") # Fallback si vide
  }

  os_disk {
    name                 = "osdisk-${var.vm_name}"
    caching              = "ReadWrite"
    storage_account_type = var.disk_type
    disk_size_gb         = var.disk_size
  }

  source_image_reference {
    publisher = var.image_publisher
    offer     = var.image_offer
    sku       = var.image_sku
    version   = "latest"
  }

  custom_data = var.user_data != "" ? base64encode(var.user_data) : null

  tags = {
    Name        = var.vm_name
    DemandeID   = var.demande_id
    Environment = var.environment
  }
}

# VM Windows
resource "azurerm_windows_virtual_machine" "vm_windows" {
  count               = var.os_type == "Windows" ? 1 : 0
  name                = var.vm_name
  location            = azurerm_resource_group.vm_rg.location
  resource_group_name = azurerm_resource_group.vm_rg.name
  size                = var.vm_size
  admin_username      = var.admin_username
  admin_password      = var.admin_password

  network_interface_ids = [
    azurerm_network_interface.vm_nic.id,
  ]

  os_disk {
    name                 = "osdisk-${var.vm_name}"
    caching              = "ReadWrite"
    storage_account_type = var.disk_type
    disk_size_gb         = var.disk_size
  }

  source_image_reference {
    publisher = var.image_publisher
    offer     = var.image_offer
    sku       = var.image_sku
    version   = "latest"
  }

  tags = {
    Name        = var.vm_name
    DemandeID   = var.demande_id
    Environment = var.environment
  }
}
