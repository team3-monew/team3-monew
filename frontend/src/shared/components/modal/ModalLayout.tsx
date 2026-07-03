import { useClosePopup } from "@/shared/hooks/useClosePopup";
import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import closeIcon from "@/assets/icons/close-primary-24.svg";

interface ModalLayoutProps {
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
  width?: string;
  noPadding?: boolean;
  disableClose?: boolean;
}

export default function ModalLayout({
  isOpen,
  onClose,
  children,
  width = "w-[502px]",
  noPadding = false,
  disableClose = false,
}: ModalLayoutProps) {
  const modalRef = useRef<HTMLDivElement>(null);

  useClosePopup(modalRef, onClose, isOpen, disableClose);

  // 모달 열릴 때 body 스크롤 막기
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }

    // cleanup: 컴포넌트 언마운트 시 원복
    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return createPortal(
    <div
      className="z-50 fixed inset-0 bg-black/50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
    >
      <div
        ref={modalRef}
        className={`${width} max-h-[90vh] overflow-y-auto h-auto rounded-2xl ${noPadding ? "" : "p-8"} gap-2.5 bg-white relative`}
        onClick={(e) => e.stopPropagation()}
      >
        <button
          className="absolute top-9 right-7 hover:text-gray-700"
          onClick={onClose}
          aria-label="close"
        >
          <img src={closeIcon} alt="닫기" className="w-6 h-6" />
        </button>
        {children}
      </div>
    </div>,
    document.body,
  );
}
