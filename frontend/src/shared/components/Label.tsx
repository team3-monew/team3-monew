interface LabelProp {
  src?: string;
  label: string;
}

export default function Label({ src, label }: LabelProp) {
  return (
    <div className="flex gap-1.5 items-center w-fit">
      {src && (
        <div className="p-0.5 bg-gray-200 rounded-full">
          <img src={src} className="w-5 h-5 rounded-full" />
        </div>
      )}
      <p className="font-pretendard font-semibold text-sm leading-5 text-gray-500">
        {label}
      </p>
    </div>
  );
}
