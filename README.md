# Event-Driven Order Processing System

Application de demonstration event-driven permettant de creer des commandes,
verifier leur stock et suivre leur statut a travers PostgreSQL, Apache Kafka,
Spring Boot, Python et Next.js.

## Architecture

```text
Next.js
  |
  | POST /api/orders
  v
Spring Boot order-service
  |
  +--> PostgreSQL: commande, articles, stock, outbox
  |
  +--> outbox publisher --> Kafka orders.created
                                  |
                                  v
                         inventory-worker
                         reservation PostgreSQL
                                  |
                    +-------------+-------------+
                    v                           v
       orders.stock-validated      orders.stock-rejected
                    |                           |
                    +-------------+-------------+
                                  v
                   Spring stock-result consumer
                                  |
                                  v
                       statut final PostgreSQL
```

## Composants

| Composant | Technologie | Rôle |
|---|---|---|
| Frontend | Next.js 16, React, TypeScript | Catalogue, panier et suivi du statut |
| API | Spring Boot 4, Java 25 | Création et lecture des commandes |
| Base | PostgreSQL 17 | Commandes, inventaire, outbox et idempotence |
| Broker | Apache Kafka 4 | Transport des événements |
| Worker | Python 3.12, Confluent Kafka | Vérification et réservation du stock |

## Ports locaux

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API Spring | http://localhost:8080 |
| Health Spring | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:15432 |
| Kafka | localhost:9092 |

## Prerequis

- Docker Desktop
- Java 25 ou version prise en charge par le wrapper Maven
- Node.js 22
- Python 3.12

## Configuration locale

Creer `.env` a partir du modele :

```powershell
Copy-Item .env.example .env
```

Modifier ensuite le mot de passe dans `.env` :

```env
POSTGRES_DB=orderdb
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
```

Ne jamais commiter `.env`. Le fichier est exclu par `.gitignore`.

## Demarrage avec Docker

Construire le backend puis lancer toute la stack :

```powershell
cd backend\order-service
.\mvnw.cmd clean package -DskipTests
cd ..\..

docker compose up -d --build
docker compose ps
```

Le frontend est accessible sur http://localhost:3000.

Pour suivre les logs :

```powershell
docker compose logs -f order-service inventory-worker
```

Pour arrêter les services :

```powershell
docker compose down
```

Pour supprimer également les données PostgreSQL locales :

```powershell
docker compose down -v
```

## Requetes API

Creer une commande :

```powershell
$body = @{
  customerId = "client-test"
  items = @(
    @{
      productId = "product-002"
      quantity = 2
    }
  )
} | ConvertTo-Json -Depth 5

$order = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/orders" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

$order
```

Lire une commande :

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/orders/$($order.id)"
```

### Statuts

- `PENDING` : commande enregistrée, résultat du stock en attente
- `STOCK_VALIDATED` : stock réservé
- `STOCK_NOT_AVAILABLE` : stock insuffisant

## Stocks PostgreSQL

Afficher les stocks :

```powershell
docker compose exec postgres psql -U postgres -d orderdb `
  -c "SELECT * FROM inventory_items ORDER BY product_id;"
```

Afficher un produit :

```powershell
docker compose exec postgres psql -U postgres -d orderdb `
  -c "SELECT * FROM inventory_items WHERE product_id = 'product-001';"
```

## Topics Kafka

Les topics suivants sont créés avant les applications :

- `orders.created`
- `orders.stock-validated`
- `orders.stock-rejected`
- `orders.dlq`

Lister les topics :

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh `
  --bootstrap-server kafka:9092 --list
```

Lire les validations :

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server kafka:9092 `
  --topic orders.stock-validated `
  --group manual-validation-reader `
  --from-beginning
```

Lire les rejets :

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server kafka:9092 `
  --topic orders.stock-rejected `
  --group manual-rejection-reader `
  --from-beginning
```

## Flux event-driven

1. Spring valide la requête et crée une commande `PENDING`.
2. Spring sauvegarde également un événement dans `outbox_events`.
3. `OutboxPublisher` publie l'événement sur `orders.created`.
4. Le worker Python verrouille les stocks, vérifie les quantités et réserve les articles.
5. Le worker publie dans `orders.stock-validated` ou `orders.stock-rejected`.
6. Les consumer Spring mettent à jour le statut avec idempotence.
7. Le frontend interroge la commande toutes les deux secondes tant qu'elle reste `PENDING`.

## Tests

Backend Spring :

```powershell
cd backend\order-service
.\mvnw.cmd test
```

Worker Python :

```powershell
cd inventory-worker
python -m pip install -r requirements.txt
python -m pytest -v
```

Frontend :

```powershell
cd frontend
npm ci
npm run build
npm run dev
```

## Structure

```text
backend/order-service/
  src/main/java/
    api/                 API REST
    order/               domaine des commandes
    outbox/              outbox pattern
    processed/           idempotence des consumers
    stock/               consumer des résultats Kafka
  src/main/resources/
    db/migration/        migrations Flyway

inventory-worker/
  worker.py              consumer et réservation du stock
  test_worker.py         tests unitaires

frontend/
  app/                   pages et styles
  components/            composants React
  lib/                   appels API et types
```

## Notes d'exploitation

- Conserver les données en utilisant `docker compose down` sans `-v`.
- Ne pas modifier une migration Flyway déjà appliquée : créer une nouvelle migration.
- Ne jamais commiter `.env`, les mots de passe, clés ou jetons.
- Utiliser une quantité strictement positive pour chaque article.
- Le frontend utilise des données locales de démonstration ; les quantités réelles proviennent de PostgreSQL.
