from worker import build_result

def test_build_result_stock_validated():
    topic, result = build_result({"orderId": "order-1", "payload": {"items": [{"productId": "product-1", "quantity": 2}]}})
    assert topic == "orders.stock-validated"
    assert result["payload"]["available"] is True

def test_large_quantity_is_rejected():
    topic, result = build_result({"orderId": "order-1", "payload": {"items": [{"productId": "product-1", "quantity": 11}]}})
    assert topic == "orders.stock-rejected"
    assert result["payload"]["available"] is False
    assert result["payload"]["reason"] == "INSUFFICIENT_STOCK"