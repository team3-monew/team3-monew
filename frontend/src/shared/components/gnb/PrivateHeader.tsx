import { Link } from "react-router";
import Logo from "@/shared/components/Logo";
import HeaderTabs from "@/shared/components/gnb/HeaderTabs";
import UserMenu from "@/shared/components/gnb/UserMenu";
import { headerStyle } from "@/shared/components/gnb/header.styles";
import { ROUTES } from "@/shared/constants/routes";

export default function PrivateHeader() {
  return (
    <header className={`${headerStyle}`}>
      <div className="flex items-center justify-between w-full">
        <Link to={ROUTES.ARTICLES} aria-label="articles">
          <Logo className="md:block hidden h-[48px]" />
        </Link>
        <HeaderTabs />
        <UserMenu />
      </div>
    </header>
  );
}
