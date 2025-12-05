#!/bin/bash

# Script d'installation Terraform local sur la VM backend
# À exécuter sur la VM où tourne le backend Spring Boot

echo "=========================================="
echo "Installation Terraform Local"
echo "=========================================="
echo ""

# 1. Installer Terraform
echo "1. Installation de Terraform..."
if ! command -v terraform &> /dev/null; then
    # Pour Ubuntu/Debian
    wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
    echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
    sudo apt update && sudo apt install -y terraform
    echo "✅ Terraform installé"
else
    echo "✅ Terraform déjà installé: $(terraform version)"
fi

# 2. Créer le répertoire Terraform
echo ""
echo "2. Création du répertoire Terraform..."
sudo mkdir -p /opt/terraform/vm-template/executions
sudo chown -R $USER:$USER /opt/terraform/vm-template
echo "✅ Répertoire créé: /opt/terraform/vm-template"

# 3. Copier les fichiers Terraform
echo ""
echo "3. Copie des fichiers Terraform..."
if [ -d "terraform" ]; then
    cp terraform/main.tf* /opt/terraform/vm-template/ 2>/dev/null || true
    cp terraform/variables.tf* /opt/terraform/vm-template/ 2>/dev/null || true
    cp terraform/outputs.tf* /opt/terraform/vm-template/ 2>/dev/null || true
    echo "✅ Fichiers Terraform copiés"
else
    echo "⚠️  Dossier terraform/ non trouvé, création des fichiers de base..."
    # Les fichiers seront générés automatiquement par l'application
fi

# 4. Générer la clé SSH
echo ""
echo "4. Génération de la clé SSH..."
if [ ! -f ~/.ssh/id_rsa.pub ]; then
    ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N "" -q
    echo "✅ Clé SSH générée"
else
    echo "✅ Clé SSH existe déjà"
fi

# Copier la clé publique
cp ~/.ssh/id_rsa.pub /opt/terraform/vm-template/id_rsa.pub 2>/dev/null || true
echo "✅ Clé SSH copiée dans /opt/terraform/vm-template/"

# 5. Vérifier Azure CLI
echo ""
echo "5. Vérification Azure CLI..."
if command -v az &> /dev/null; then
    echo "✅ Azure CLI installé: $(az version --output tsv 2>/dev/null | head -1)"
    echo ""
    echo "Pour configurer l'authentification Azure:"
    echo "  az login"
    echo "  az account set --subscription 'YOUR_SUBSCRIPTION_ID'"
else
    echo "⚠️  Azure CLI non installé"
    echo "Installation recommandée:"
    echo "  curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash"
fi

# 6. Configuration des variables d'environnement
echo ""
echo "6. Configuration des variables d'environnement Azure..."
echo ""
echo "IMPORTANT: Configurez les variables d'environnement suivantes:"
echo ""
echo "export ARM_CLIENT_ID='your-client-id'"
echo "export ARM_CLIENT_SECRET='your-client-secret'"
echo "export ARM_TENANT_ID='your-tenant-id'"
echo "export ARM_SUBSCRIPTION_ID='your-subscription-id'"
echo ""
echo "Ou ajoutez-les dans /etc/environment pour persistance:"
echo "sudo nano /etc/environment"
echo ""

# 7. Test de Terraform
echo ""
echo "7. Test de Terraform..."
cd /opt/terraform/vm-template
if terraform version &> /dev/null; then
    echo "✅ Terraform fonctionne correctement"
else
    echo "❌ Erreur avec Terraform"
fi

echo ""
echo "=========================================="
echo "Installation terminée!"
echo "=========================================="
echo ""
echo "Prochaines étapes:"
echo "1. Configurer les variables d'environnement Azure (ARM_*)"
echo "2. Vérifier que Spring Boot peut accéder à /opt/terraform/vm-template"
echo "3. Tester la création d'une VM via l'API"
echo ""

