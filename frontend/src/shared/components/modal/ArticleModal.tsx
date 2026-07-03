import { useEffect, useState } from "react";
import ModalLayout from "@/shared/components/modal/ModalLayout";
import Input from "@/shared/components/Input";
import Button from "@/shared/components/button/Button";
import type { RestoreArticlesParams } from "@/api/articles/types";

interface ArticleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: RestoreArticlesParams) => void;
}

export default function ArticleModal({
  isOpen,
  onClose,
  onSave,
}: ArticleModalProps) {
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  const isFormValid = fromDate.trim() !== "" && toDate.trim() !== "";

  useEffect(() => {
    if (!isOpen) {
      setFromDate("");
      setToDate("");
    }
  }, [isOpen]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (isFormValid) {
      onSave({
        from: fromDate,
        to: toDate,
      });
      onClose();
    }
  };

  return (
    <ModalLayout isOpen={isOpen} onClose={onClose}>
      <form onSubmit={handleSubmit} className="w-[438px] h-auto gap-10">
        <h2 className="text-24-sb mb-10">기사 복구하기</h2>
        <Input
          label="날짜"
          value={fromDate}
          placeholder="2025.01.01 부터"
          onChange={(e) => setFromDate(e.target.value)}
          className="mb-2"
        />
        <Input
          value={toDate}
          placeholder="2025.01.01 까지"
          onChange={(e) => setToDate(e.target.value)}
          className="mb-12"
        />

        <Button className="w-full" disabled={!isFormValid} type="submit">
          복구하기
        </Button>
      </form>
    </ModalLayout>
  );
}
