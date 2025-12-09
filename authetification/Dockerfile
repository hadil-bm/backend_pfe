# Utiliser une image Java légère
FROM openjdk:17-jdk-slim

# 1. Installer les dépendances nécessaires (curl, unzip, wget)
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    gnupg \
    software-properties-common \
    wget \
    && rm -rf /var/lib/apt/lists/*

# 2. Installer Terraform
# (On télécharge le binaire directement pour être sûr de la version)
RUN wget -O- https://apt.releases.hashicorp.com/gpg | gpg --dearmor | tee /usr/share/keyrings/hashicorp-archive-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | tee /etc/apt/sources.list.d/hashicorp.list && \
    apt-get update && apt-get install -y terraform

# 3. Préparer le dossier de l'application
WORKDIR /app

# 4. Copier le JAR généré par Maven (Assure-toi que le build Maven est fait AVANT le docker build)
# Le fichier se trouve généralement dans target/
COPY target/*.jar app.jar

# 5. Copier le dossier Terraform (Code Infrastructure)
# Pour qu'il soit disponible au runtime dans /app/terraform
COPY src/main/resources/terraform /app/terraform

# 6. Exposer le port
EXPOSE 8080

# 7. Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
