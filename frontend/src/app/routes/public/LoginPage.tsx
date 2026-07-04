import LoginForm from "@/features/auth/components/LoginForm";
import NavigationButton from "@/features/auth/components/NavigationButton";
import { ROUTES } from "@/shared/constants/routes";

export default function LoginPage() {
  return (
    <div className="flex flex-col items-center justify-center w-full">
      <LoginForm />
      <NavigationButton
        linkTo={ROUTES.AUTH_SIGNUP}
        contentText="아직 계정이 없으신가요?"
        linkText="회원가입하기"
      />
    </div>
  );
}
