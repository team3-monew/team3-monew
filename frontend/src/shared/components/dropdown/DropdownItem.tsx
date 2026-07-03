interface DropdownItemProps {
  label: string;
  onClick: () => void;
}

export default function DropdownItem({ label, onClick }: DropdownItemProps) {
  return (
    <li
      className={`h-11 p-3 gap-0.5 bg-white font-pretendard font-medium text-sm leading-5 hover:bg-gray-100 truncate`}
      onClick={onClick}
    >
      <span>{label}</span>
    </li>
  );
}
