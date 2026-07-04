import { useEffect, useState } from "react";
import Input from "@/shared/components/Input";
import ModalLayout from "@/shared/components/modal/ModalLayout";
import Button from "@/shared/components/button/Button";
import Tag from "@/shared/components/Tag";
import { toast } from "react-toastify";
import type { UpdateInterestBody } from "@/api/interests/types";

interface InterestEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (keywords: string[]) => void;
  initialData?: UpdateInterestBody;
}

export default function InterestEditModal({
  isOpen,
  onClose,
  onSave,
  initialData,
}: InterestEditModalProps) {
  const [keyword, setKeyword] = useState("");
  const [keywords, setKeywords] = useState<string[]>([]);

  const isFormValid = keywords.length > 0;

  useEffect(() => {
    if (isOpen && initialData) {
      setKeywords(initialData.keywords);
      setKeyword("");
    } else if (!isOpen) {
      setKeyword("");
      setKeywords([]);
    }
  }, [isOpen, initialData]);

  const handleAddKeyword = () => {
    if (keyword.trim() !== "" && !keywords.includes(keyword.trim())) {
      setKeywords((prev) => [...prev, keyword.trim()]);
      setKeyword("");
    } else if (keywords.includes(keyword.trim())) {
      toast.error("동일한 키워드는 등록할 수 없습니다.");
    }
  };

  const handleRemoveKeyword = (keywordToRemove: string) => {
    setKeywords((prev) =>
      prev.filter((keyword) => keyword !== keywordToRemove),
    );
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (isFormValid) {
      onSave(keywords);
      onClose();
    }
  };

  return (
    <ModalLayout isOpen={isOpen} onClose={onClose}>
      <form onSubmit={handleSubmit} className="w-[438px] h-auto gap-10">
        <h2 className="text-24-sb mb-10">관심사 수정</h2>

        <div className="mb-1.5 flex gap-2">
          <Input
            label="키워드"
            placeholder="키워드를 추가해주세요"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className="flex-1"
          />
          <Button
            className="px-4 mt-8 whitespace-nowrap"
            onClick={handleAddKeyword}
            type="button"
          >
            키워드 추가
          </Button>
        </div>

        {keywords.length > 0 && (
          <div className="max-h-32 overflow-y-auto p-3 mb-10">
            <div className="flex flex-wrap gap-2">
              {keywords?.map((keyword, index) => (
                <Tag
                  key={index}
                  label={keyword}
                  onRemove={() => handleRemoveKeyword(keyword)}
                />
              ))}
            </div>
          </div>
        )}

        <Button className="w-full" disabled={!isFormValid} type="submit">
          수정하기
        </Button>
      </form>
    </ModalLayout>
  );
}
