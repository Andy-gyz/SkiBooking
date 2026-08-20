"use client";

import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";

import { AUTH_CART_ID_KEY, AUTH_TOKEN_KEY, getCurrentUser, login as loginRequest, register as registerRequest, requestLogout, type AuthResponse, type LoginInput, type RegisterInput, type User } from "@/lib/auth";

type AuthContextValue = {
  user: User | null;
  accessToken: string | null;
  cartId: number | null;
  loading: boolean;
  login: (input: LoginInput) => Promise<AuthResponse>;
  register: (input: RegisterInput) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  updateCartId: (cartId: number | null) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [cartId, setCartId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  const clearSession = useCallback(() => {
    window.localStorage.removeItem(AUTH_TOKEN_KEY);
    window.localStorage.removeItem(AUTH_CART_ID_KEY);
    setAccessToken(null);
    setCartId(null);
    setUser(null);
  }, []);

  const updateCartId = useCallback((nextCartId: number | null) => {
    setCartId(nextCartId);
    if (nextCartId) window.localStorage.setItem(AUTH_CART_ID_KEY, String(nextCartId));
    else window.localStorage.removeItem(AUTH_CART_ID_KEY);
  }, []);

  const storeSession = useCallback((response: AuthResponse) => {
    window.localStorage.setItem(AUTH_TOKEN_KEY, response.accessToken);
    setAccessToken(response.accessToken);
    setUser(response.user);
    updateCartId(response.cartId);
  }, [updateCartId]);

  useEffect(() => {
    const storedToken = window.localStorage.getItem(AUTH_TOKEN_KEY);
    const storedCartId = Number(window.localStorage.getItem(AUTH_CART_ID_KEY));
    if (!storedToken) {
      queueMicrotask(() => setLoading(false));
      return;
    }
    queueMicrotask(() => {
      setAccessToken(storedToken);
      if (Number.isInteger(storedCartId) && storedCartId > 0) setCartId(storedCartId);
      getCurrentUser(storedToken)
        .then(setUser)
        .catch(clearSession)
        .finally(() => setLoading(false));
    });
  }, [clearSession]);

  const login = useCallback(async (input: LoginInput) => {
    const response = await loginRequest(input);
    storeSession(response);
    return response;
  }, [storeSession]);

  const register = useCallback(async (input: RegisterInput) => {
    const response = await registerRequest(input);
    storeSession(response);
    return response;
  }, [storeSession]);

  const logout = useCallback(async () => {
    if (accessToken) {
      try { await requestLogout(accessToken); } finally { clearSession(); }
    } else clearSession();
  }, [accessToken, clearSession]);

  return <AuthContext.Provider value={{ user, accessToken, cartId, loading, login, register, logout, updateCartId }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider.");
  return context;
}
