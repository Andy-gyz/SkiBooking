"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";

import { useAuth } from "@/components/auth-provider";
import { addCartItem, CART_ID_KEY, CART_TOKEN_KEY, createCart, deleteCartItem, getCart, requestFromCartItem, updateCartItem, type Cart, type CartIdentity, type CartItem, type CartItemRequest } from "@/lib/cart";

type CartContextValue = {
  cart: Cart | null;
  loading: boolean;
  error: string | null;
  anonymousCartToken: string | null;
  addItem: (request: CartItemRequest) => Promise<Cart>;
  changeQuantity: (item: CartItem, quantity: number) => Promise<Cart>;
  removeItem: (itemId: number) => Promise<void>;
  refresh: () => Promise<void>;
};

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const { accessToken, cartId: authenticatedCartId, loading: authLoading, updateCartId } = useAuth();
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [anonymousCartToken, setAnonymousCartToken] = useState<string | null>(null);
  const identityRef = useRef<CartIdentity | null>(null);
  const createPromiseRef = useRef<Promise<CartIdentity> | null>(null);

  const clearIdentity = useCallback(() => {
    identityRef.current = null;
    window.localStorage.removeItem(CART_ID_KEY);
    window.localStorage.removeItem(CART_TOKEN_KEY);
    queueMicrotask(() => setAnonymousCartToken(null));
  }, []);

  const storeIdentity = useCallback((identity: CartIdentity) => {
    identityRef.current = identity;
    if (identity.token) {
      window.localStorage.setItem(CART_ID_KEY, String(identity.id));
      window.localStorage.setItem(CART_TOKEN_KEY, identity.token);
      setAnonymousCartToken(identity.token);
    }
  }, []);

  useEffect(() => {
    if (authLoading) return;
    let active = true;
    queueMicrotask(() => {
      if (!active) return;
      setLoading(true);
      setError(null);
    });

    if (accessToken) {
      clearIdentity();
      if (!authenticatedCartId) {
        queueMicrotask(() => {
          if (!active) return;
          setCart(null);
          setLoading(false);
        });
        return () => { active = false; };
      }
      const identity = { id: authenticatedCartId };
      identityRef.current = identity;
      getCart(identity, accessToken)
        .then((nextCart) => { if (active) setCart(nextCart); })
        .catch((caught) => {
          if (!active) return;
          setCart(null);
          setError(caught instanceof Error ? caught.message : "Unable to load your cart.");
        })
        .finally(() => { if (active) setLoading(false); });
      return () => { active = false; };
    }

    const id = Number(window.localStorage.getItem(CART_ID_KEY));
    const token = window.localStorage.getItem(CART_TOKEN_KEY);
    if (!Number.isInteger(id) || id <= 0 || !token) {
      queueMicrotask(() => {
        if (!active) return;
        setCart(null);
        setLoading(false);
      });
      return () => { active = false; };
    }
    const identity = { id, token };
    identityRef.current = identity;
    queueMicrotask(() => { if (active) setAnonymousCartToken(token); });
    getCart(identity)
      .then((nextCart) => { if (active) setCart(nextCart); })
      .catch(() => { if (active) { clearIdentity(); setCart(null); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [accessToken, authenticatedCartId, authLoading, clearIdentity]);

  const ensureIdentity = useCallback(async () => {
    if (identityRef.current) return identityRef.current;
    if (!createPromiseRef.current) {
      createPromiseRef.current = createCart(accessToken).then(({ identity, cart: createdCart }) => {
        storeIdentity(identity);
        if (accessToken) updateCartId(identity.id);
        setCart(createdCart);
        return identity;
      }).finally(() => { createPromiseRef.current = null; });
    }
    return createPromiseRef.current;
  }, [accessToken, storeIdentity, updateCartId]);

  const addItem = useCallback(async (request: CartItemRequest) => {
    setError(null);
    const identity = await ensureIdentity();
    try {
      const updated = await addCartItem(identity, request, accessToken);
      setCart(updated);
      return updated;
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : "Unable to add this item.";
      setError(message);
      throw caught;
    }
  }, [accessToken, ensureIdentity]);

  const changeQuantity = useCallback(async (item: CartItem, quantity: number) => {
    if (!identityRef.current) throw new Error("Your cart could not be found.");
    setError(null);
    const updated = await updateCartItem(identityRef.current, item.id, requestFromCartItem(item, quantity), accessToken);
    setCart(updated);
    return updated;
  }, [accessToken]);

  const removeItem = useCallback(async (itemId: number) => {
    if (!identityRef.current) return;
    setError(null);
    await deleteCartItem(identityRef.current, itemId, accessToken);
    const updated = await getCart(identityRef.current, accessToken);
    setCart(updated);
  }, [accessToken]);

  const refresh = useCallback(async () => {
    if (!identityRef.current) return;
    setCart(await getCart(identityRef.current, accessToken));
  }, [accessToken]);

  return <CartContext.Provider value={{ cart, loading, error, anonymousCartToken: accessToken ? null : anonymousCartToken, addItem, changeQuantity, removeItem, refresh }}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) throw new Error("useCart must be used inside CartProvider.");
  return context;
}
