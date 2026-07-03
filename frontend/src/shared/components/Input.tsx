import VisibilityOn from "@/assets/icons/visibility-on.svg";
import VisibilityOff from "@/assets/icons/visibility-off.svg";
import { useId, useState } from "react";

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: string;
  label?: string;
  inputSize?: "sm" | "md";
}

export default function Input({
  type = "text",
  error,
  placeholder,
  value,
  onChange,
  label,
  inputSize = "md",
  className,
  ...props
}: InputProps) {
  const [showPassword, setShowPassword] = useState(false);

  const isPassword = type === "password";

  const inputId = useId();

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  const getInputType = () => {
    if (type !== "password") {
      return type;
    }
    return showPassword ? "text" : "password";
  };

  const sizeClasses = {
    sm: "min-h-10 py-1.5 px-3",
    md: "min-h-14 py-4 px-5",
  };

  return (
    <div className={className}>
      {label && (
        <label htmlFor={inputId} className="pl-1 text-16-m text-gray-600">
          {label}
        </label>
      )}

      <div
        className={`w-full border rounded-lg mt-1.5 gap-2.5 ${props.disabled ? "bg-gray-50" : "bg-white"} ${sizeClasses[inputSize]} ${error ? "border-error" : "border-gray-200"} focus-within:border-cyan-500 ${className || ""}`}
      >
        <div className="flex items-center justify-between">
          <input
            type={getInputType()}
            placeholder={placeholder}
            className={`flex-1 outline-none bg-transparent text-16-m pr-2 ${props.disabled ? "text-gray-400 cursor-not-allowed" : "text-gray-900"}`}
            value={value}
            onChange={onChange}
            id={inputId}
            {...props}
          />

          {isPassword && (
            <button
              type="button"
              onClick={togglePasswordVisibility}
              aria-label={showPassword ? "비밀번호 보기" : "비밀번호 숨기기"}
            >
              {showPassword ? (
                <img
                  src={VisibilityOn}
                  className="w-6 h-6"
                  alt="비밀번호 보기"
                />
              ) : (
                <img
                  src={VisibilityOff}
                  className="w-6 h-6"
                  alt="비밀번호 숨기기"
                />
              )}
            </button>
          )}
        </div>
      </div>
      {error && <p className="mt-1.5 px-1 text-14-m text-error">{error}</p>}
    </div>
  );
}
