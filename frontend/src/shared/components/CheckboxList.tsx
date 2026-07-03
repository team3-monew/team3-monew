import chevronDown from "@/assets/icons/chevron-down-16.svg";
import { useEffect, useRef, useState } from "react";

interface CheckboxListProps {
  items: string[];
  values: string[];
  onChange: (values: string[]) => void;
  className?: string;
  placeholder?: string;
}

export default function CheckboxList({
  items,
  values,
  onChange,
  className = "w-full h-10",
  placeholder = "선택하세요",
}: CheckboxListProps) {
  const [isOpen, setIsOpen] = useState(false);
  const selectRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        selectRef.current &&
        !selectRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const handleToggle = () => {
    setIsOpen(!isOpen);
  };

  const handleCheckboxChange = (item: string) => {
    const newValues = values.includes(item)
      ? values.filter((v) => v !== item)
      : [...values, item];
    onChange(newValues);
  };

  const displayText =
    values.length > 0 ? `${values.length}개 선택됨` : placeholder;

  return (
    <div ref={selectRef} className={`relative ${className}`}>
      <button
        type="button"
        className="border rounded-lg border-gray-200 bg-white py-2.5 px-3 cursor-pointer w-full h-full focus:outline-none"
        onClick={handleToggle}
      >
        <div className="flex justify-between items-center">
          <p
            className={`text-14-m ${values.length === 0 ? "text-gray-400" : "text-gray-900"}`}
          >
            {displayText}
          </p>
          <img
            src={chevronDown}
            className={`transform transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`}
            alt="chevron"
          />
        </div>
      </button>

      {isOpen && (
        <div className="absolute top-full left-0 right-0 z-10">
          <div className="absolute box-border bg-white border border-gray-200 rounded-lg overflow-hidden w-full">
            <ul className="py-1 max-h-60 overflow-y-auto">
              {items.map((item) => (
                <li
                  key={item}
                  className="h-11 p-3 gap-0.5 bg-white font-pretendard font-medium text-sm leading-5 hover:bg-gray-100 cursor-pointer"
                >
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={values.includes(item)}
                      onChange={() => handleCheckboxChange(item)}
                      className="w-4 h-4 rounded border-gray-300 text-cyan-600 focus:ring-cyan-500 cursor-pointer"
                    />
                    <span>{item}</span>
                  </label>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}
