import notFoundIcon from "@/assets/icons/symbol404.svg";

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center gap-4 min-h-[inherit]">
      <img src={notFoundIcon} alt="gray monew logo" className="h-[60px]" />
      <p className="sm:text-20-m text-16-m">404 Not Found</p>
    </div>
  );
}
