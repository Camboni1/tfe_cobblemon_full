# TFE Cobblemon - API Backend

Ce projet est le backend d'une application de gestion pour l'univers Cobblemon (Minecraft Mod). Il permet de gérer les Pokémon, les items, les règles d'apparition (spawns), les butins (drops) et les biomes.

## Technologies utilisées

- **Java 21**
- **Spring Boot 3.5.13**
- **Spring Data JPA**
- **PostgreSQL** (Base de données)
- **Flyway** (Migrations de base de données)
- **Spring Security** & **OAuth2 Resource Server**
- **SpringDoc OpenAPI (Swagger)**
- **Lombok**
- **Maven**

## Installation et démarrage

### Prérequis

- JDK 21 installé
- PostgreSQL installé et configuré
- Maven

### Configuration

Vérifiez les paramètres de connexion à la base de données dans le fichier `src/main/resources/application.properties` (ou `application.yml`).

### Lancement

Pour démarrer l'application :

```bash
mvn spring-boot:run
```

L'API sera accessible sur `http://localhost:8080`.

## Documentation de l'API

Une interface Swagger UI est disponible à l'adresse suivante une fois l'application lancée :
`http://localhost:8080/swagger-ui.html`

La liste complète des routes est également disponible dans le fichier [routes.md](./routes.md).

## Fonctionnalités principales

- Recherche et consultation des Pokémon (filtrage par nom, génération, implémentation).
- Gestion des formes de Pokémon.
- Gestion des règles de spawn complexes.
- Consultation et recherche d'items.
- Consultation des biomes et des Pokémon qui y apparaissent.
- Système de traductions multilingues.
- Gestion des drops (butins) par Pokémon.
