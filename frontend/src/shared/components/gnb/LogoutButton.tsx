import { logout } from "@/features/auth/actions";
import { toast } from "react-toastify";
import logoutIcon from "@/assets/icons/logout.svg";
import { useNavigate } from "react-router";

export default function LogoutButton() {
  const navigate = useNavigate();
  const onClick = async () => {
    logout();
    navigate("/login");
    toast.success("로그아웃이 완료되었습니다.");
  };

  return (
    <button
      type="button"
      onClick={onClick}
      className="text-14-m text-gray-400"
      aria-label="로그아웃"
      title="로그아웃"
    >
      <span className="sm:block hidden">로그아웃</span>
      <img
        src={logoutIcon}
        alt=""
        className="sm:hidden block h-5 cursor-pointer"
      />
    </button>
  );
}
