export type OrderRequest = {
  customerId: string;
  items: Array<{
    productId: string;
    quantity: number;
  }>;
};

export type Order = {
  id: string;
  customerId: string;
  items: Array<{
    productId: string;
    quantity: number;
  }>;
  status: "PENDING" | "STOCK_VALIDATED" | "STOCK_NOT_AVAILABLE" | string;
  createdAt: string;
};
