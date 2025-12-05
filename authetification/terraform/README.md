# Configuration Terraform pour le Provisionnement de VMs

Ce dossier contient les fichiers de configuration Terraform pour automatiser la création de machines virtuelles via Terraform Cloud.

## 📁 Structure des fichiers

- **`main.tf`** : Configuration principale Terraform (ressources AWS)
- **`variables.tf`** : Définition de toutes les variables
- **`outputs.tf`** : Définition des outputs (valeurs retournées après création)
- **`.gitignore`** : Fichiers à ignorer dans Git

## 🚀 Utilisation

### Configuration Terraform Cloud

1. Créez un workspace dans Terraform Cloud
2. Configurez les variables dans `application.properties` :
   ```properties
   terraform.cloud.organization=your-org
   terraform.cloud.workspace=vm-provisioning
   terraform.cloud.api.token=your-token
   ```

### Configuration Azure

1. **Authentification Azure** : Configurez les credentials Azure dans Terraform Cloud ou via variables d'environnement :
   ```bash
   export ARM_CLIENT_ID="your-client-id"
   export ARM_CLIENT_SECRET="your-client-secret"
   export ARM_SUBSCRIPTION_ID="your-subscription-id"
   export ARM_TENANT_ID="your-tenant-id"
   ```

2. **Ou utilisez Azure CLI** :
   ```bash
   az login
   az account set --subscription "your-subscription-id"
   ```

### Génération automatique

L'application génère automatiquement les fichiers Terraform à partir d'une demande. Le service `TerraformService` crée la configuration en utilisant les informations de la demande.

### Exécution manuelle

Si vous voulez tester manuellement :

```bash
cd terraform

# Initialiser Terraform
terraform init

# Planifier les changements
terraform plan -var-file="terraform.tfvars"

# Appliquer les changements
terraform apply -var-file="terraform.tfvars"

# Détruire les ressources
terraform destroy -var-file="terraform.tfvars"
```

## 📝 Variables requises

Les variables suivantes doivent être fournies (via Terraform Cloud ou fichier `.tfvars`) :

- `vm_name` : Nom de la VM
- `demande_id` : ID de la demande
- `vm_size` : Taille de la VM Azure (ex: Standard_B2s)
- `image_publisher` : Publisher de l'image (ex: Canonical, MicrosoftWindowsServer)
- `image_offer` : Offer de l'image (ex: UbuntuServer, WindowsServer)
- `image_sku` : SKU de l'image (ex: 22.04-LTS, 2019-Datacenter)
- `os_type` : Type d'OS
- `os_version` : Version de l'OS
- `subnet_id` : ID du subnet Azure (optionnel, peut être créé automatiquement)

### Ajouter des ressources supplémentaires

Vous pouvez ajouter d'autres ressources dans `main.tf` :
- Load balancers
- Databases
- Storage buckets
- etc.

## 📊 Outputs disponibles

Après l'exécution, les outputs suivants sont disponibles :

- `vm_id` : ID de la VM Azure
- `vm_public_ip` : IP publique
- `vm_private_ip` : IP privée
- `vm_fqdn` : FQDN (Fully Qualified Domain Name)
- `vm_location` : Localisation Azure
- `vm_size` : Taille de la VM
- `vm_resource_group_name` : Nom du Resource Group
- `vm_network_security_group_id` : ID du NSG
- `provisioning_info` : Toutes les informations en JSON

Ces outputs sont automatiquement récupérés par l'application et stockés dans la base de données.

## 🔐 Sécurité

- Les tokens Terraform Cloud doivent être stockés de manière sécurisée
- Ne commitez jamais les fichiers `.tfvars` contenant des secrets
- Utilisez Terraform Cloud pour stocker les variables sensibles

## 📚 Documentation

- [Terraform Azure Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Azure VM Sizes](https://docs.microsoft.com/en-us/azure/virtual-machines/sizes)
- [Azure Images](https://docs.microsoft.com/en-us/azure/virtual-machines/linux/cli-ps-findimage)
- [Terraform Cloud](https://www.terraform.io/cloud-docs)

## 🔄 Migration depuis AWS

Si vous migrez depuis AWS, notez les différences principales :

- **Instance Type** : `t3.medium` (AWS) → `Standard_B2s` (Azure)
- **AMI** : `ami-xxx` (AWS) → `publisher:offer:sku` (Azure)
- **Security Group** : `aws_security_group` → `azurerm_network_security_group`
- **Elastic IP** : `aws_eip` → `azurerm_public_ip`
- **EBS Volume** : `aws_ebs_volume` → `azurerm_managed_disk`

