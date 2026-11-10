import json
import logging
import os
import uuid
from datetime import datetime, timezone

from confluent_kafka import Consumer, Producer, KafkaError

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger(__name__)

BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
INPUT_TOPIC = "orders.created"
OUTPUT_TOPIC = ("orders.stock-validated", "orders.stock-rejected")

def build_result(event: dict) -> tuple[str, dict]:
    items = event["payload"]["items"]
    available = all(item["quantity"] <= 10 for item in items)
    event_type = "StockValidated" if available else "StockRejected"
    payload = {"available": available}

    if not available:
        payload["reason"] = "Insufficient stock for one or more items."
    result = {
        "event_id": str(uuid.uuid4()),
        "event_type": event_type,
        "occured_At": datetime.now(timezone.utc).isoformat(),
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
                if message.error().code() == KafkaError._PARTITION_EOF:
                    continue
                raise RuntimeError(f"Consumer error: {message.error()}")
            try:
                event = json.loads(message.value().decode("utf-8"))
                topic, result = build_result(event)
                producer.produce(
                    topic,
                    key=event["orderId"], 
                    value=json.dumps(result).encode("utf-8"),
                    )
                producer.flush()
                consumer.commit(message)
            except (KeyError, json.JSONDecodeError) as e:
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