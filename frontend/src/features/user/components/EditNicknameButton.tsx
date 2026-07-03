import EditIconUrl from "@/assets/icons/edit.svg";
import EditNicknameModal from "@/shared/components/modal/EditNicknameModal";
import useEditNicknameModal from "@/shared/hooks/useEditNicknameModal";
import { useAuth } from "@/features/auth/hooks/useAuth";

export default function EditNicknameButton() {
  const user = useAuth();
  const { isOpen, openModal, onClose } = useEditNicknameModal();

  if (!user) return null;

  const handleModalOpen = () => {
    openModal(user.id);
  };

  return (
    <>
      <button
        type="button"
        aria-label="닉네임 수정"
        onClick={handleModalOpen}
        disabled={!user}
      >
        <img src={EditIconUrl} alt="" />
      </button>

      {isOpen && (
        <EditNicknameModal isOpen={isOpen} onClose={onClose} user={user} />
      )}
    </>
  );
}
