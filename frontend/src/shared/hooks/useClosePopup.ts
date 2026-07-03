// '창 바깥 클릭, esc 키다운 시 닫힘' 기능 수행
import { useEffect } from "react";

export const useClosePopup = (
  ref: React.RefObject<HTMLElement | null>,
  onClose: () => void,
  isOpen: boolean = true,
  disableClose: boolean = false,
) => {
  useEffect(() => {
    if (!isOpen || disableClose) return;

    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as Node | null;

      if (
        ref.current &&
        target instanceof Node &&
        !ref.current.contains(target)
      ) {
        onClose();
      }
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };

    document.addEventListener("mousedown", handleClickOutside, true);
    document.addEventListener("keydown", handleKeyDown, true);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside, true);
      document.removeEventListener("keydown", handleKeyDown, true);
    };
  }, [onClose, ref, isOpen, disableClose]);
};
