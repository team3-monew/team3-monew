import type { ArticleListItem } from "@/api/articles/types";
import { useState } from "react";

export default function useArticleDetailModal() {
  const [isOpen, setIsOpen] = useState(false);
  const [modalData, setModalData] = useState<ArticleListItem | null>(null);

  const openModal = (data: ArticleListItem) => {
    setModalData(data);
    setIsOpen(true);
  };

  const closeModal = () => {
    setIsOpen(false);
    setModalData(null);
  };

  return { isOpen, openModal, onClose: closeModal, initialData: modalData };
}
