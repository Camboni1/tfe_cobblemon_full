# Docker — guide pratique du projet

Toutes les commandes ci-dessous se lancent depuis la **racine du repo** (`tfe_cobblemon_full/`), là où se trouve `docker-compose.yml`. Pas depuis `backend/` ou `frontend/`.

---

## Architecture des services

Le `docker-compose.yml` définit 4 services :

| Service     | Conteneur            | Port hôte | Rôle                                              |
|-------------|----------------------|-----------|---------------------------------------------------|
| `db`        | `cobblemon-db`       | 5432      | PostgreSQL 16, données dans le volume `cobblemon_db_data` |
| `backend`   | `cobblemon-backend`  | 8080      | API Spring Boot                                   |
| `frontend`  | `cobblemon-frontend` | 3000      | App Next.js                                       |
| `importer`  | (éphémère)           | —         | Importeur de dataset Cobblemon — **profil `import`** |

Au démarrage normal (`docker compose up`), seuls `db`, `backend` et `frontend` tournent. L'importer ne se lance que quand tu actives explicitement son profil.

---

## Démarrer / arrêter la stack

### Démarrer en arrière-plan
```bash
docker compose up -d
```
Lance db + backend + frontend. Le back attend que la DB soit `healthy` (healthcheck `pg_isready`).

### Démarrer + rebuilder les images si le code a changé
```bash
docker compose up -d --build
```

### Arrêter (sans supprimer les volumes)
```bash
docker compose down
```
Supprime les conteneurs et le réseau, mais **garde les données Postgres** (volume `cobblemon_db_data`).

### Arrêter ET tout supprimer (données comprises)
```bash
docker compose down -v
```
Le `-v` supprime les volumes → **la DB est vidée**. À utiliser quand tu veux un environnement vraiment propre.

### Redémarrer un seul service
```bash
docker compose restart backend
docker compose restart frontend
```

---

## Logs

### Suivre les logs en temps réel
```bash
docker compose logs -f                # tous les services
docker compose logs -f backend        # juste le back
docker compose logs -f backend frontend
```

### Voir les N dernières lignes
```bash
docker compose logs --tail=200 backend
```

`Ctrl+C` arrête le suivi (sans arrêter les conteneurs).

---

## Rebuild après changement de code

Le code est copié dans l'image au moment du `docker build`. Donc **modifier un fichier ne suffit pas** — il faut rebuilder.

### Rebuilder un service précis
```bash
docker compose build backend
docker compose build frontend
docker compose up -d backend          # relance avec la nouvelle image
```

### Forcer un build sans cache (rare, en cas de doute)
```bash
docker compose build --no-cache backend
```

---

## État de la stack

### Lister les conteneurs du projet
```bash
docker compose ps
```

### Lister les images du projet
```bash
docker compose images
```

### Inspecter un conteneur
```bash
docker inspect cobblemon-backend
```

### Ouvrir un shell dans un conteneur
```bash
docker compose exec backend sh
docker compose exec db psql -U cobblemon -d cobblemon
```

---

## L'importer (chargement d'un dataset Cobblemon)

L'importer est gated par le profil `import` (déclaration `profiles: ["import"]` dans le compose). Il ne démarre **que si tu l'invoques explicitement**.

### Lancer un import avec les valeurs par défaut
```bash
docker compose --profile import up --build importer
```
Variables d'environnement par défaut (modifiables via `.env` ou inline) :
- `APP_DATASET_IMPORT_CODE=cb161`
- `APP_DATASET_IMPORT_LABEL=Cobblemon 1.6.1`
- `APP_DATASET_IMPORT_INPUT_PATH=./import-data/cb161`
- `APP_DATASET_IMPORT_CLEAN_BEFORE_IMPORT=true`

### Lancer avec d'autres valeurs
```bash
APP_DATASET_IMPORT_CODE=cb162 \
APP_DATASET_IMPORT_LABEL="Cobblemon 1.6.2" \
docker compose --profile import up --build importer
```

L'importer s'arrête tout seul à la fin (c'est un job, pas un service long-running).

---

## Erreurs fréquentes

### `Bind for 0.0.0.0:3000 failed: port is already allocated`
Un autre process écoute déjà sur le port (un `npm run dev` resté ouvert dans un autre terminal, un ancien conteneur, etc.). Solutions :

```bash
# Voir qui écoute (Windows PowerShell)
Get-Process -Id (Get-NetTCPConnection -LocalPort 3000).OwningProcess

# Ou lister les conteneurs Docker qui mappent ce port
docker ps --filter "publish=3000"

# Si c'est un conteneur orphelin :
docker compose down
docker ps -a                          # vérifier qu'aucun cobblemon-* ne traîne
docker rm -f cobblemon-frontend       # au pire, suppression brutale
```

Idem pour 8080 (back) et 5432 (DB) si tu as un Postgres local.

### `No active profile set, falling back to "default"` côté back
Le back a démarré sans le profil `docker` → il lit `application.properties` qui pointe sur `localhost:5432`. Tu n'as **pas** lancé via `docker compose up` mais via `docker run` direct. Toujours utiliser compose.

### `Connection to db:5432 refused`
La DB n'est pas encore prête. Vérifie son healthcheck :
```bash
docker compose ps                     # cobblemon-db doit être (healthy)
docker compose logs db
```
Le `depends_on: condition: service_healthy` doit normalement empêcher ce cas.

### Le front affiche du contenu obsolète après une modif
Le build Next.js a été fait au moment du `docker compose build`. Modifier les fichiers source ensuite ne change rien tant que tu ne rebuildes pas l'image :
```bash
docker compose build frontend && docker compose up -d frontend
```

---

## Nettoyage

### Images intermédiaires `<none>` (cache de build inutile)
```bash
docker image prune
```

### Cache buildkit
```bash
docker builder prune
```

### Tout ce qui n'est pas utilisé (images, conteneurs, réseaux, build cache)
```bash
docker system prune -a
```
**Attention** : supprime aussi les images d'autres projets si elles ne sont pas utilisées par un conteneur en cours.

### Voir ce qui prend de la place
```bash
docker system df
```

---

## Workflow type au quotidien

```bash
# 1. Modif de code back
docker compose build backend
docker compose up -d backend
docker compose logs -f backend

# 2. Modif de code front
docker compose build frontend
docker compose up -d frontend
docker compose logs -f frontend

# 3. Reset complet de la DB (re-import propre)
docker compose down -v
docker compose up -d db
docker compose --profile import up --build importer
docker compose up -d backend frontend
```
