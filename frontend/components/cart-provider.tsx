"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";

import { addCartItem, CART_ID_KEY, CART_TOKEN_KEY, createAnonymousCart, deleteCartItem, getCart, requestFromCartItem, updateCartItem, type Cart, type CartIdentity, type CartItem, type CartItemRequest } from "@/lib/cart";

type CartContextValue = {
  cart: Cart | null;
  loading: boolean;
  error: string | null;
  addItem: (request: CartItemRequest) => Promise<Cart>;
  changeQuantity: (item: CartItem, quantity: number) => Promise<Cart>;
  removeItem: (itemId: number) => Promise<void>;
  refresh: () => Promise<void>;
};

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const identityRef = useRef<CartIdentity | null>(null);
  const createPromiseRef = useRef<Promise<CartIdentity> | null>(null);

  const clearIdentity = useCallback(() => {
    identityRef.current = null;
    window.localStorage.removeItem(CART_ID_KEY);
    window.localStorage.removeItem(CART_TOKEN_KEY);
  }, []);

  const storeIdentity = useCallback((identity: CartIdentity) => {
    identityRef.current = identity;
    window.localStorage.setItem(CART_ID_KEY, String(identity.id));
    window.localStorage.setItem(CART_TOKEN_KEY, identity.token);
  }, []);

  useEffect(() => {
    const id = Number(window.localStorage.getItem(CART_ID_KEY));
    const token = window.localStorage.getItem(CART_TOKEN_KEY);
    if (!Number.isInteger(id) || id <= 0 || !token) {
      queueMicrotask(() => setLoading(false));
      return;
    }
    const identity = { id, token };
    identityRef.current = identity;
    getCart(identity)
      .then(setCart)
      .catch(() => { clearIdentity(); setCart(null); })
      .finally(() => setLoading(false));
  }, [clearIdentity]);

  const ensureIdentity = useCallback(async () => {
    if (identityRef.current) return identityRef.current;
    if (!createPromiseRef.current) {
      createPromiseRef.current = createAnonymousCart().then(({ identity, cart: createdCart }) => {
        storeIdentity(identity);
        setCart(createdCart);
        return identity;
      }).finally(() => { createPromiseRef.current = null; });
    }
    return createPromiseRef.current;
  }, [storeIdentity]);

  const addItem = useCallback(async (request: CartItemRequest) => {
    setError(null);
    const identity = await ensureIdentity();
    try {
      const updated = await addCartItem(identity, request);
      setCart(updated);
      return updated;
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : "Unable to add this item.";
      setError(message);
      throw caught;
    }
  }, [ensureIdentity]);

  const changeQuantity = useCallback(async (item: CartItem, quantity: number) => {
    if (!identityRef.current) throw new Error("Your cart could not be found.");
    setError(null);
    const updated = await updateCartItem(identityRef.current, item.id, requestFromCartItem(item, quantity));
    setCart(updated);
    return updated;
  }, []);

  const removeItem = useCallback(async (itemId: number) => {
    if (!identityRef.current) return;
    setError(null);
    await deleteCartItem(identityRef.current, itemId);
    const updated = await getCart(identityRef.current);
    setCart(updated);
  }, []);

  const refresh = useCallback(async () => {
    if (!identityRef.current) return;
    setCart(await getCart(identityRef.current));
  }, []);

  return <CartContext.Provider value={{ cart, loading, error, addItem, changeQuantity, removeItem, refresh }}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) throw new Error("useCart must be used inside CartProvider.");
  return context;
}
