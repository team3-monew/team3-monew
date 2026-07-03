import emptyUrl from "@/assets/images/empty.svg";

type EmptyStateProps = {
  message: string;
  className?: string;
};

export default function EmptyState({ message, className }: EmptyStateProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      aria-atomic="true"
      className={`flex flex-col items-center justify-center text-center gap-4 h-full
        ${className ? `${className}` : ""}`}
    >
      <img
        src={emptyUrl}
        alt=""
        aria-hidden="true"
        className="md:w-[110px] w-[70px] select-none"
        draggable={false}
      />
      <p className="text-gray-400 md:text-16-m text-14-m">{message}</p>
    </div>
  );
}
