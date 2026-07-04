import logoUrl from "@/assets/logos/app/logo.svg";

export default function HeaderLogo({ className }: { className?: string }) {
  return (
    <img
      src={logoUrl}
      alt="Monew logo"
      className={`${className ? `${className}` : ""}`}
    />
  );
}
