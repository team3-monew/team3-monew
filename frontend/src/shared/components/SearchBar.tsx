import { useRef } from "react";
import searchIcon from "@/assets/icons/search.svg";

interface SearchBarProps extends React.InputHTMLAttributes<HTMLInputElement> {
  width?: string;
  height?: string;
  containerClassName?: string;
  onSearch?: (value: string) => void;
}

export default function SearchBar({
  width = "w-3xs",
  height = "h-[40px]",
  containerClassName = "",
  className = "",
  onSearch, //검색버튼 누를때 실행할 함수
  onKeyDown, //다른 키보드 이벤트 처리함수
  ...props
}: SearchBarProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleSearchClick = () => {
    const value = inputRef.current?.value || "";

    if (onSearch && value.trim()) {
      onSearch(value.trim());

      if (inputRef.current) {
        inputRef.current.value = "";
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    //한글 입력시 엔터 두번되는 거 방지
    if (e.key === "Enter" && onSearch && !e.nativeEvent.isComposing) {
      e.preventDefault();
      handleSearchClick();
    }
    //원래 핸들러도 실행
    onKeyDown?.(e);
  };

  return (
    <div
      className={`${width} ${height} px-4 py-2.5 border bg-white border-gray-300 rounded-[100px] flex items-center justify-between gap-2.5 ${containerClassName}`}
    >
      <input
        ref={inputRef}
        type="text"
        placeholder="검색어를 입력해주세요"
        className={`flex-1 outline-none font-pretendard placeholder:text-sm placeholder:text-gray-400 placeholder:font-normal placeholder:leading-5 ${className}`}
        onKeyDown={handleKeyDown}
        {...props}
      />
      <button type="button" onClick={handleSearchClick} aria-label="검색">
        <img src={searchIcon} alt="검색" className="w-4 h-4" />
      </button>
    </div>
  );
}
