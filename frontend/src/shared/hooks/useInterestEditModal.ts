import type { UpdateInterestBody } from "@/api/interests/types";
import { useState } from "react";

export default function useInterestEditModal() {
  const [isOpen, setIsOpen] = useState(false);
  const [editInterest, setEditInterest] = useState<
    UpdateInterestBody | undefined
  >();

  const openModal = (interest: UpdateInterestBody) => {
    setEditInterest(interest);
    setIsOpen(true);
  };

  const closeModal = () => setIsOpen(false);

  return { isOpen, openModal, onClose: closeModal, initialData: editInterest };
}
