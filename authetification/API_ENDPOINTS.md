# Documentation des Endpoints API

## URL de base
```
http://localhost:8080/api
```

## Endpoints publics (pas d'authentification requise)

### 1. Health Check
- **GET** `/api/health`
- **Description**: Vérifie que l'API fonctionne
- **Réponse**: 
```json
{
  "status": "UP",
  "message": "API is running",
  "timestamp": 1234567890
}
```

### 2. Informations API
- **GET** `/api/info`
- **Description**: Retourne les informations sur l'API
- **Réponse**: Informations sur l'API et ses endpoints

### 3. Authentification

#### Inscription
- **POST** `/api/auth/register`
- **Body**:
```json
{
  "username": "nom_utilisateur",
  "email": "email@example.com",
  "password": "mot_de_passe",
  "role": "DEMANDEUR" // ou "EQUIPECLOUD", "EQUIPESUPPORT", "ADMIN"
}
```

#### Connexion
- **POST** `/api/auth/login`
- **Body**:
```json
{
  "username": "nom_utilisateur",
  "password": "mot_de_passe"
}
```
- **Réponse**:
```json
{
  "message": "Connexion réussie pour l'utilisateur : nom_utilisateur",
  "token": "jwt_token_here"
}
```

#### Mot de passe oublié
- **POST** `/api/auth/forgot-password`
- **Body**:
```json
{
  "email": "email@example.com"
}
```

#### Réinitialisation du mot de passe
- **POST** `/api/auth/reset-password`
- **Body**:
```json
{
  "token": "reset_token",
  "newPassword": "nouveau_mot_de_passe"
}
```

## Endpoints authentifiés

### Headers requis pour les endpoints authentifiés
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

## Endpoints pour les Demandeurs (ROLE_DEMANDEUR)

### Demandes
- **POST** `/api/demandes/demandeur/create` - Créer une demande
- **GET** `/api/demandes/demandeur/mes-demandes` - Liste des demandes de l'utilisateur
- **GET** `/api/demandes/demandeur/{id}` - Détails d'une demande
- **PUT** `/api/demandes/demandeur/modifier/{id}` - Modifier une demande
- **DELETE** `/api/demandes/demandeur/supprimer/{id}` - Supprimer une demande

## Endpoints pour l'équipe Cloud (ROLE_EQUIPECLOUD)

### Validation des demandes
- **GET** `/api/cloud-team/demandes/en-attente` - Liste des demandes en attente
- **GET** `/api/cloud-team/demandes/en-validation` - Liste des demandes en validation
- **POST** `/api/cloud-team/demandes/{id}/valider` - Valider une demande
- **POST** `/api/cloud-team/demandes/{id}/refuser` - Refuser une demande
- **POST** `/api/cloud-team/demandes/{id}/demander-modification` - Demander des modifications

## Endpoints pour l'équipe Support (ROLE_EQUIPESUPPORT)

### WorkOrders
- **GET** `/api/support-system/workorders/en-attente` - Liste des workorders en attente
- **GET** `/api/support-system/workorders/mes-workorders` - Liste des workorders assignés
- **POST** `/api/support-system/workorders/{id}/assigner` - Assigner un workorder
- **POST** `/api/support-system/workorders/{id}/demarrer` - Démarrer le provisionnement
- **POST** `/api/support-system/workorders/{id}/completer-etape` - Compléter une étape
- **POST** `/api/support-system/workorders/{id}/creer-vm` - Créer une VM
- **POST** `/api/support-system/workorders/{id}/finaliser` - Finaliser le provisionnement
- **POST** `/api/support-system/workorders/{id}/erreur` - Marquer une erreur

### VMs
- **GET** `/api/support-system/vms` - Liste de toutes les VMs
- **GET** `/api/support-system/vms/demande/{demandeId}` - VMs d'une demande
- **PUT** `/api/support-system/vms/{id}/status` - Mettre à jour le statut d'une VM

