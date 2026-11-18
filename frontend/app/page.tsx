"use client";

import { FormEvent, useState } from "react";
import { createOrder, getOrder } from "../lib/order-api";
import type { Order } from "../lib/types";
import { ProductCard } from "../components/product-card";
import { Cart } from "../components/cart";

const products = [
  { id: "product-001", name: "Casque Studio", category: "Audio", price: 89, accent: "peach", emoji: "🎧" },
  { id: "product-002", name: "Montre Serein", category: "Accessoires", price: 129, accent: "blue", emoji: "⌚" },
  { id: "product-003", name: "Sac Everyday", category: "Mode", price: 74, accent: "lavender", emoji: "👜" },
] ;

type CartLine = (typeof products)[number] & { quantity: number };

export default function Home() {
  const [customerId, setCustomerId] = useState("");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function addProduct(product: (typeof products)[number]) {
    setCart((current) => {
      const existing = current.find((line) => line.id === product.id);
      if (existing) {
        return current.map((line) =>
          line.id === product.id ? { ...line, quantity: line.quantity + 1 } : line,
        );
      }
      return [...current, { ...product, quantity: 1 }];
    });
  }

  function updateQuantity(productId: string, quantity: number) {
    if (quantity < 1) {
      setCart((current) => current.filter((line) => line.id !== productId));
      return;
    }
    setCart((current) =>
      current.map((line) => (line.id === productId ? { ...line, quantity } : line)),
    );
  }

  async function submitOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const created = await createOrder({
        customerId: customerId.trim(),
        items: cart.map(({ id, quantity }) => ({
          productId: id,
          quantity,
        })),
      });
      setOrder(created);
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Impossible de créer la commande.");
    } finally {
      setLoading(false);
    }
  }

  async function refreshOrder() {
    if (!order) return;
    setLoading(true);
    setError("");
    try {
      setOrder(await getOrder(order.id));
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Impossible d'actualiser la commande.");
    } finally {
      setLoading(false);
    }
  }

  const total = cart.reduce((sum, line) => sum + line.price * line.quantity, 0);

  return (
    <main className="store">
      <header className="store-header">
        <a className="brand" href="#" aria-label="Accueil Nova">
          <span className="brand-mark">N</span>
          <span>Nova</span>
        </a>
        <div className="header-status">
          <span className="status-dot" />
          Backend connecté
        </div>
      </header>

      <div className="store-layout">
        <section className="catalog" aria-labelledby="catalog-title">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Collection du moment</p>
              <h1 id="catalog-title">Essentiels doux pour chaque jour.</h1>
            </div>
            <span className="item-count">{products.length} articles</span>
          </div>

          <div className="product-grid">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} onAdd={addProduct} />
            ))}
          </div>
        </section>

        <aside className="checkout" aria-label="Créer une commande">
          <form onSubmit={submitOrder}>
            <div className="checkout-heading">
              <div>
                <p className="eyebrow">Votre sélection</p>
                <h2>Récapitulatif</h2>
              </div>
              <span className="cart-count">{cart.reduce((sum, line) => sum + line.quantity, 0)}</span>
            </div>

            <Cart lines={cart} onUpdateQuantity={updateQuantity} />

            <label className="field">
              <span>Identifiant client</span>
              <input
                required
                minLength={1}
                placeholder="ex. cliente-marie"
                value={customerId}
                onChange={(event) => setCustomerId(event.target.value)}
              />
            </label>

            <div className="total-row">
              <span>Total</span>
              <strong>{total.toFixed(2)} €</strong>
            </div>

            <button
              className="primary-button"
              type="submit"
              disabled={cart.length === 0 || !customerId.trim() || loading}
            >
              {loading ? "Envoi en cours..." : "Créer la commande"}
            </button>
          </form>

          {error && <div className="notice notice-error" role="alert">{error}</div>}

          {order && (
            <section className="order-result" aria-live="polite">
              <div className="order-result-heading">
                <div>
                  <p className="eyebrow">Statut de la commande</p>
                </div>
                <span className={`order-status status-${order.status.toLowerCase()}`}>
                  {order.status}
                </span>
              </div>
              <button className="secondary-button" type="button" onClick={refreshOrder} disabled={loading}>
                Actualiser le statut
              </button>
            </section>
          )}
        </aside>
      </div>
    </main>
  );
}
