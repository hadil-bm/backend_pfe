# Configuration Terraform Local

## 🎯 Architecture

```
Frontend → Spring Boot API → Terraform Local (sur la VM backend) → Azure → VM créée
```

## 📋 Prérequis sur la VM Backend

### 1. Installer Terraform

```bash
# Sur Ubuntu/Debian
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform

# Vérifier l'installation
terraform version
```

### 2. Configurer les credentials Azure

#### Option A : Service Principal (Recommandé)

```bash
# Créer un Service Principal
az ad sp create-for-rbac --role="Contributor" --scopes="/subscriptions/YOUR_SUBSCRIPTION_ID"

# Exporter les variables d'environnement
export ARM_CLIENT_ID="xxxx-xxxx-xxxx"
export ARM_CLIENT_SECRET="xxxx-xxxx-xxxx"
export ARM_TENANT_ID="xxxx-xxxx-xxxx"
export ARM_SUBSCRIPTION_ID="xxxx-xxxx-xxxx"

# Ajouter dans /etc/environment pour persistance
sudo nano /etc/environment
# Ajouter les lignes :
ARM_CLIENT_ID=xxxx-xxxx-xxxx
ARM_CLIENT_SECRET=xxxx-xxxx-xxxx
ARM_TENANT_ID=xxxx-xxxx-xxxx
ARM_SUBSCRIPTION_ID=xxxx-xxxx-xxxx
```

#### Option B : Azure CLI

```bash
az login
az account set --subscription "YOUR_SUBSCRIPTION_ID"
```

### 3. Créer le répertoire Terraform

```bash
sudo mkdir -p /opt/terraform/vm-template
sudo chown $USER:$USER /opt/terraform/vm-template

# Copier les fichiers Terraform
cp terraform/main.tf /opt/terraform/vm-template/
cp terraform/variables.tf /opt/terraform/vm-template/
cp terraform/outputs.tf /opt/terraform/vm-template/

# Créer le répertoire pour les exécutions
mkdir -p /opt/terraform/vm-template/executions
```

### 4. Créer la clé SSH (pour Linux VMs)

```bash
# Générer une clé SSH si elle n'existe pas
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ""

# Copier la clé publique dans le répertoire Terraform
cp ~/.ssh/id_rsa.pub /opt/terraform/vm-template/id_rsa.pub
```

### 5. Permissions

```bash
# Donner les permissions au processus Spring Boot
sudo chmod 755 /opt/terraform/vm-template
sudo chmod 755 /opt/terraform/vm-template/executions
```

## ⚙️ Configuration dans application.properties

```properties
# Terraform Local Configuration
terraform.local.path=/opt/terraform/vm-template
terraform.local.working.dir=/opt/terraform/vm-template
terraform.binary.path=terraform
```

## 🔄 Workflow

1. **Équipe Support démarre le provisionnement** :
   ```
   POST /api/support-system/workorders/{id}/demarrer
   ```

2. **Le système exécute automatiquement** :
   - Crée un répertoire unique pour cette exécution : `/opt/terraform/vm-template/executions/{executionId}/`
   - Génère `main.tf`, `variables.tf`, `outputs.tf`, `terraform.tfvars`
   - Exécute `terraform init`
   - Exécute `terraform apply -auto-approve`
   - Récupère les outputs
   - Crée la VM dans la base de données

3. **Logs** :
   - Tous les logs Terraform sont capturés et stockés dans `TerraformExecution`
   - Accessibles via l'API

## 📝 Structure des fichiers générés

Pour chaque exécution, un répertoire est créé :

```
/opt/terraform/vm-template/executions/{executionId}/
├── main.tf
├── variables.tf
├── outputs.tf
├── terraform.tfvars
├── .terraform/          (après terraform init)
├── terraform.tfstate    (après terraform apply)
└── terraform.tfstate.backup
```

## 🔍 Vérification

### Tester manuellement

```bash
cd /opt/terraform/vm-template/executions/{executionId}
terraform init
terraform plan
terraform apply -auto-approve
terraform output
```

### Vérifier les logs

Les logs sont disponibles dans :
- Console Spring Boot
- Base de données MongoDB (collection `terraform_executions`)
- Fichiers dans `/opt/terraform/vm-template/executions/{executionId}/`

## 🐛 Dépannage

### Erreur : "terraform: command not found"
```bash
# Vérifier que Terraform est installé
which terraform
terraform version

# Si non installé, suivre les étapes d'installation ci-dessus
```

### Erreur : "Authentication failed"
```bash
# Vérifier les variables d'environnement
echo $ARM_CLIENT_ID
echo $ARM_CLIENT_SECRET
echo $ARM_TENANT_ID
echo $ARM_SUBSCRIPTION_ID

# Tester la connexion Azure
az account show
```

### Erreur : "Permission denied"
```bash
# Vérifier les permissions
ls -la /opt/terraform/vm-template
sudo chown -R $USER:$USER /opt/terraform/vm-template
```

### Erreur : "Resource already exists"
- La VM avec ce nom existe déjà dans Azure
- Changer le nom de la VM dans la demande

## 📊 Monitoring

### Vérifier le statut d'une exécution

```bash
# Via l'API
GET /api/terraform/local/executions/{executionId}/status

# Directement dans Azure
az vm list --output table
az vm show --resource-group rg-{vm-name}-{demande-id} --name {vm-name}
```

## 🔐 Sécurité

- Les credentials Azure sont stockés dans les variables d'environnement (jamais dans le code)
- Les fichiers `terraform.tfvars` contiennent des informations sensibles et ne doivent pas être commités
- Utiliser un Service Principal avec des permissions limitées (Contributor uniquement sur le Resource Group)

## 📚 Documentation

- [Terraform Azure Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Terraform CLI](https://www.terraform.io/docs/cli)

