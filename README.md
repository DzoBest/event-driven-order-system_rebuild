# Event-Driven Order Processing System

Projet monorepo démontrant le traitement asynchrone d'une commande avec
Spring Boot, PostgreSQL, Kafka, un worker Python et Next.js.

## Démarrage local

### Backend

```powershell
cd backend\order-service
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Le backend expose `POST /api/orders` et `GET /api/orders/{id}`.

### Tous les services avec Docker Compose

Construire le JAR avant de construire l'image backend :

```powershell
cd backend\order-service
.\mvnw.cmd clean package -DskipTests
cd ..\..
docker compose up --build
```

Le frontend peut être lancé séparément :

```powershell
cd frontend
npm install
npm run dev
```

## Tests

```powershell
cd backend\order-service
.\mvnw.cmd clean verify

cd ..\..\inventory-worker
python -m pip install -r requirements.txt
python -m pytest

cd ..\frontend
npm install
npm run build
```

Les paramètres d'environnement sont documentés dans `.env.example`.