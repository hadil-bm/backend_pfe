# Utiliser une image Java légère et récente (Ubuntu Jammy)
FROM eclipse-temurin:17-jdk-jammy

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

# 4. Copier le JAR généré par Maven
COPY target/*.jar app.jar

# 5. Créer le dossier terraform dans le conteneur
RUN mkdir -p /app/terraform

# 6. Copier les fichiers Terraform (Depuis la racine du projet vers /app/terraform)
# ⚠️ Assure-toi que le dossier "terraform" est bien à côté de ce Dockerfile !
COPY terraform/ /app/terraform/

# 7. Donner les droits d'exécution (au cas où)
RUN chmod -R 755 /app/terraform

# 8. Exposer le port
EXPOSE 8080

# 9. Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
