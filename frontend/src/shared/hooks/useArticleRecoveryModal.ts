import { useState } from "react";

export default function useArticleRecoveryModal() {
  const [isOpen, setIsOpen] = useState(false);

  const openModal = () => setIsOpen(true);
  const closeModal = () => setIsOpen(false);

  return { isOpen, openModal, onClose: closeModal };
}
