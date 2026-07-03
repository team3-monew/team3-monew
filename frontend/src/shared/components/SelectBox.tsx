import chevronDown from "@/assets/icons/chevron-down-16.svg";
import { useEffect, useRef, useState } from "react";
import Dropdown from "@/shared/components/dropdown/Dropdown";

interface SelectBarProps {
  placeholder?: string;
  items: string[];
  value?: string;
  onChange: (value: string) => void;
  className?: string;
  noBorder?: boolean;
  textClassName?: string;
  noBackground?: boolean;
}

export default function SelectBox({
  placeholder = "선택하세요",
  items,
  value,
  onChange,
  className = "w-full h-10",
  noBorder = false,
  textClassName = "text-14-m",
  noBackground = false,
}: SelectBarProps) {
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

  const handleSelectBarClick = () => {
    setIsOpen(!isOpen);
  };

  const handleItemSelect = (selectedValue: string) => {
    onChange(selectedValue);
    setIsOpen(false);
  };

  return (
    <div ref={selectRef} className={`relative ${className}`}>
      <button
        type="button"
        className={`${noBorder ? "" : "border rounded-lg border-gray-200"} ${noBackground ? "" : "bg-white"} py-2.5 px-3 cursor-pointer w-full h-full focus:outline-none`}
        onClick={handleSelectBarClick}
      >
        <div className="flex justify-between items-center">
          <p
            className={`${textClassName} ${!value ? "text-gray-400" : noBorder ? "text-cyan-600" : "text-gray-900"}`}
          >
            {value || placeholder}
          </p>
          <img
            src={chevronDown}
            className={`transform transition-transform duration-200 ${isOpen ? "rotate-180" : ""} ${noBorder ? "ml-3" : ""}`}
            alt="chevron"
          />
        </div>
      </button>

      {isOpen && (
        <div className="absolute top-full left-0 right-0 z-10">
          <Dropdown
            items={items}
            onChange={handleItemSelect}
            className="w-full"
          />
        </div>
      )}
    </div>
  );
}
