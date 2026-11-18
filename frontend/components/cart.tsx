type CartLine = {
  id: string;
  name: string;
  price: number;
  quantity: number;
  emoji: string;
};

export function Cart({
  lines,
  onUpdateQuantity,
}: {
  lines: CartLine[];
  onUpdateQuantity: (productId: string, quantity: number) => void;
}) {
  if (lines.length === 0) {
    return (
      <div className="empty-cart">
        <span>🛍️</span>
        <p>Votre panier est vide</p>
        <small>Ajoutez un article pour commencer.</small>
      </div>
    );
  }

  return (
    <div className="cart-lines">
      {lines.map((line) => (
        <div className="cart-line" key={line.id}>
          <span className="cart-emoji" aria-hidden="true">{line.emoji}</span>
          <div className="cart-line-info">
            <strong>{line.name}</strong>
            <span>{(line.price * line.quantity).toFixed(2)} €</span>
          </div>
          <div className="quantity-control" aria-label={`Quantité pour ${line.name}`}>
            <button type="button" onClick={() => onUpdateQuantity(line.id, line.quantity - 1)} aria-label="Réduire">−</button>
            <input
              aria-label="Quantité"
              type="number"
              min="1"
              max="99"
              value={line.quantity}
              onChange={(event) => onUpdateQuantity(line.id, Number(event.target.value))}
            />
            <button type="button" onClick={() => onUpdateQuantity(line.id, line.quantity + 1)} aria-label="Augmenter">+</button>
          </div>
        </div>
      ))}
    </div>
  );
}
