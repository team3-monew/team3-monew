interface TagProps {
  label: string;
  onRemove?: () => void;
}

export default function Tag({ label, onRemove }: TagProps) {
  return (
    <div
      className={`w-fit h-8 px-2 py-1 rounded-lg bg-gray-100 flex items-center gap-1`}
    >
      <p className={`text-16-m text-gray-500`}>{label}</p>
      {onRemove && (
        <button
          type="button"
          onClick={onRemove}
          className="w-4 h-4 flex items-center justify-center text-gray-400 hover:text-gray-600 cursor-pointer text-sm"
          aria-label="태그 삭제"
        >
          ×
        </button>
      )}
    </div>
  );
}
