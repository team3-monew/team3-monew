import { useEffect, useState } from "react";
import Input from "@/shared/components/Input";
import ModalLayout from "@/shared/components/modal/ModalLayout";
import Button from "@/shared/components/button/Button";
import Tag from "@/shared/components/Tag";
import type { AddInterestBody } from "@/api/interests/types";
import { toast } from "react-toastify";

interface UpdateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: AddInterestBody) => void;
}

export default function UpdateModal({
  isOpen,
  onClose,
  onSave,
}: UpdateModalProps) {
  const [interestValue, setInterestValue] = useState("");
  const [keywords, setKeywords] = useState<string[]>([]);
  const [keyword, setKeyword] = useState("");

  const isFormValid = interestValue.trim() !== "" && keywords.length > 0;

  useEffect(() => {
    if (!isOpen) {
      setInterestValue("");
      setKeyword("");
    }
  }, [isOpen]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (isFormValid) {
      onSave({
        name: interestValue,
        keywords: keywords,
      });
      setInterestValue("");
      setKeyword("");
      setKeywords([]);
      onClose();
    }
  };

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

  return (
    <ModalLayout isOpen={isOpen} onClose={onClose}>
      <form onSubmit={handleSubmit} className="w-[438px] h-auto gap-10">
        <h2 className="text-24-sb mb-10">관심사 등록</h2>
        <Input
          label="관심사 이름"
          placeholder="관심사 이름을 입력해주세요"
          value={interestValue}
          onChange={(e) => setInterestValue(e.target.value)}
          className="mb-6"
        />
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

        <p className="px-1 gap-2.5 text-14-r text-gray-500 mb-16">
          *설정한 키워드 기준으로 뉴스를 자동 수집합니다
        </p>

        {keywords.length > 0 && (
          <div className="max-h-20 overflow-y-auto flex flex-wrap gap-2 mb-10">
            {keywords?.map((keyword, index) => (
              <Tag
                key={index}
                label={keyword}
                onRemove={() => handleRemoveKeyword(keyword)}
              />
            ))}
          </div>
        )}

        <Button className="w-full" disabled={!isFormValid} type="submit">
          등록하기
        </Button>
      </form>
    </ModalLayout>
  );
}
