type IconProps = { className?: string };

export function MountainIcon({ className }: IconProps) {
  return <svg className={className} viewBox="0 0 48 48" aria-hidden="true"><path d="M4 39 18.2 13l5.2 8.2L29.7 9 44 39H4Z" fill="currentColor" /><path d="m13.2 22.2 5-9.2 5.2 8.2 2.4 4.8-7.3-4.6-5.3 3.2v-2.4ZM25.7 17l4-8L37 24.2l-5.2-3.8-3.4 2.3-2.7-5.7Z" fill="white" fillOpacity=".9" /></svg>;
}

export function ArrowIcon({ className }: IconProps) {
  return <svg className={className} viewBox="0 0 20 20" aria-hidden="true"><path d="M4 10h11m-4-4 4 4-4 4" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></svg>;
}

export function CheckIcon({ className }: IconProps) {
  return <svg className={className} viewBox="0 0 20 20" aria-hidden="true"><path d="m4.5 10.2 3.2 3.2 7.8-7.8" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" /></svg>;
}

export function CartIcon({ className }: IconProps) {
  return <svg className={className} viewBox="0 0 24 24" aria-hidden="true"><path d="M3.5 4h2l1.8 10.2h10.8l2-7.2H6.2M9 19a1 1 0 1 1-2 0 1 1 0 0 1 2 0Zm10 0a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" /></svg>;
}

export function UserIcon({ className }: IconProps) {
  return <svg className={className} viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5" fill="none" stroke="currentColor" strokeWidth="1.7" /><path d="M5.5 20c.6-4 2.8-6 6.5-6s5.9 2 6.5 6" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.7" /></svg>;
}
