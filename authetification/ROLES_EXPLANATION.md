# Explication des Rôles dans l'Application

## 📍 Où sont définis les rôles ?

### 1. **Modèle User** (`User.java`)
```java
private List<String> roles;  // Liste des rôles de l'utilisateur
```

Les rôles sont stockés dans la base de données MongoDB dans la collection `users`.

### 2. **Configuration de sécurité** (`SecurityConfig.java`)
Les rôles sont utilisés pour contrôler l'accès aux endpoints :

```java
// Routes pour les demandeurs (clients internes)
.requestMatchers("/api/demandes/demandeur/**").hasAnyRole("DEMANDEUR", "ADMIN")

// Routes pour l'équipe Cloud
.requestMatchers("/api/cloud-team/**").hasAnyRole("EQUIPECLOUD", "ADMIN")

// Routes pour l'équipe Support Système
.requestMatchers("/api/support-system/**").hasAnyRole("EQUIPESUPPORT", "ADMIN")

// Routes pour les administrateurs
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

## 🔐 Les 4 rôles disponibles

### 1. **ROLE_DEMANDEUR** (Client interne)
- **Classe** : `User.java` (champ `roles`)
- **Utilisation** : `SecurityConfig.java` ligne 67-68
- **Permissions** :
  - Créer des demandes de VM
  - Voir ses propres demandes
  - Modifier/supprimer ses demandes

### 2. **ROLE_EQUIPECLOUD** (Équipe Cloud)
- **Classe** : `User.java` (champ `roles`)
- **Utilisation** : `SecurityConfig.java` ligne 70
- **Permissions** :
  - Valider/refuser les demandes
  - Ajouter les informations techniques (IP, réseau, datastore)
  - Voir toutes les demandes en attente

### 3. **ROLE_EQUIPESUPPORT** (Équipe Support Système)
- **Classe** : `User.java` (champ `roles`)
- **Utilisation** : `SecurityConfig.java` ligne 72
- **Permissions** :
  - Gérer les workorders
  - Créer les VMs
  - Exécuter le provisionnement
  - **Déclencher Terraform automatiquement** ⚡

### 4. **ROLE_ADMIN** (Administrateurs)
- **Classe** : `User.java` (champ `roles`)
- **Utilisation** : `SecurityConfig.java` ligne 74
- **Permissions** :
  - Accès complet à tous les endpoints
  - Gestion des utilisateurs et rôles
  - Configuration des règles de gouvernance
  - Tableau de bord administrateur

## 📝 Format des rôles

Les rôles doivent être stockés avec le préfixe `ROLE_` :
- `ROLE_DEMANDEUR`
- `ROLE_EQUIPECLOUD`
- `ROLE_EQUIPESUPPORT`
- `ROLE_ADMIN`

## 🔄 Comment créer un utilisateur avec un rôle ?

### Via l'API
```json
POST /api/auth/register
{
  "username": "john",
  "email": "john@example.com",
  "password": "password123",
  "role": ["DEMANDEUR"]  // ou ["EQUIPECLOUD"], ["EQUIPESUPPORT"], ["ADMIN"]
}
```

### Dans le code (AuthService.java)
```java
List<String> formattedRoles = roles.stream()
    .map(role -> "ROLE_" + role.toUpperCase())
    .collect(Collectors.toList());
newUser.setRoles(formattedRoles);
```

## 📂 Fichiers concernés

1. **`User.java`** : Définition du modèle avec le champ `roles`
2. **`SecurityConfig.java`** : Configuration des autorisations par rôle
3. **`AuthService.java`** : Création d'utilisateurs avec rôles
4. **`AuthController.java`** : Endpoint d'inscription

