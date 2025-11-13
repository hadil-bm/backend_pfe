# Guide de connexion Front-End à l'API

## URL de base de l'API
```
http://localhost:8080/api
```

## Configuration CORS

L'API est configurée pour accepter les requêtes depuis :
- ✅ Toutes les origines en développement (`*`)
- ✅ Headers autorisés : `Authorization`, `Content-Type`, etc.
- ✅ Méthodes autorisées : `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`

## Étapes de connexion

### 1. Vérifier que l'API fonctionne

Testez l'endpoint de santé :
```javascript
fetch('http://localhost:8080/api/health')
  .then(response => response.json())
  .then(data => console.log(data));
```

Réponse attendue :
```json
{
  "status": "UP",
  "message": "API is running",
  "timestamp": 1234567890
}
```

### 2. Authentification

#### Inscription d'un utilisateur
```javascript
const registerUser = async (username, email, password, role) => {
  const response = await fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username: username,
      email: email,
      password: password,
      role: role // "DEMANDEUR", "EQUIPECLOUD", "EQUIPESUPPORT", "ADMIN"
    })
  });
  
  return await response.json();
};
```

#### Connexion
```javascript
const login = async (username, password) => {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username: username,
      password: password
    })
  });
  
  const data = await response.json();
  
  if (response.ok && data.token) {
    // Stocker le token
    localStorage.setItem('token', data.token);
    return data;
  } else {
    throw new Error(data.message || 'Erreur de connexion');
  }
};
```

### 3. Utiliser le token pour les requêtes authentifiées

```javascript
const makeAuthenticatedRequest = async (url, options = {}) => {
  const token = localStorage.getItem('token');
  
  const response = await fetch(`http://localhost:8080/api${url}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...options.headers
    }
  });
  
  if (response.status === 401) {
    // Token invalide ou expiré
    localStorage.removeItem('token');
    // Rediriger vers la page de connexion
    window.location.href = '/login';
    return;
  }
  
  return await response.json();
};

// Exemple d'utilisation
const getMyDemandes = async () => {
  return await makeAuthenticatedRequest('/demandes/demandeur/mes-demandes', {
    method: 'GET'
  });
};
```

## Exemples complets

### React avec Axios

```javascript
import axios from 'axios';

// Configuration de base
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  }
});

// Intercepteur pour ajouter le token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Intercepteur pour gérer les erreurs
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Services
export const authService = {
  login: (username, password) => 
    api.post('/auth/login', { username, password }),
  
  register: (username, email, password, role) =>
    api.post('/auth/register', { username, email, password, role }),
};

export const demandeService = {
  getMyDemandes: () => 
    api.get('/demandes/demandeur/mes-demandes'),
  
  createDemande: (demande) =>
    api.post('/demandes/demandeur/create', demande),
};

export const notificationService = {
  getNotifications: () =>
    api.get('/notifications'),
  
  getUnreadCount: () =>
    api.get('/notifications/unread/count'),
};
```

### Vue.js avec Fetch

```javascript
// api.js
const API_BASE_URL = 'http://localhost:8080/api';

export const api = {
  async request(url, options = {}) {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`${API_BASE_URL}${url}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
        ...options.headers
      }
    });
    
    if (!response.ok) {
      if (response.status === 401) {
        localStorage.removeItem('token');
        window.location.href = '/login';
      }
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return await response.json();
  },
  
  get(url) {
    return this.request(url, { method: 'GET' });
  },
  
  post(url, data) {
    return this.request(url, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },
  
  put(url, data) {
    return this.request(url, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },
  
  delete(url) {
    return this.request(url, { method: 'DELETE' });
  }
};

// Utilisation dans un composant Vue
export default {
  async mounted() {
    try {
      const demandes = await api.get('/demandes/demandeur/mes-demandes');
      this.demandes = demandes;
    } catch (error) {
      console.error('Erreur:', error);
    }
  }
};
```

### Angular avec HttpClient

```typescript
// api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    });
  }

  get<T>(url: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${url}`, {
      headers: this.getHeaders()
    });
  }

  post<T>(url: string, data: any): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${url}`, data, {
      headers: this.getHeaders()
    });
  }

  put<T>(url: string, data: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${url}`, data, {
      headers: this.getHeaders()
    });
  }

  delete<T>(url: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${url}`, {
      headers: this.getHeaders()
    });
  }
}

// auth.service.ts
import { Injectable } from '@angular/core';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private api: ApiService) {}

  login(username: string, password: string) {
    return this.api.post('/auth/login', { username, password }).pipe(
      tap(response => {
        if (response.token) {
          localStorage.setItem('token', response.token);
        }
      })
    );
  }

  register(username: string, email: string, password: string, role: string) {
    return this.api.post('/auth/register', {
      username,
      email,
      password,
      role
    });
  }
}
```

## Gestion des erreurs

### Codes de statut HTTP

- **200 OK**: Requête réussie
- **201 Created**: Ressource créée
- **400 Bad Request**: Requête invalide
- **401 Unauthorized**: Non authentifié (token manquant ou invalide)
- **403 Forbidden**: Non autorisé (rôle insuffisant)
- **404 Not Found**: Ressource non trouvée
- **500 Internal Server Error**: Erreur serveur

### Exemple de gestion d'erreurs

```javascript
const handleApiError = (error) => {
  if (error.response) {
    switch (error.response.status) {
      case 401:
        // Token invalide ou expiré
        localStorage.removeItem('token');
        window.location.href = '/login';
        break;
      case 403:
        alert('Vous n\'avez pas les permissions nécessaires');
        break;
      case 404:
        alert('Ressource non trouvée');
        break;
      case 500:
        alert('Erreur serveur. Veuillez réessayer plus tard.');
        break;
      default:
        alert('Une erreur est survenue');
    }
  } else {
    alert('Erreur de connexion au serveur');
  }
};
```

## Test de connexion

1. Ouvrez le fichier `FRONTEND_CONNECTION_EXAMPLE.html` dans un navigateur
2. Cliquez sur "Tester /api/health" pour vérifier que l'API fonctionne
3. Entrez vos identifiants et cliquez sur "Se connecter"
4. Le token sera automatiquement rempli et vous pourrez tester les endpoints authentifiés

## Dépannage

### Erreur CORS
- Vérifiez que l'API est bien démarrée sur le port 8080
- Vérifiez que vous utilisez l'URL correcte : `http://localhost:8080/api`
- Vérifiez la configuration CORS dans `SecurityConfig.java`

### Erreur 401 Unauthorized
- Vérifiez que le token est bien présent dans le header `Authorization`
- Vérifiez que le token n'est pas expiré (durée : 24h par défaut)
- Vérifiez que vous êtes bien connecté

### Erreur 403 Forbidden
- Vérifiez que l'utilisateur a le bon rôle pour accéder à la ressource
- Vérifiez les permissions dans `SecurityConfig.java`

## Support

Pour plus d'informations, consultez le fichier `API_ENDPOINTS.md` qui contient la documentation complète de tous les endpoints.

