# OpenApiReader

## Description
OpenApiReader est une application Java permettant de lire, d’analyser et de regrouper les chemins (paths) d’une spécification OpenAPI. Cette application extrait les informations importantes comme les méthodes HTTP, les descriptions des endpoints, et les rôles de sécurité associés.

## Fonctionnalités principales
- Récupération d'une spécification OpenAPI via une URL.
- Analyse des chemins (éléments "paths") pour regrouper les endpoints par racine.
- Extraction des détails des endpoints, y compris les méthodes HTTP, les descriptions, et les rôles de sécurité.
- Exclusion configurable de certains chemins (par exemple, ceux commençant par "test").

## Prérequis
- Java 21 ou version ultérieure
- Maven pour la gestion des dépendances

## Installation
1. Clonez ce répertoire :
   ```bash
   git clone <repository-url>
   ```
2. Compilez le projet avec Maven :
   ```bash
   mvn clean install
   ```

## Utilisation
Exécutez l'application avec la commande suivante :
```bash
java -jar target/OpenApiReader.jar <URL>
```

### Exemple
```bash
java -jar target/OpenApiReader.jar https://petstore.swagger.io/v2/swagger.json
```

### Sortie attendue
L’application affiche les chemins regroupés par racine avec leurs détails :
```
==pet==
  - GET /pet/findByStatus : Find pets by status
     Roles : user,admin
  - POST /pet : Add a new pet to the store
     Roles : admin
```

## Configuration
### Exclusion de chemins
Vous pouvez exclure certains chemins en modifiant la méthode `shouldExcludePath` dans le code :
```java
private static boolean shouldExcludePath(String path) {
    List<String> excludedPaths = List.of("test");
    return excludedPaths.stream().anyMatch(path::startsWith);
}
```

## Dépendances
Le projet utilise les bibliothèques suivantes :
- **Spring Web** : pour effectuer des requêtes HTTP.
- **Jackson** : pour analyser et manipuler le JSON.
- **SLF4J** : pour la journalisation.

## Structure du Code
- `main(String[] args)` : Point d’entrée principal de l’application.
- `getOpenApiSpec(String url)` : Récupère la spécification OpenAPI via HTTP.
- `extractApi(String openApiJson)` : Analyse et regroupe les chemins d’API par racine.
- `groupPathsByRoot(JsonNode pathsNode)` : Réalise le regroupement des chemins par racine.
- `shouldExcludePath(String path)` : Vérifie si un chemin doit être exclu de l’analyse.

## Limitations
- L’application suppose que les chemins contiennent un élément "tags" pour regrouper les endpoints.
- La gestion des rôles de sécurité repose sur un format JSON standard, mais peut ne pas fonctionner pour des spécifications personnalisées.

## Contributions
Les contributions sont les bienvenues. Merci de soumettre une pull request avec une description claire des modifications.

## Licence
Ce projet est sous licence MIT. Consultez le fichier `LICENSE` pour plus de détails.

