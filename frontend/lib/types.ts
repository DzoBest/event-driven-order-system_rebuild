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
  status: "PENDING" | "VALIDATED" | "REJECTED" | string;
  createdAt: string;
};
