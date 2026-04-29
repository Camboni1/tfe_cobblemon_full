# Liste des Routes API

Toutes les routes commencent par le préfixe `/api/v1` (sauf indication contraire).

## Pokémon

| Méthode | Route | Description |
| :--- | :--- | :--- |
| GET | `/api/v1/pokemon` | Rechercher des Pokémon (filtres: search, generationCode, implemented). Supporte la pagination. |
| GET | `/api/v1/pokemon/{slug}` | Récupérer les détails d'un Pokémon par son slug. |
| GET | `/api/v1/pokemon/{slug}/spawns` | Récupérer les règles de spawn d'un Pokémon. |
| POST | `/api/v1/pokemon` | Créer un nouveau Pokémon. |
| PUT | `/api/v1/pokemon/{slug}` | Mettre à jour un Pokémon. |
| DELETE | `/api/v1/pokemon/{slug}` | Supprimer un Pokémon. |

### Formes de Pokémon

| Méthode | Route | Description |
| :--- | :--- | :--- |
| POST | `/api/v1/pokemon/{slug}/forms` | Créer une nouvelle forme pour un Pokémon. |
| PUT | `/api/v1/pokemon/{slug}/forms/{code}` | Mettre à jour une forme spécifique. |
| DELETE | `/api/v1/pokemon/{slug}/forms/{code}` | Supprimer une forme. |

## Items

| Méthode | Route | Description |
| :--- | :--- | :--- |
| GET | `/api/v1/items` | Rechercher des items (filtre: search). Supporte la pagination. |

## Spawns (Règles d'apparition)

| Méthode | Route | Description |
| :--- | :--- | :--- |
| POST | `/api/v1/spawns` | Créer une règle de spawn. |
| PUT | `/api/v1/spawns/{id}` | Mettre à jour une règle de spawn par ID. |
| DELETE | `/api/v1/spawns/{id}` | Supprimer une règle de spawn. |

## Drops (Butins)

| Méthode | Route | Description |
| :--- | :--- | :--- |
| POST | `/api/v1/pokemon/{slug}/forms/{code}/drops` | Créer un drop pour une forme de Pokémon. |
| PUT | `/api/v1/drops/{id}` | Mettre à jour un drop. |
| DELETE | `/api/v1/drops/{id}` | Supprimer un drop. |

## Biomes

| Méthode | Route | Description |
| :--- | :--- | :--- |
| GET | `/api/v1/biomes` | Lister les biomes (filtre: search). Supporte la pagination. |
| GET | `/api/v1/biomes/pokemon` | Récupérer les Pokémon présents dans un biome (paramètre: biome). |

## Traductions

| Méthode | Route | Description |
| :--- | :--- | :--- |
| GET | `/api/v1/translations` | Rechercher des traductions (filtres: search, locale). |
| PUT | `/api/v1/translations/{key}` | Mettre à jour ou créer une traduction pour une clé donnée. |
