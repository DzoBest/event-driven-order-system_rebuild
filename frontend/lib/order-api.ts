import type { Order, OrderRequest } from "./types";

function getApiUrl(): string {
  if (process.env.NEXT_PUBLIC_API_URL) {
    return process.env.NEXT_PUBLIC_API_URL;
  }
  if (typeof window !== "undefined") {
    return `${window.location.protocol}//${window.location.hostname}:8080`;
  }
  return "http://localhost:8080";
}

export async function createOrder(request: OrderRequest): Promise<Order> {
  const API_URL = getApiUrl();
  const response = await fetch(`${API_URL}/api/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    let message = "Impossible de créer la commande.";
    try {
      const body = await response.json();
      if (body.message) message = body.message;
    } catch {
      // Le backend peut ne pas renvoyer de corps JSON pour certaines erreurs.
    }
    throw new Error(message);
  }

  return response.json();
}

export async function getOrder(id: string): Promise<Order> {
  const API_URL = getApiUrl();
  const response = await fetch(`${API_URL}/api/orders/${id}`);
  if (!response.ok) {
    throw new Error("Impossible d'actualiser la commande.");
  }
  return response.json();
}
