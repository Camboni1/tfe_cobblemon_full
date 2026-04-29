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
docker compose logs -f frontend       # juste le front
docker compose logs -f backend frontend
```

### Voir les N dernières lignes
```bash
docker compose logs --tail=200 frontend
```

`Ctrl+C` arrête le suivi (sans arrêter les conteneurs).

---

## Rebuild après changement de code

Le code est copié dans l'image au moment du `docker build`. Donc **modifier un fichier ne suffit pas** — il faut rebuilder.

### Rebuilder un service précis
```bash
docker compose build frontend
docker compose build backend
docker compose up -d frontend         # relance avec la nouvelle image
```

### Forcer un build sans cache (rare, en cas de doute)
```bash
docker compose build --no-cache frontend
```

### Note Next.js
Le `next build` se fait **dans l'image** au moment du `docker build`. Tant que tu ne rebuildes pas l'image, le bundle servi reste celui de la dernière build, même si tu modifies des fichiers dans `src/`. Pour itérer rapidement en dev, utilise plutôt `npm run dev` en local (hors Docker) sur le port 3000, avec le back qui tourne dans Docker.

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
docker inspect cobblemon-frontend
```

### Ouvrir un shell dans un conteneur
```bash
docker compose exec frontend sh
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

### Le front n'arrive pas à joindre le back
Vérifie que dans le compose `API_INTERNAL_URL=http://backend:8080` est bien utilisé pour les fetchs côté serveur (SSR, route handlers), et `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` côté navigateur. Le navigateur de l'utilisateur ne sait pas résoudre `backend` (nom DNS interne au réseau Docker).

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
# 1. Modif de code front
docker compose build frontend
docker compose up -d frontend
docker compose logs -f frontend

# 2. Modif de code back
docker compose build backend
docker compose up -d backend
docker compose logs -f backend

# 3. Reset complet de la DB (re-import propre)
docker compose down -v
docker compose up -d db
docker compose --profile import up --build importer
docker compose up -d backend frontend
```

---

## Alternative dev front : sans Docker

Pour itérer vite sur le front sans rebuilder l'image à chaque modif :

```bash
# Stack Docker mais sans le front
docker compose up -d db backend

# Front en local
cd frontend
npm run dev
```

Ton front sur `http://localhost:3000` (live reload) tape sur le back conteneurisé sur `http://localhost:8080`. Pense à arrêter le service `frontend` Docker s'il tournait, sinon conflit de port.
