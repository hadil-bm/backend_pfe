# 🚀 Guide de démarrage rapide - Terraform Local

## ✅ Solution implémentée

Votre application Spring Boot exécute maintenant **Terraform localement** sur la VM backend au lieu d'utiliser Terraform Cloud.

## 📋 Étapes d'installation (sur la VM backend)

### 1. Installer Terraform

```bash
# Sur Ubuntu/Debian
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform

# Vérifier
terraform version
```

### 2. Configurer Azure (Service Principal)

```bash
# Créer un Service Principal
az ad sp create-for-rbac --role="Contributor" --scopes="/subscriptions/YOUR_SUBSCRIPTION_ID"

# Exporter les variables (ajouter dans /etc/environment pour persistance)
export ARM_CLIENT_ID="xxxx-xxxx-xxxx"
export ARM_CLIENT_SECRET="xxxx-xxxx-xxxx"
export ARM_TENANT_ID="xxxx-xxxx-xxxx"
export ARM_SUBSCRIPTION_ID="xxxx-xxxx-xxxx"

# Ajouter dans /etc/environment
sudo nano /etc/environment
# Ajouter:
ARM_CLIENT_ID=xxxx-xxxx-xxxx
ARM_CLIENT_SECRET=xxxx-xxxx-xxxx
ARM_TENANT_ID=xxxx-xxxx-xxxx
ARM_SUBSCRIPTION_ID=xxxx-xxxx-xxxx
```

### 3. Créer le répertoire Terraform

```bash
sudo mkdir -p /opt/terraform/vm-template/executions
sudo chown -R $USER:$USER /opt/terraform/vm-template

# Copier les fichiers template (optionnel, ils seront générés automatiquement)
# cp terraform/main.tf.simple /opt/terraform/vm-template/
# cp terraform/variables.tf.simple /opt/terraform/vm-template/
# cp terraform/outputs.tf.simple /opt/terraform/vm-template/
```

### 4. Générer la clé SSH

```bash
# Générer la clé SSH si elle n'existe pas
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ""

# Copier dans le répertoire Terraform
cp ~/.ssh/id_rsa.pub /opt/terraform/vm-template/id_rsa.pub
```

### 5. Configurer application.properties

```properties
# Terraform Local Configuration
terraform.local.path=/opt/terraform/vm-template
terraform.local.working.dir=/opt/terraform/vm-template
terraform.binary.path=terraform
```

### 6. Redémarrer Spring Boot

```bash
# Redémarrer l'application pour charger les nouvelles configurations
sudo systemctl restart your-spring-boot-app
# ou
./mvnw spring-boot:run
```

## 🔄 Workflow automatique

1. **Client soumet une demande** → `POST /api/demandes/demandeur`
2. **Équipe Cloud valide** → `POST /api/cloud-team/demandes/{id}/valider`
3. **WorkOrder créé automatiquement**
4. **Équipe Support démarre le provisionnement** → `POST /api/support-system/workorders/{id}/demarrer`
5. **Terraform s'exécute automatiquement** :
   - Crée un répertoire unique : `/opt/terraform/vm-template/executions/{executionId}/`
   - Génère `main.tf`, `variables.tf`, `outputs.tf`, `terraform.tfvars`
   - Exécute `terraform init`
   - Exécute `terraform apply -auto-approve`
   - Récupère les outputs (IP, ID, etc.)
   - Crée la VM dans la base de données
   - Envoie des notifications

## 📁 Structure des fichiers générés

Pour chaque exécution :

```
/opt/terraform/vm-template/executions/{executionId}/
├── main.tf              (généré depuis template)
├── variables.tf         (généré depuis template)
├── outputs.tf          (généré depuis template)
├── terraform.tfvars    (généré avec les valeurs de la demande)
├── id_rsa.pub          (copié depuis template)
├── .terraform/         (après terraform init)
└── terraform.tfstate  (après terraform apply)
```

## 🔍 Vérification

### Tester manuellement

```bash
# Vérifier qu'une exécution a été créée
ls -la /opt/terraform/vm-template/executions/

# Aller dans un répertoire d'exécution
cd /opt/terraform/vm-template/executions/{executionId}

# Vérifier les fichiers
ls -la

# Tester terraform
terraform init
terraform plan
terraform output
```

### Vérifier via l'API

```bash
# Vérifier le statut d'une exécution
GET /api/terraform/local/executions/{executionId}/status

# Voir les logs dans MongoDB
# Collection: terraform_executions
```

## 🐛 Dépannage

### Erreur : "terraform: command not found"
```bash
# Vérifier l'installation
which terraform
terraform version

# Réinstaller si nécessaire
sudo apt install terraform
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
sudo chmod 755 /opt/terraform/vm-template
```

### Erreur : "id_rsa.pub not found"
```bash
# Générer et copier la clé SSH
ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ""
cp ~/.ssh/id_rsa.pub /opt/terraform/vm-template/id_rsa.pub
```

### Erreur : "Resource already exists"
- La VM avec ce nom existe déjà dans Azure
- Changer le nom de la VM dans la demande

## 📊 Monitoring

### Logs Spring Boot

Les logs Terraform sont affichés dans la console Spring Boot :
```
INFO  TerraformLocalService - Démarrage de la création de VM pour la demande: xxx
INFO  TerraformLocalService - Exécution de terraform init...
INFO  TerraformLocalService - Terraform: Initializing provider plugins...
INFO  TerraformLocalService - Exécution de terraform apply...
INFO  TerraformLocalService - Output vm_id = /subscriptions/.../resourceGroups/.../providers/Microsoft.Compute/virtualMachines/...
```

### Base de données

Vérifier dans MongoDB :
```javascript
db.terraform_executions.find().sort({dateCreation: -1}).limit(5)
```

### Azure Portal

Vérifier les ressources créées :
```bash
az vm list --output table
az resource list --resource-group rg-{vm-name}-{demande-id} --output table
```

## ✅ Checklist de vérification

- [ ] Terraform installé et accessible (`terraform version`)
- [ ] Variables d'environnement Azure configurées (`echo $ARM_CLIENT_ID`)
- [ ] Répertoire `/opt/terraform/vm-template` créé avec permissions
- [ ] Clé SSH `id_rsa.pub` présente dans `/opt/terraform/vm-template/`
- [ ] `application.properties` configuré avec les chemins Terraform
- [ ] Spring Boot redémarré
- [ ] Test de création d'une VM via l'API

## 🎯 Prochaines étapes

1. Tester la création d'une VM complète via l'interface
2. Vérifier les logs et les outputs
3. Configurer le monitoring des VMs créées
4. Mettre en place des alertes en cas d'échec

## 📚 Documentation

- [TERRAFORM_LOCAL_SETUP.md](TERRAFORM_LOCAL_SETUP.md) - Guide détaillé
- [AZURE_SETUP.md](AZURE_SETUP.md) - Configuration Azure
- [API_ENDPOINTS.md](API_ENDPOINTS.md) - Documentation API

