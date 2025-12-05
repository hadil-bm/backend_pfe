# Configuration Azure pour Terraform

## 🔧 Configuration requise

### 1. Authentification Azure

Vous devez configurer l'authentification Azure pour Terraform. Plusieurs options :

#### Option A : Service Principal (Recommandé pour Terraform Cloud)

```bash
# Créer un Service Principal
az ad sp create-for-rbac --role="Contributor" --scopes="/subscriptions/YOUR_SUBSCRIPTION_ID"

# Cela retourne :
# {
#   "appId": "xxxx-xxxx-xxxx",
#   "password": "xxxx-xxxx-xxxx",
#   "tenant": "xxxx-xxxx-xxxx"
# }
```

Configurez ces valeurs dans Terraform Cloud comme variables d'environnement :
- `ARM_CLIENT_ID` = appId
- `ARM_CLIENT_SECRET` = password
- `ARM_SUBSCRIPTION_ID` = votre subscription ID
- `ARM_TENANT_ID` = tenant

#### Option B : Azure CLI (pour développement local)

```bash
az login
az account set --subscription "YOUR_SUBSCRIPTION_ID"
```

### 2. Configuration dans Terraform Cloud

Dans votre workspace Terraform Cloud, ajoutez les variables suivantes :

**Variables d'environnement :**
- `ARM_CLIENT_ID` (sensitive)
- `ARM_CLIENT_SECRET` (sensitive)
- `ARM_SUBSCRIPTION_ID`
- `ARM_TENANT_ID`

**Variables Terraform :**
- `azure_location` = "West Europe" (ou votre région)
- `terraform_organization` = votre organisation
- `terraform_workspace` = votre workspace

## 📋 Tailles de VM Azure disponibles

Les tailles de VM sont mappées automatiquement selon CPU/RAM :

| CPU | RAM | Taille Azure |
|-----|-----|--------------|
| ≤2  | ≤4  | Standard_B2s |
| ≤2  | ≤8  | Standard_B2ms |
| ≤4  | ≤8  | Standard_B4ms |
| ≤4  | ≤16 | Standard_D2s_v3 |
| ≤8  | ≤16 | Standard_D4s_v3 |
| ≤8  | ≤32 | Standard_D8s_v3 |
| >8  | >32 | Standard_D16s_v3 |

## 🖼️ Images Azure disponibles

### Ubuntu
- Publisher: `Canonical`
- Offer: `UbuntuServer`
- SKU: `22.04-LTS`, `20.04-LTS`, `18.04-LTS`

### Windows Server
- Publisher: `MicrosoftWindowsServer`
- Offer: `WindowsServer`
- SKU: `2019-Datacenter`, `2022-Datacenter`

### CentOS
- Publisher: `OpenLogic`
- Offer: `CentOS`
- SKU: `8_5`, `7_9`

### Debian
- Publisher: `Debian`
- Offer: `debian-11`
- SKU: `11`

### Red Hat Enterprise Linux
- Publisher: `RedHat`
- Offer: `RHEL`
- SKU: `8.5`, `8.6`

## 🔐 Types de disques Azure

- `Premium_LRS` : SSD Premium (recommandé pour production)
- `Standard_LRS` : HDD Standard (économique)
- `StandardSSD_LRS` : SSD Standard (bon compromis)
- `Premium_ZRS` : SSD Premium avec réplication de zone
- `UltraSSD_LRS` : Ultra SSD (haute performance)

## 🌍 Régions Azure disponibles

Quelques exemples de régions :
- `West Europe` (Pays-Bas)
- `North Europe` (Irlande)
- `East US` (Virginie, USA)
- `West US` (Californie, USA)
- `France Central` (France)
- `UK South` (Royaume-Uni)

## 📝 Exemple de configuration complète

```hcl
# terraform.tfvars
terraform_organization = "my-org"
terraform_workspace    = "vm-provisioning"

azure_location = "West Europe"
environment    = "production"

vm_name    = "my-vm"
demande_id = "demande-123"

vm_size         = "Standard_B2s"
image_publisher = "Canonical"
image_offer     = "UbuntuServer"
image_sku       = "22.04-LTS"

os_type    = "Ubuntu"
os_version = "22.04"

cpu_cores = 2
ram_gb    = 4

disk_size = 30
disk_type = "Premium_LRS"

subnet_id        = ""
create_vnet      = true
assign_public_ip = true
```

## 🚀 Test de la configuration

```bash
cd terraform

# Initialiser avec Azure
terraform init

# Vérifier la configuration
terraform validate

# Planifier
terraform plan -var-file="terraform.tfvars"

# Appliquer
terraform apply -var-file="terraform.tfvars"
```

## 🔍 Vérification des ressources créées

```bash
# Lister les VMs
az vm list --output table

# Voir les détails d'une VM
az vm show --resource-group rg-my-vm-demande-123 --name my-vm

# Voir l'IP publique
az vm show -d --resource-group rg-my-vm-demande-123 --name my-vm --query publicIps -o tsv
```

## 📚 Documentation Azure

- [Terraform Azure Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Azure VM Sizes](https://docs.microsoft.com/en-us/azure/virtual-machines/sizes)
- [Azure Images](https://docs.microsoft.com/en-us/azure/virtual-machines/linux/cli-ps-findimage)
- [Azure Networking](https://docs.microsoft.com/en-us/azure/virtual-network/)

