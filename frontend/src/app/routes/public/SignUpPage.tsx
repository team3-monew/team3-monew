import SignUpForm from "@/features/auth/components/SignUpForm";
import NavigationButton from "@/features/auth/components/NavigationButton";
import { ROUTES } from "@/shared/constants/routes";

export default function SignUpPage() {
  return (
    <div className="flex flex-col items-center justify-center w-full">
      <SignUpForm />
      <NavigationButton
        linkTo={ROUTES.AUTH_LOGIN}
        contentText="이미 계정이 있으신가요?"
        linkText="로그인하기"
      />
    </div>
  );
}
