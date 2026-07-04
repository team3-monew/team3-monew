import { Link } from "react-router";

type NavigationButtonProps = {
  linkTo: string;
  contentText: string;
  linkText: string;
  className?: string;
};

export default function NavigationButton({
  linkTo,
  contentText,
  linkText,
  className,
}: NavigationButtonProps) {
  return (
    <div
      className={`flex text-center gap-2.5 mt-6 ${className ? `${className}` : ""}`}
    >
      <p className="text-16-m text-gray-500">{contentText}</p>
      <Link to={linkTo} className="text-16-m text-cyan-600 underline">
        {linkText}
      </Link>
    </div>
  );
}
