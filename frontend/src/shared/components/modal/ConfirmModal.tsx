import ModalLayout from "@/shared/components/modal/ModalLayout";
import Button from "@/shared/components/button/Button";

interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: "danger" | "warning" | "info";
}

export default function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  cancelText = "취소",
  confirmText = "확인",
}: ConfirmModalProps) {
  const handleConfirm = async () => {
    await onConfirm();
    onClose();
  };

  return (
    <ModalLayout isOpen={isOpen} onClose={onClose}>
      <div className="w-full p-6 text-center">
        <h2 className="text-20-b text-gray-900 mb-4">{title}</h2>
        <p className="text-16-r text-gray-600 mb-8 leading-relaxed">
          {message}
        </p>

        <div className="flex gap-4 pt-4">
          <Button
            variant="secondary"
            onClick={onClose}
            className="min-w-[100px] flex-1 px-6"
          >
            {cancelText}
          </Button>
          <Button
            variant="primary"
            onClick={handleConfirm}
            className="min-w-[100px] flex-1 px-6"
          >
            {confirmText}
          </Button>
        </div>
      </div>
    </ModalLayout>
  );
}
