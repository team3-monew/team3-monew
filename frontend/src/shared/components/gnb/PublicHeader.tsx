import { Link } from "react-router";
import Logo from "@/shared/components/Logo";
import { headerStyle } from "@/shared/components/gnb/header.styles";
import { ROUTES } from "@/shared/constants/routes";

export default function PublicHeader() {
  return (
    <header className={`${headerStyle}`}>
      <div className="flex items-center w-full">
        <Link to={ROUTES.ROOT} aria-label="home">
          <Logo className="md:h-[48px] h-[36px]" />
        </Link>
      </div>
    </header>
  );
}
