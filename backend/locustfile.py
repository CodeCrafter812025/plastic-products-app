# locustfile.py
import random
from locust import HttpUser, task, between

with open("tokens.txt") as f:
    TOKENS = [line.strip() for line in f if line.strip()]

PRODUCT_IDS = [1, 2]  # با شناسه‌ی محصولات واقعی توی دیتابیس تستتون هماهنگ کنید


class BuyerUser(HttpUser):
    host = "http://127.0.0.1:8000"
    wait_time = between(2, 8)

    def on_start(self):
        token = random.choice(TOKENS)
        self.client.headers.update({"Authorization": f"Bearer {token}"})
        self.cart_item_id = None

    @task(5)
    def list_products(self):
        self.client.get("/api/v1/products/", name="/api/v1/products/")

    @task(3)
    def get_product(self):
        pid = random.choice(PRODUCT_IDS)
        self.client.get(f"/api/v1/products/{pid}/", name="/api/v1/products/[id]/")

    @task(2)
    def add_to_cart(self):
        pid = random.choice(PRODUCT_IDS)
        with self.client.post(
            "/api/v1/cart/", json={"product_id": pid, "quantity": random.randint(1, 3)},
            name="/api/v1/cart/ [POST]", catch_response=True,
        ) as resp:
            if resp.status_code == 201:
                self.cart_item_id = resp.json().get("data", {}).get("id")
                resp.success()
            else:
                resp.failure(f"add to cart failed: {resp.status_code}")

    @task(1)
    def update_cart_item(self):
        if not self.cart_item_id:
            return
        pid = random.choice(PRODUCT_IDS)
        self.client.patch(
            f"/api/v1/cart/{self.cart_item_id}/",
            json={"product_id": pid, "quantity": random.randint(1, 5)},
            name="/api/v1/cart/[id]/",
        )

    @task(1)
    def create_order(self):
        if not self.cart_item_id:
            return
        with self.client.post(
            "/api/v1/orders/", json={}, name="/api/v1/orders/ [POST]", catch_response=True
        ) as resp:
            if resp.status_code == 201:
                self.cart_item_id = None
                resp.success()
            else:
                resp.failure(f"order failed: {resp.status_code}")