# Solution pour l'erreur ClassNotFoundException

## Problème
```
Error: Could not find or load main class com.project.authetification.AuthetificationApplication
Caused by: java.lang.ClassNotFoundException: com.project.authetification.AuthetificationApplication
```

## Corrections apportées

### 1. Nettoyage du pom.xml
- ✅ Suppression des dépendances dupliquées
- ✅ Ajout de la configuration `mainClass` dans le plugin Spring Boot Maven
- ✅ Organisation propre des dépendances

### 2. Corrections de code
- ✅ Correction des imports dans `GovernanceRule.java`
- ✅ Initialisation des listes dans `WorkOrder.java`
- ✅ Correction des méthodes de repository pour utiliser la syntaxe MongoDB correcte (`_Id`)

## Solution : Recompiler le projet

### Option 1 : Utiliser Maven Wrapper (Recommandé)
```bash
# Dans le répertoire authetification
./mvnw clean compile
./mvnw spring-boot:run
```

### Option 2 : Utiliser Maven directement
```bash
# Dans le répertoire authetification
mvn clean compile
mvn spring-boot:run
```

### Option 3 : Utiliser le script batch (Windows)
```bash
# Dans le répertoire authetification
clean-and-build.bat
```

### Option 4 : Dans votre IDE (IntelliJ IDEA / Eclipse)
1. Clic droit sur le projet → **Maven** → **Reload Project**
2. **Build** → **Rebuild Project**
3. Exécutez `AuthetificationApplication.java`

## Vérifications

### 1. Vérifier que la classe principale existe
Le fichier doit être présent à :
```
authetification/src/main/java/com/project/authetification/AuthetificationApplication.java
```

### 2. Vérifier la configuration dans pom.xml
Le plugin Spring Boot doit contenir :
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <mainClass>com.project.authetification.AuthetificationApplication</mainClass>
        ...
    </configuration>
</plugin>
```

### 3. Nettoyer le répertoire target
Supprimez le répertoire `target` et recompilez :
```bash
rm -rf target  # Linux/Mac
rmdir /s /q target  # Windows
```

## Si le problème persiste

1. **Vérifiez Java Version**
   ```bash
   java -version
   ```
   Doit être Java 21 ou supérieur

2. **Vérifiez Maven Version**
   ```bash
   mvn -version
   ```

3. **Reimportez le projet Maven dans votre IDE**
   - IntelliJ: File → Invalidate Caches / Restart
   - Eclipse: Right-click project → Maven → Update Project

4. **Vérifiez les erreurs de compilation**
   ```bash
   mvn clean compile
   ```

## Structure du projet

Le projet doit avoir cette structure :
```
authetification/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── project/
│   │   │           └── authetification/
│   │   │               ├── AuthetificationApplication.java
│   │   │               ├── config/
│   │   │               ├── controller/
│   │   │               ├── model/
│   │   │               ├── repository/
│   │   │               └── service/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── target/
```

## Notes importantes

- Le projet utilise Java 21
- MongoDB doit être configuré dans `application.properties`
- Les dépendances sont maintenant propres et sans doublons
- Le plugin Spring Boot est correctement configuré avec la classe principale

