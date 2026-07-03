import type { UserId } from "@/types/ids";
import { useState } from "react";

export default function useEditNicknameModal() {
  const [isOpen, setIsOpen] = useState(false);
  const [editUser, setEditUser] = useState<UserId | undefined>();

  const openModal = (user: UserId) => {
    setEditUser(user);
    setIsOpen(true);
  };

  const closeModal = () => setIsOpen(false);

  return { isOpen, openModal, onClose: closeModal, initialData: editUser };
}
