import { useState } from "react";

interface ConfirmModalData {
  title: string;
  message: string;
  onConfirm: () => void | Promise<void>;
  confirmText?: string;
  cancelText?: string;
}

export default function useConfirmModal() {
  const [isOpen, setIsOpen] = useState(false);
  const [modalData, setModalData] = useState<ConfirmModalData | null>(null);

  const openModal = (data: ConfirmModalData) => {
    setModalData(data);
    setIsOpen(true);
  };

  const closeModal = () => {
    setIsOpen(false);
    setModalData(null);
  };

  return { isOpen, openModal, onClose: closeModal, initialData: modalData };
}
