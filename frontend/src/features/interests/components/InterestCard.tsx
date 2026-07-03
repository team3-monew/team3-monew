import kebabMenuIcon from "@/assets/icons/kebab-menu-32.svg";
import personIcon from "@/assets/icons/person.svg";
import checkIcon from "@/assets/icons/check-default.svg";
import Button from "@/shared/components/button/Button";
import { useRef, useState } from "react";
import type { InterestId } from "@/types/ids";
import Dropdown from "@/shared/components/dropdown";
import { useClosePopup } from "@/shared/hooks/useClosePopup";
import useInterestEditModal from "@/shared/hooks/useInterestEditModal";
import InterestEditModal from "@/shared/components/modal/InterestEditModal";
import ConfirmModal from "@/shared/components/modal/ConfirmModal";
import useConfirmModal from "@/shared/hooks/useConfirmModal";

interface InterestCardProps {
  interestId: InterestId;
  name: string;
  keywords: string[];
  subscriberCount: number;
  isSubscribed?: boolean;
  onSubscribeClick: (id: InterestId, isSubscribed: boolean) => void;
  onSaveKeyword: (id: InterestId, keywords: string[]) => void;
  onDeleteInterest: (id: InterestId) => void;
}

export default function InterestCard({
  interestId,
  name,
  keywords,
  subscriberCount,
  isSubscribed = false,
  onSubscribeClick,
  onSaveKeyword,
  onDeleteInterest,
}: InterestCardProps) {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLButtonElement>(null);

  const { isOpen, openModal, onClose, initialData } = useInterestEditModal();

  const {
    isOpen: isConfirmOpen,
    openModal: openConfirmModal,
    onClose: closeConfirmModal,
    initialData: confirmModalData,
  } = useConfirmModal();

  useClosePopup(dropdownRef, () => setIsDropdownOpen(false), isDropdownOpen);

  const handleSubscribeClick = () => {
    onSubscribeClick(interestId, isSubscribed);
  };

  const handleDropdownChange = (selectedItem: string) => {
    if (selectedItem === "키워드 수정") {
      openModal({
        keywords: keywords,
      });
    } else if (selectedItem === "관심사 삭제") {
      openConfirmModal({
        title: "관심사 삭제",
        message: `'${name}' 관심사를 정말 삭제하시겠습니까?\n삭제된 관심사는 복구할 수 없습니다.`,
        confirmText: "삭제",
        cancelText: "취소",
        onConfirm: () => onDeleteInterest(interestId),
      });
    }
    setIsDropdownOpen(false); //메뉴선택시 드롭다운 닫히게
  };

  const handleKeywordSave = (updatedKeywords: string[]) => {
    onSaveKeyword(interestId, updatedKeywords);
    onClose();
  };

  return (
    <div className="w-full h-[232px] border border-gray-200 rounded-2xl p-6 bg-white flex flex-col">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-20-b text-gray-900">{name}</h2>
        <button
          className="relative"
          onClick={() => setIsDropdownOpen(!isDropdownOpen)}
          ref={dropdownRef}
        >
          <img src={kebabMenuIcon} className="w-8 h-8" alt="케밥" />
          {isDropdownOpen && (
            <Dropdown
              items={["키워드 수정", "관심사 삭제"]}
              onChange={handleDropdownChange}
              className="right-0 top-7 z-10 min-w-32"
            />
          )}
        </button>
      </div>

      <div className="flex flex-wrap gap-2 mb-6 flex-1 overflow-y-auto">
        {keywords.map((keyword, index) => (
          <div
            key={index}
            className="rounded-lg py-1 px-2 bg-gray-100 text-16-m text-gray-500 h-fit"
          >
            {keyword}
          </div>
        ))}
      </div>

      <div className="flex justify-between items-center">
        <div className="flex items-center justify-center">
          <img src={personIcon} className="w-6 h-6" alt="사람모양" />
          <span className="text-14-r text-gray-500">
            {subscriberCount} 구독자
          </span>
        </div>
        {isSubscribed ? (
          <Button
            variant="secondary"
            size="sm"
            className="flex gap-1 min-w-[91px]"
            onClick={handleSubscribeClick}
          >
            <img src={checkIcon} className="w-4 h-4" alt="체크" />
            구독 중
          </Button>
        ) : (
          <Button
            className="min-w-[91px]"
            size="sm"
            onClick={handleSubscribeClick}
          >
            구독하기
          </Button>
        )}
      </div>
      <InterestEditModal
        isOpen={isOpen}
        onClose={onClose}
        onSave={handleKeywordSave}
        initialData={initialData}
      />

      {confirmModalData && (
        <ConfirmModal
          isOpen={isConfirmOpen}
          onClose={closeConfirmModal}
          onConfirm={confirmModalData.onConfirm}
          title={confirmModalData.title}
          message={confirmModalData.message}
          confirmText={confirmModalData.confirmText}
          cancelText={confirmModalData.cancelText}
        />
      )}
    </div>
  );
}
