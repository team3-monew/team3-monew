import LandingBox from "@/shared/components/LandingBox";
import Button from "@/shared/components/button/Button";
import { useAuthInfo } from "@/features/auth/hooks/useAuthInfo";
import { Link } from "react-router";
import { ROUTES } from "@/shared/constants/routes";
import firstImageUrl from "@/assets/images/landing_interests.svg";
import secondImageUrl from "@/assets/images/landing_notifications.svg";
import thirdImageUrl from "@/assets/images/landing_comments.svg";

export default function LandingPage() {
  const { user } = useAuthInfo();
  const link = user ? ROUTES.ARTICLES : ROUTES.AUTH_LOGIN;

  return (
    <div className="flex flex-col items-center px-4 w-full">
      <div className="lg:mb-10 mb-6 md:gap-1.5 gap-0 flex flex-col text-center">
        <p className="lg:text-[30px] md:text-[24px] text-[20px]">
          마음대로 골라 보는 모든 뉴스
        </p>
        <h1 className="lg:text-[36px] md:text-[30px] text-[24px] font-bold text-cyan-500">
          모뉴
        </h1>
      </div>

      <div className="lg:flex-row flex flex-col justify-center items-center gap-6 w-full">
        <LandingBox
          imageSrc={firstImageUrl}
          imageAlt="관심사 목록"
          description="관심사를 등록해보세요"
          containerClassName="bg-white border-cyan-400"
          descriptionClassName="text-cyan-500"
        />
        <LandingBox
          imageSrc={secondImageUrl}
          imageAlt="알림 목록"
          description="키워드에 맞는 뉴스가 수집돼요"
          containerClassName="bg-gray-400 border-gray-200"
          descriptionClassName="text-white"
        />
        <LandingBox
          imageSrc={thirdImageUrl}
          imageAlt="댓글 목록"
          description="뉴스를 읽고 의견을 남겨보세요"
          containerClassName="bg-gray-500 border-gray-300"
          descriptionClassName="text-white"
        />
      </div>

      <Link to={link}>
        <Button className="lg:mt-15 mt-10 w-[250px]">지금 시작하기</Button>
      </Link>
    </div>
  );
}
