# Intégration Terraform Cloud

## 🎯 Objectif

Automatiser la création de machines virtuelles via Terraform Cloud lorsque l'équipe Support démarre le provisionnement.

## 🔄 Workflow

1. **Client interne** crée une demande → Statut: `EN_ATTENTE`
2. **Équipe Cloud** valide la demande → Statut: `VALIDE` → Crée un WorkOrder
3. **Équipe Support** démarre le provisionnement → **Déclenche automatiquement Terraform Cloud** ⚡
4. **Terraform Cloud** exécute le script et crée la VM
5. **Système** récupère les outputs et met à jour la VM dans la base de données

## 📁 Fichiers créés

### Modèles
- **`TerraformExecution.java`** : Modèle pour stocker les exécutions Terraform

### Services
- **`TerraformService.java`** : Service pour gérer les interactions avec Terraform Cloud
  - Génération de configuration Terraform
  - Création de runs Terraform Cloud
  - Vérification du statut
  - Récupération des outputs

### Contrôleurs
- **`TerraformController.java`** : Endpoints pour gérer Terraform manuellement

### Configuration
- **`AsyncConfig.java`** : Configuration pour l'exécution asynchrone
- **`application.properties`** : Configuration Terraform Cloud

## ⚙️ Configuration

### Variables d'environnement

```bash
# Token Terraform Cloud (obtenu depuis https://app.terraform.io/app/settings/tokens)
export TERRAFORM_CLOUD_TOKEN=your-token-here

# Organisation Terraform Cloud
export TERRAFORM_ORGANIZATION=my-org

# Workspace Terraform Cloud
export TERRAFORM_WORKSPACE=vm-provisioning
```

### Configuration dans application.properties

```properties
terraform.cloud.api.url=https://app.terraform.io/api/v2
terraform.cloud.api.token=${TERRAFORM_CLOUD_TOKEN:}
terraform.cloud.organization=${TERRAFORM_ORGANIZATION:my-org}
terraform.cloud.workspace=${TERRAFORM_WORKSPACE:vm-provisioning}
```

## 🚀 Utilisation

### Déclenchement automatique

Quand l'équipe Support démarre le provisionnement :

```java
// Dans SupportSystemService.demarrerProvisionnement()
terraformService.createTerraformRun(workOrderId);
```

Cela va :
1. Générer la configuration Terraform à partir de la demande
2. Créer un run dans Terraform Cloud
3. Exécuter le script Terraform
4. Créer la VM automatiquement

### Déclenchement manuel

```bash
POST /api/terraform/workorders/{workOrderId}/execute
Authorization: Bearer <token>
```

### Vérifier le statut

```bash
GET /api/terraform/executions/{executionId}/status
Authorization: Bearer <token>
```

## 📝 Format de la configuration Terraform générée

La configuration Terraform est générée automatiquement à partir des informations de la demande :

```hcl
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  cloud {
    organization = "my-org"
    workspaces {
      name = "vm-provisioning"
    }
  }
}

resource "aws_instance" "vm_demande_123" {
  ami           = var.ami_id
  instance_type = "t3.medium"  # Basé sur CPU/RAM de la demande
  key_name      = var.key_name
  
  tags = {
    Name      = "VM-Demande-123"
    DemandeID = "123"
  }
  
  root_block_device {
    volume_type = "gp3"
    volume_size = 50  # Basé sur la demande
  }
}

output "vm_ip_demande_123" {
  value = aws_instance.vm_demande_123.public_ip
}
```

## 🔐 Sécurité

- Le token Terraform Cloud doit être stocké de manière sécurisée (variables d'environnement)
- Les endpoints Terraform sont accessibles uniquement à l'équipe Support (`ROLE_EQUIPESUPPORT`) et aux admins

## 📊 Statuts Terraform

- **PENDING** : En attente d'exécution
- **RUNNING** : En cours d'exécution
- **APPLIED** : Appliqué avec succès
- **ERROR** : Erreur lors de l'exécution
- **CANCELLED** : Annulé

## 🔧 Personnalisation

### Changer le provider cloud

Modifiez la méthode `generateTerraformConfig()` dans `TerraformService.java` pour utiliser :
- Azure : `azurerm`
- GCP : `google`
- VMware : `vsphere`

### Adapter le mapping CPU/RAM → Instance Type

Modifiez la méthode `getInstanceType()` dans `TerraformService.java`

### Adapter le mapping OS → AMI ID

Modifiez la méthode `getAmiIdForOS()` dans `TerraformService.java`

## 🐛 Mode Simulation

Si le token Terraform Cloud n'est pas configuré, le système fonctionne en mode simulation :
- Les runs sont créés avec un ID simulé
- Le statut passe automatiquement à `APPLIED`
- Une VM simulée est créée dans la base de données

Cela permet de tester le workflow sans avoir besoin d'un compte Terraform Cloud.

## 📚 Documentation Terraform Cloud API

- API v2 Documentation: https://www.terraform.io/cloud-docs/api-docs
- Runs API: https://www.terraform.io/cloud-docs/api-docs/runs