## Endpoints pour les Administrateurs (ROLE_ADMIN)

### Tableau de bord
- **GET** `/api/admin/dashboard/stats` - Statistiques du tableau de bord

### Règles de gouvernance
- **GET** `/api/admin/governance-rules` - Liste des règles
- **GET** `/api/admin/governance-rules/active` - Règles actives
- **POST** `/api/admin/governance-rules` - Créer une règle
- **PUT** `/api/admin/governance-rules/{id}` - Modifier une règle
- **PUT** `/api/admin/governance-rules/{id}/disable` - Désactiver une règle

### Gestion
- **GET** `/api/admin/demandes` - Liste de toutes les demandes
- **GET** `/api/admin/vms` - Liste de toutes les VMs
- **GET** `/api/admin/users` - Liste de tous les utilisateurs
- **PUT** `/api/admin/users/{id}/roles` - Modifier les rôles d'un utilisateur

### Monitoring
- **GET** `/api/admin/monitoring/metrics/recent` - Métriques récentes
- **GET** `/api/admin/monitoring/alerts` - Alertes

## Endpoints de Monitoring (Authentifiés)

- **POST** `/api/monitoring/metrics/collect` - Collecter des métriques
- **GET** `/api/monitoring/metrics/vm/{vmId}` - Métriques d'une VM
- **GET** `/api/monitoring/metrics/vm/{vmId}/range` - Métriques dans une plage de dates
- **GET** `/api/monitoring/metrics/source/{source}` - Métriques par source
- **GET** `/api/monitoring/alerts` - Liste des alertes
- **GET** `/api/monitoring/metrics/recent` - Métriques récentes
- **POST** `/api/monitoring/vms/{vmId}/enable` - Activer le monitoring
- **POST** `/api/monitoring/vms/{vmId}/disable` - Désactiver le monitoring

## Endpoints de Notifications (Authentifiés)

- **GET** `/api/notifications` - Liste des notifications
- **GET** `/api/notifications/unread/count` - Nombre de notifications non lues
- **PUT** `/api/notifications/mark-as-read/{id}` - Marquer comme lu
- **PUT** `/api/notifications/mark-all-as-read` - Marquer toutes comme lues

## Exemple d'utilisation avec JavaScript/Fetch

```javascript
// 1. Se connecter
const loginResponse = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    username: 'nom_utilisateur',
    password: 'mot_de_passe'
  })
});

const loginData = await loginResponse.json();
const token = loginData.token;

// 2. Faire une requête authentifiée
const response = await fetch('http://localhost:8080/api/demandes/demandeur/mes-demandes', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  }
});

const data = await response.json();
```

## Exemple d'utilisation avec Axios

```javascript
import axios from 'axios';

// Configuration de base
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  }
});

// Ajouter le token aux requêtes
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Exemple d'utilisation
const login = async (username, password) => {
  const response = await api.post('/auth/login', { username, password });
  localStorage.setItem('token', response.data.token);
  return response.data;
};

const getMyDemandes = async () => {
  const response = await api.get('/demandes/demandeur/mes-demandes');
  return response.data;
};
```

## Codes de statut HTTP

- **200 OK**: Requête réussie
- **201 Created**: Ressource créée avec succès
- **400 Bad Request**: Requête invalide
- **401 Unauthorized**: Non authentifié
- **403 Forbidden**: Non autorisé (rôle insuffisant)
- **404 Not Found**: Ressource non trouvée
- **500 Internal Server Error**: Erreur serveur

## Configuration CORS

L'API est configurée pour accepter les requêtes depuis :
- `http://localhost:3000` (React par défaut)
- `http://localhost:4200` (Angular par défaut)
- `http://localhost:5173` (Vite par défaut)
- Toutes les origines en développement (`*`)

Pour la production, modifiez la configuration CORS dans `SecurityConfig.java`.

