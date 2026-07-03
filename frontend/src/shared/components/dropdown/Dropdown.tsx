import DropdownItem from "./DropdownItem";

interface DropdownProps {
  items: string[];
  onChange: (value: string) => void;
  className?: string;
}

export default function Dropdown({
  items,
  onChange,
  className,
}: DropdownProps) {
  const handleSelect = (item: string) => {
    onChange(item);
  };

  return (
    <div
      className={`absolute box-border bg-white border border-gray-200 rounded-lg overflow-hidden ${className}`}
    >
      <ul className={`py-1 max-h-60 overflow-y-auto cursor-pointer`}>
        {items.map((item, index) => (
          <DropdownItem
            key={index}
            label={item}
            onClick={() => handleSelect(item)}
          />
        ))}
      </ul>
    </div>
  );
}
