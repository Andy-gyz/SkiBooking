"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";

import { useAuth } from "@/components/auth-provider";
import { useCart } from "@/components/cart-provider";
import { ArrowIcon, CheckIcon, MountainIcon } from "@/components/icons";
import { sendVerificationCode } from "@/lib/auth";

type AuthFormProps = {
  mode: "login" | "register";
  nextPath?: string;
};

function safeNextPath(nextPath?: string) {
  return nextPath?.startsWith("/") && !nextPath.startsWith("//") ? nextPath : "/account";
}

export function AuthForm({ mode, nextPath }: AuthFormProps) {
  const router = useRouter();
  const { user, login, register } = useAuth();
  const { anonymousCartToken, cart } = useCart();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [codeSent, setCodeSent] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const formRef = useRef<HTMLFormElement>(null);
  const destination = safeNextPath(nextPath);
  const isRegister = mode === "register";
  const switchHref = `${isRegister ? "/login" : "/register"}?next=${encodeURIComponent(destination)}`;

  useEffect(() => {
    if (user) router.replace(!nextPath && user.role === "ADMIN" ? "/admin" : destination);
  }, [destination, nextPath, router, user]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = window.setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [cooldown]);

  async function requestCode(email: string) {
    const response = await sendVerificationCode(email);
    setCodeSent(true);
    setCooldown(response.resendAfterSeconds);
  }

  async function resendCode() {
    if (!formRef.current || cooldown > 0) return;
    setPending(true);
    setError(null);
    try { await requestCode(String(new FormData(formRef.current).get("email") ?? "")); }
    catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to send another code."); }
    finally { setPending(false); }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError(null);
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "");
    const password = String(form.get("password") ?? "");
    try {
      if (isRegister) {
        if (!codeSent) {
          await requestCode(email);
          setPending(false);
          return;
        }
        await register({
          firstName: String(form.get("firstName") ?? ""),
          lastName: String(form.get("lastName") ?? ""),
          email,
          password,
          phone: String(form.get("phone") ?? ""),
          verificationCode: String(form.get("verificationCode") ?? ""),
          cartToken: anonymousCartToken ?? undefined,
        });
      } else {
        const response = await login({ email, password, cartToken: anonymousCartToken ?? undefined });
        router.replace(!nextPath && response.user.role === "ADMIN" ? "/admin" : destination);
        return;
      }
      router.replace(destination);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to continue. Please try again.");
      setPending(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-shell">
        <div className="auth-story">
          <div className="auth-story__glow" />
          <Link className="auth-story__brand" href="/"><MountainIcon /> Snow Alpine</Link>
          <div className="auth-story__copy">
            <p className="eyebrow eyebrow--light">One connected snow day</p>
            <h1>{isRegister ? "Create an account. Keep your plans." : "Welcome back to the mountain."}</h1>
            <p>Your cart stays exactly as you built it. Sign in only now, when you&apos;re ready to move toward secure checkout.</p>
          </div>
          <div className="auth-cart-note">
            <CheckIcon />
            <span><small>Cart ready</small><strong>{cart?.itemCount ?? 0} {cart?.itemCount === 1 ? "reservation" : "reservations"} will follow you</strong></span>
          </div>
        </div>

        <div className="auth-panel">
          <div className="auth-panel__inner">
            <p className="eyebrow">{isRegister ? "New customer" : "Customer account"}</p>
            <h2>{isRegister ? "Start your account" : "Sign in to continue"}</h2>
            <p className="auth-intro">{isRegister ? "Just the essentials. You can review everything before payment." : "Use the email and password connected to your Snow Alpine account."}</p>
            <form className="auth-form" onSubmit={submit} ref={formRef}>
              {isRegister && <div className="auth-form__row">
                <label><span>First name</span><input name="firstName" autoComplete="given-name" maxLength={100} required /></label>
                <label><span>Last name</span><input name="lastName" autoComplete="family-name" maxLength={100} required /></label>
              </div>}
              <label><span>Email address</span><input name="email" type="email" autoComplete="email" maxLength={255} readOnly={isRegister && codeSent} required /></label>
              {isRegister && <label><span>Phone <small>Optional</small></span><input name="phone" type="tel" autoComplete="tel" maxLength={30} /></label>}
              <label><span>Password</span><input name="password" type="password" autoComplete={isRegister ? "new-password" : "current-password"} minLength={8} maxLength={72} required /></label>
              {isRegister && codeSent && <div className="verification-step">
                <div className="verification-step__heading"><span><CheckIcon /> Code sent</span><button type="button" onClick={() => { setCodeSent(false); setCooldown(0); setError(null); }}>Change email</button></div>
                <label><span>6-digit verification code</span><input className="verification-code-input" name="verificationCode" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} placeholder="000000" required autoFocus /></label>
                <button className="verification-resend" type="button" disabled={pending || cooldown > 0} onClick={resendCode}>{cooldown > 0 ? `Send another code in ${cooldown}s` : "Send another code"}</button>
              </div>}
              {error && <div className="auth-error" role="alert">{error}</div>}
              <button className="button button--ink" type="submit" disabled={pending}>{pending ? "Please wait…" : isRegister ? codeSent ? "Verify and create account" : "Send verification code" : "Sign in"}<ArrowIcon /></button>
            </form>
            <p className="auth-switch">{isRegister ? "Already have an account?" : "New to Snow Alpine?"} <Link href={switchHref}>{isRegister ? "Sign in" : "Create one"}</Link></p>
            <Link className="auth-back" href="/cart">← Return to your cart</Link>
          </div>
        </div>
      </section>
    </main>
  );
}
