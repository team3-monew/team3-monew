import { useAuthInfo } from "@/features/auth/hooks/useAuthInfo";
import EditProfileButton from "./EditNicknameButton";

export default function ProfileCard() {
  const { userName, userEmail } = useAuthInfo();

  return (
    <div className="flex flex-col w-[260px] h-[100px] bg-white rounded-2xl border border-gray-200 p-6">
      <div className="flex w-full gap-1.5">
        <p className="text-black text-18-sb">{userName}</p>
        <EditProfileButton />
      </div>
      <span className="text-[#9EA5B0] text-16-r">{userEmail}</span>
    </div>
  );
}
