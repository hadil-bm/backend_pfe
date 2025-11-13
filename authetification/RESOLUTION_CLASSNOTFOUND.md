# Résolution du problème ClassNotFoundException

## Problème
```
Error: Could not find or load main class com.project.authetification.AuthetificationApplication
Caused by: java.lang.ClassNotFoundException: com.project.authetification.AuthetificationApplication
```

## Solutions

### Solution 1 : Utiliser le script de build (Recommandé)
1. Exécutez le script `FIX_CLASSNOTFOUND.bat` :
   ```bash
   FIX_CLASSNOTFOUND.bat
   ```
   
2. Ce script va :
   - Nettoyer le projet (`mvn clean`)
   - Compiler le projet (`mvn compile`)
   - Créer le JAR exécutable (`mvn package`)

### Solution 2 : Utiliser Maven Wrapper manuellement
```bash
# Nettoyer le projet
mvnw.cmd clean

# Compiler le projet
mvnw.cmd compile

# Créer le JAR
mvnw.cmd package -DskipTests
```

### Solution 3 : Exécuter avec Spring Boot Maven Plugin
```bash
mvnw.cmd spring-boot:run
```

### Solution 4 : Exécuter le JAR après build
```bash
# Après avoir exécuté mvnw.cmd package
java -jar target\authetification-0.0.1-SNAPSHOT.jar
```

## Corrections apportées

1. **Correction du modèle VM** : Changement de `isMonitored` en `monitored` pour respecter les conventions Lombok
2. **Correction des erreurs de compilation** : Mise à jour de tous les appels aux méthodes `getIsMonitored()` et `setIsMonitored()`
3. **Nettoyage des imports** : Suppression des imports inutilisés
4. **Configuration Maven** : Amélioration de la configuration du plugin Spring Boot

## Vérification

Pour vérifier que tout fonctionne :
1. Exécutez `FIX_CLASSNOTFOUND.bat`
2. Vérifiez qu'il n'y a pas d'erreurs de compilation
3. Lancez l'application avec `mvnw.cmd spring-boot:run`

## Notes

- Assurez-vous d'avoir Java 21 installé
- Assurez-vous que MongoDB est accessible (vérifiez `application.properties`)
- Les dépendances Maven seront téléchargées automatiquement au premier build

