# Configuration Terraform principale pour le provisionnement de VMs Azure
# Ce fichier est généré automatiquement par l'application

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }

  # Configuration pour Terraform Cloud
  cloud {
    organization = var.terraform_organization
    workspaces {
      name = var.terraform_workspace
    }
  }
}

# Provider Azure
provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }
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

# Virtual Network (si elle n'existe pas déjà)
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

# Network Security Group avec règles de pare-feu
resource "azurerm_network_security_group" "vm_nsg" {
  name                = "nsg-${var.vm_name}"
  location            = azurerm_resource_group.vm_rg.location
  resource_group_name = azurerm_resource_group.vm_rg.name

  # Règles de pare-feu dynamiques basées sur les besoins de la demande
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

  # Règle de trafic sortant par défaut
  security_rule {
    name                       = "AllowAllOutbound"
    priority                   = 1000
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
    description                = "Allow all outbound traffic"
  }

  tags = {
    DemandeID = var.demande_id
  }
}

# Public IP (si nécessaire)
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

# Association du Network Security Group au Network Interface
resource "azurerm_network_interface_security_group_association" "vm_nic_nsg" {
  network_interface_id      = azurerm_network_interface.vm_nic.id
  network_security_group_id = azurerm_network_security_group.vm_nsg.id
}

# Virtual Machine (VM Azure)
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
    public_key = var.ssh_public_key
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

  # User data pour configuration initiale
  custom_data = var.user_data != "" ? base64encode(var.user_data) : null

  # Monitoring
  boot_diagnostics {
    storage_account_uri = var.enable_monitoring ? azurerm_storage_account.vm_diagnostics[0].primary_blob_endpoint : null
  }

  tags = {
    Name        = var.vm_name
    DemandeID   = var.demande_id
    Environment = var.environment
    OS          = "${var.os_type}-${var.os_version}"
    CPU         = var.cpu_cores
    RAM         = "${var.ram_gb}GB"
    Disk        = "${var.disk_size}GB"
  }
}

# Virtual Machine Windows (si OS Windows)
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

  # User data pour configuration initiale
  custom_data = var.user_data != "" ? base64encode(var.user_data) : null

  # Monitoring
  boot_diagnostics {
    storage_account_uri = var.enable_monitoring ? azurerm_storage_account.vm_diagnostics[0].primary_blob_endpoint : null
  }

  tags = {
    Name        = var.vm_name
    DemandeID   = var.demande_id
    Environment = var.environment
    OS          = "${var.os_type}-${var.os_version}"
    CPU         = var.cpu_cores
    RAM         = "${var.ram_gb}GB"
    Disk        = "${var.disk_size}GB"
  }
}

# Storage Account pour les diagnostics (si monitoring activé)
resource "azurerm_storage_account" "vm_diagnostics" {
  count                    = var.enable_monitoring ? 1 : 0
  name                     = "diag${replace(var.vm_name, "-", "")}${substr(var.demande_id, 0, 8)}"
  resource_group_name      = azurerm_resource_group.vm_rg.name
  location                 = azurerm_resource_group.vm_rg.location
  account_tier             = "Standard"
  account_replication_type = "LRS"

  tags = {
    DemandeID = var.demande_id
  }
}

# Disque de données additionnel (si nécessaire)
resource "azurerm_managed_disk" "additional_storage" {
  count                = var.additional_storage_size > 0 ? 1 : 0
  name                 = "datadisk-${var.vm_name}"
  location             = azurerm_resource_group.vm_rg.location
  resource_group_name  = azurerm_resource_group.vm_rg.name
  storage_account_type = var.disk_type
  create_option        = "Empty"
  disk_size_gb         = var.additional_storage_size

  tags = {
    DemandeID = var.demande_id
  }
}

# Attachement du disque de données
resource "azurerm_virtual_machine_data_disk_attachment" "additional_storage_attachment" {
  count              = var.additional_storage_size > 0 ? 1 : 0
  managed_disk_id    = azurerm_managed_disk.additional_storage[0].id
  virtual_machine_id = var.os_type != "Windows" ? azurerm_linux_virtual_machine.vm[0].id : azurerm_windows_virtual_machine.vm_windows[0].id
  lun                = "0"
  caching            = "ReadWrite"
}

