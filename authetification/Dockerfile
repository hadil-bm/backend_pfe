# Nesta3mlou version Ubuntu (Jammy) fiha Java 21 pre-installé
FROM eclipse-temurin:21-jdk-jammy

# 1. Mise à jour et installation des outils de base (curl, unzip, etc.)
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    gnupg \
    software-properties-common \
    lsb-release \
    && rm -rf /var/lib/apt/lists/*

# 2. Installation de Terraform
# - Ajout de la clé GPG HashiCorp
# - Ajout du repository
# - Installation
RUN curl -fsSL https://apt.releases.hashicorp.com/gpg | gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | tee /etc/apt/sources.list.d/hashicorp.list && \
    apt-get update && apt-get install -y terraform

# 3. Installation de Azure CLI (az)
RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash

# 4. Configuration de l'application
WORKDIR /app

# Copier le JAR
COPY target/*.jar app.jar

# 5. Créer un dossier pour les fichiers Terraform (bech t7ot fihom tes fichiers .tf)
# On suppose que tu as mis tes fichiers .tf dans src/main/resources/terraform
RUN mkdir -p /app/terraform

# (Optionnel) Copier les fichiers .tf s'ils ne sont pas dans le JAR
# COPY src/main/resources/terraform/*.tf /app/terraform/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
