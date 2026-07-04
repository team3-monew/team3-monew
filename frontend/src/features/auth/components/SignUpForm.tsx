import { useState } from "react";
import { useNavigate } from "react-router";
import Input from "@/shared/components/Input";
import Button from "@/shared/components/button/Button";
import { signUpAction } from "@/features/auth/actions";
import { useFormField } from "@/shared/hooks/useFormField";
import {
  required,
  isEmail,
  isPassword,
  compose,
  maxLength,
  isPasswordConfirm,
} from "@/shared/utils/validation";
import Skeleton from "@/shared/components/Skeleton";
import { toast } from "react-toastify";
import { toastApiError } from "@/shared/utils/toastApiError";
import { ROUTES } from "@/shared/constants/routes";

export default function SignUpForm() {
  const nav = useNavigate();

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string>("");

  // 입력 필드 상태 + 검증 훅
  const nicknameField = useFormField(
    "",
    compose(required("닉네임을 입력해 주세요."), maxLength(10)),
  );
  const emailField = useFormField(
    "",
    compose(required("이메일을 입력해 주세요."), isEmail()),
  );
  const passwordField = useFormField(
    "",
    compose(required("비밀번호를 입력해 주세요."), isPassword()),
  );
  const passwordConfirmField = useFormField(
    "",
    compose(
      required("비밀번호 확인을 입력해 주세요."),
      isPasswordConfirm(() => passwordField.value),
    ),
  );

  // 폼 유효성
  const formValid =
    !!nicknameField.value &&
    !!emailField.value &&
    !!passwordField.value &&
    !!passwordConfirmField.value &&
    !nicknameField.error &&
    !emailField.error &&
    !passwordField.error &&
    !passwordConfirmField.error;

  // 제출 로직
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const nicknameError = nicknameField.validateNow();
    const emailError = emailField.validateNow();
    const passwordError = passwordField.validateNow();
    const passwordConfirmError = passwordConfirmField.validateNow();
    if (nicknameError || emailError || passwordError || passwordConfirmError)
      return;

    setIsSubmitting(true);
    setSubmitError("");
    try {
      await signUpAction({
        email: emailField.value,
        password: passwordField.value,
        nickname: nicknameField.value,
      });
      toast.success("회원가입이 완료되었습니다.");
      nav(ROUTES.AUTH_LOGIN, { replace: true });
    } catch (error) {
      const message =
        (error as Error)?.message ?? "잠시 후 다시 시도해 주세요.";
      setSubmitError(message);
      toastApiError(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col w-full max-w-[400px] gap-4"
    >
      <Input
        id="nickname"
        label="닉네임"
        placeholder="닉네임을 입력해 주세요 (최대 10자)"
        value={nicknameField.value}
        onChange={nicknameField.onChange}
        onBlur={nicknameField.onBlur}
        error={nicknameField.touched ? nicknameField.error : ""}
        autoComplete="nickname"
      />
      <Input
        id="email"
        label="아이디"
        placeholder="이메일을 입력해 주세요"
        value={emailField.value}
        onChange={emailField.onChange}
        onBlur={emailField.onBlur}
        error={emailField.touched ? emailField.error : ""}
        autoComplete="email"
        inputMode="email"
      />
      <Input
        id="password"
        label="비밀번호"
        type="password"
        placeholder="비밀번호를 입력해 주세요"
        value={passwordField.value}
        onChange={passwordField.onChange}
        onBlur={passwordField.onBlur}
        error={passwordField.touched ? passwordField.error : ""}
        autoComplete="new-password"
      />
      <Input
        id="passwordConfirm"
        label="비밀번호 확인"
        type="password"
        placeholder="비밀번호를 다시 입력해 주세요"
        value={passwordConfirmField.value}
        onChange={passwordConfirmField.onChange}
        onBlur={passwordConfirmField.onBlur}
        error={passwordConfirmField.touched ? passwordConfirmField.error : ""}
        autoComplete="new-password"
      />

      {submitError && (
        <p className="mt-1.5 px-1 text-14-m text-error" role="alert">
          {submitError}
        </p>
      )}

      <Button
        type="submit"
        className="w-full mt-8"
        disabled={!formValid || isSubmitting}
      >
        {isSubmitting ? <Skeleton className="mx-4" /> : "회원가입하기"}
      </Button>
    </form>
  );
}
