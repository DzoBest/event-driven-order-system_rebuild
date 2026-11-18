import json
import logging
import os
import uuid
from datetime import datetime, timezone
from collections import defaultdict

from confluent_kafka import Consumer, Producer, KafkaError
import psycopg2
from psycopg2.extras import RealDictCursor

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger(__name__)

BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
INPUT_TOPIC = "orders.created"
OUTPUT_TOPIC = ("orders.stock-validated", "orders.stock-rejected")

POSTGRES_DSN = os.getenv("POSTGRES_DSN")


def reserve_stock(items: list[dict], event_id: str | None = None) -> tuple[bool, list[dict], bool]:
    if not POSTGRES_DSN:
        raise RuntimeError("POSTGRES_DSN is required")

    requested = defaultdict(int)
    for item in items:
        product_id = item["productId"]
        quantity = item["quantity"]
        if quantity <= 0:
            raise ValueError("Item quantity must be positive")
        requested[product_id] += quantity

    insufficient = []
    connection = psycopg2.connect(POSTGRES_DSN)
    try:
        with connection:
            with connection.cursor(cursor_factory=RealDictCursor) as cursor:
                if event_id:
                    cursor.execute(
                        """
                        INSERT INTO inventory_processed_events (event_id, processed_at)
                        VALUES (%s, NOW())
                        ON CONFLICT (event_id) DO NOTHING
                        RETURNING event_id
                        """,
                        (event_id,),
                    )
                    if cursor.fetchone() is None:
                        return False, [], True

                for product_id, quantity in sorted(requested.items()):
                    cursor.execute(
                        """
                        SELECT available_quantity
                        FROM inventory_items
                        WHERE product_id = %s
                        FOR UPDATE
                        """,
                        (product_id,),
                    )
                    row = cursor.fetchone()
                    available = row["available_quantity"] if row else 0
                    if available < quantity:
                        insufficient.append({
                            "productId": product_id,
                            "requestedQuantity": quantity,
                            "availableQuantity": available,
                        })

                if insufficient:
                    return False, insufficient, False

                for product_id, quantity in sorted(requested.items()):
                    cursor.execute(
                        """
                        UPDATE inventory_items
                        SET available_quantity = available_quantity - %s,
                            reserved_quantity = reserved_quantity + %s,
                            version = version + 1,
                            updated_at = NOW()
                        WHERE product_id = %s
                        """,
                        (quantity, quantity, product_id),
                    )
        return True, [], False
    finally:
        connection.close()


def build_result(event: dict, stock_reservation=None) -> tuple[str, dict]:
    items = event["payload"]["items"]
    if stock_reservation is None:
        stock_reservation = reserve_stock(items)
    if len(stock_reservation) == 2:
        available, insufficient = stock_reservation
    else:
        available, insufficient, _ = stock_reservation
    event_type = "StockValidated" if available else "StockRejected"
    payload = {"available": available}

    if not available:
        payload["reason"] = "INSUFFICIENT_STOCK"
        payload["items"] = insufficient
    result = {
        "event_id": str(uuid.uuid4()),
        "event_type": event_type,
        "occurred_At": datetime.now(timezone.utc).isoformat(),
        "version": 1,
        "orderId": event["orderId"],
        "payload": payload,
    }
    return ("orders.stock-validated" if available else "orders.stock-rejected", result)


def create_consumer() -> Consumer:
    return Consumer(
        {
            "bootstrap.servers": BOOTSTRAP_SERVERS,
            "group.id": "inventory-worker-group",
            "enable.auto.commit": False,
            "auto.offset.reset": "earliest",
        }
    )

def run() -> None:
    consumer = create_consumer()
    producer = Producer({"bootstrap.servers": BOOTSTRAP_SERVERS})
    consumer.subscribe([INPUT_TOPIC])
    logger.info("Inventory worker started, listening for events...")
    try:
        while True:
            message = consumer.poll(1.0)
            if message is None:
                continue
            if message.error():
                if message.error().code() in (
                    KafkaError._PARTITION_EOF,
                    KafkaError.UNKNOWN_TOPIC_OR_PART,
                    KafkaError._TRANSPORT,
                    KafkaError._ALL_BROKERS_DOWN,
                ):
                    continue
                raise RuntimeError(f"Consumer error: {message.error()}")
            try:
                event = json.loads(message.value().decode("utf-8"))
                event_id = event.get("eventId") or event.get("event_id")
                stock_reservation = reserve_stock(event["payload"]["items"], event_id)
                if stock_reservation[2]:
                    consumer.commit(message)
                    continue
                topic, result = build_result(event, stock_reservation)
                producer.produce(
                    topic,
                    key=event["orderId"], 
                    value=json.dumps(result).encode("utf-8"),
                    )
                producer.flush()
                consumer.commit(message)
            except psycopg2.Error as e:
                logger.exception("Database unavailable while reserving stock: %s", e)
                continue
            except (KeyError, json.JSONDecodeError, ValueError) as e:
                logger.exception(f"Invalid order eevent; it requires DLQ handling: %s", e)
                producer.produce(
                    "orders.dlq",
                    key=message.key(),
                    value=json.dumps(
                        { 
                            "originalMessage": message.value().decode("utf-8", errors="replace"),
                            "error": str(e),
                            "sourceTopic": message.topic(),
                        }
                    ).encode("utf-8"),
                )
                producer.flush()
                consumer.commit(message=message)
    finally:
        consumer.close()

if __name__ == "__main__":
    run()