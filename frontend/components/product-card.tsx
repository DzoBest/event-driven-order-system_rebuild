type Product = {
  id: string;
  name: string;
  category: string;
  price: number;
  accent: string;
  emoji: string;
};

export function ProductCard({
  product,
  onAdd,
}: {
  product: Product;
  onAdd: (product: Product) => void;
}) {
  return (
    <article className="product-card">
      <div className={`product-art product-${product.accent}`} aria-hidden="true">
        <span className="product-emoji">{product.emoji}</span>
        <span className="product-tag">Nouveau</span>
      </div>
      <div className="product-info">
        <p>{product.category}</p>
        <h2>{product.name}</h2>
        <div className="product-footer">
          <strong>{product.price.toFixed(2)} €</strong>
          <button type="button" onClick={() => onAdd(product)} aria-label={`Ajouter ${product.name}`}>
            +
          </button>
        </div>
      </div>
    </article>
  );
}
